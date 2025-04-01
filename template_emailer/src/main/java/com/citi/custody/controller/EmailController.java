package com.citi.custody.controller;

import com.citi.custody.constant.ErrorCodeConstants;
import com.citi.custody.entity.Email;
import com.citi.custody.entity.FilterParams;
import com.citi.custody.service.EmailSenderService;
import com.citi.custody.service.EmailService;
import com.citi.custody.util.AssertUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/email")
public class EmailController {
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailSenderService emailSenderService;

    @Value("${attachment.storage.path:/temp/attachments}")
    private String attachmentPath;

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @PostMapping("/save")
    public ResponseEntity<String> saveEmail(@RequestBody Email email) {
        try {
            logger.info("Received request to save email: {}", email.getEmailName());
            
            // Validate the email
            if (StringUtils.isBlank(email.getEmailName()) || StringUtils.isBlank(email.getContentTemplateId())) {
                logger.warn("Invalid email parameters: emailName or contentTemplateId is empty");
                return ResponseEntity.badRequest().body("Email subject and template ID cannot be empty");
            }

            // Set default status if not provided
            if (email.getStatus() == null) {
                email.setStatus("DRAFT");
                logger.debug("Setting default status to DRAFT");
            }

            // Save the email
            String id = emailService.saveEmail(email);

            // Check the result
            if (StringUtils.isBlank(id)) {
                logger.error("Failed to save email: no ID returned");
                return ResponseEntity.status(500).body("Failed to save email: no ID returned");
            }

            logger.info("Email saved successfully with ID: {}", id);
            return ResponseEntity.ok(id);
        } catch (Exception e) {
            logger.error("Error saving email: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error saving email: " + e.getMessage());
        }
    }

    @PostMapping("/emailsList")
    public Page<Email> getEmailsList(@RequestBody FilterParams params) {
        Pageable pageable = PageRequest.of(params.getPage(), params.getSize());
        Page<Email> emailsList = emailService.getEmailsList(params.getName(), pageable);
        return emailsList;
    }

    @GetMapping("/{id}")
    public Email getEmailById(@PathVariable String id) {
        return emailService.getEmailById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmail(@PathVariable String id) {
        try {
            logger.info("Received request to delete email with ID: {}", id);
            
            if (StringUtils.isBlank(id)) {
                logger.warn("Invalid email ID: empty");
                return ResponseEntity.badRequest().body("Email ID cannot be empty");
            }

            boolean deleted = emailService.deleteEmail(id);
          
            if (deleted) {
                logger.info("Email deleted successfully, ID: {}", id);
                return ResponseEntity.ok("Email deleted successfully");
            } else {
                logger.warn("Failed to delete email with ID: {}", id);
                return ResponseEntity.status(404).body("Failed to delete email. It may not exist or cannot be deleted.");
            }
        } catch (Exception e) {
            logger.error("Error deleting email: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error deleting email: " + e.getMessage());
        }
    }

    @PostMapping("/send/{id}")
    public ResponseEntity<String> sendEmail(@PathVariable String id) {
        try {
            logger.info("Received request to send email with ID: {}", id);
            
            if (StringUtils.isBlank(id)) {
                logger.warn("Invalid email ID: empty");
                return ResponseEntity.badRequest().body("Email ID cannot be empty");
            }

            // Check if email exists
            Email email = emailService.getEmailById(id);
            if (email == null) {
                logger.warn("Email not found with ID: {}", id);
                return ResponseEntity.status(404).body("Email not found with ID: " + id);
            }

            emailSenderService.sendEmail(id);
            logger.info("Email sending process initiated for ID: {}", id);
            return ResponseEntity.ok("Email sending process initiated");
        } catch (Exception e) {
            logger.error("Error initiating email send: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }

    @PostMapping("/retry/{id}")
    public ResponseEntity<String> retryFailedEmail(@PathVariable String id) {
        try {
            emailSenderService.resendFailedEmail(id);
            return ResponseEntity.ok("Email retry initiated");
        } catch (Exception e) {
            logger.error("Error retrying email send: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to retry email: " + e.getMessage());
        }
    }

    @PostMapping("/upload-attachment")
    public ResponseEntity<List<String>> uploadAttachments(@RequestParam("files") List<MultipartFile> files) {
        List<String> fileNames = new ArrayList<>();
      
        try {
            // Ensure attachment storage directory exists
            File directory = new File(attachmentPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                // Generate a unique file name
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String newFilename = UUID.randomUUID().toString() + extension;

                // Save the file
                Path filepath = Paths.get(attachmentPath, newFilename);
                Files.write(filepath, file.getBytes());
              
                fileNames.add(newFilename);
                logger.info("Attachment uploaded: {}", newFilename);
            }

            return ResponseEntity.ok(fileNames);
        } catch (IOException e) {
            logger.error("Error uploading attachments: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // Add diagnostic endpoint to check MongoDB connection status
    @GetMapping("/mongo-status")
    public ResponseEntity<String> checkMongoStatus() {
        try {
            logger.info("Checking MongoDB connection status");
            String status = "MongoDB Connection Status:\n";

            // Test connection
            status += "Connection Test: ";
            try {
                mongoTemplate.getDb().getName();
                status += "SUCCESS\n";
            } catch (Exception e) {
                status += "FAILED - " + e.getMessage() + "\n";
                logger.error("MongoDB connection test failed", e);
            }

            // Test write operation
            status += "Write Test: ";
            try {
                org.bson.Document doc = new org.bson.Document();
                doc.put("test", "connection");
                doc.put("timestamp", new Date());
                mongoTemplate.getCollection("diagnostics").insertOne(doc);
                status += "SUCCESS\n";
            } catch (Exception e) {
                status += "FAILED - " + e.getMessage() + "\n";
                logger.error("MongoDB write test failed", e);
            }

            // Test read operation
            status += "Read Test: ";
            try {
                long count = mongoTemplate.getCollection("diagnostics").countDocuments();
                status += "SUCCESS - Found " + count + " diagnostic records\n";
            } catch (Exception e) {
                status += "FAILED - " + e.getMessage() + "\n";
                logger.error("MongoDB read test failed", e);
            }

            logger.info("MongoDB connection check completed");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error checking MongoDB status", e);
            return ResponseEntity.status(500).body("Error checking MongoDB status: " + e.getMessage());
        }
    }

    @PostMapping("/test-save")
    public ResponseEntity<String> testSaveEmail() {
        try {
            logger.info("Testing simple email save functionality");

            // Create a simple test email object
            Email testEmail = new Email();
            testEmail.setEmailName("Test Email " + System.currentTimeMillis());
            testEmail.setContentTemplateId("dummyTemplateId");
            testEmail.setCreatedBy("TEST");
            testEmail.setTo(new ArrayList<>(Arrays.asList("test@example.com")));
            testEmail.setCc(new ArrayList<>());
            testEmail.setStatus("DRAFT");
            testEmail.setAttachments(new ArrayList<>());

            // Ensure ID is null to let MongoDB generate it
            testEmail.setId(null);

            // Save the email
            logger.info("Saving test email: {}", testEmail);
            String id = emailService.saveEmail(testEmail);

          
            if (StringUtils.isBlank(id)) {
                logger.error("Test save failed: no ID returned");
                return ResponseEntity.status(500).body("Test save failed: no ID returned");
            }

            // Verify the email can be retrieved by its ID
            Email savedEmail = emailService.getEmailById(id);
            if (savedEmail == null) {
                logger.error("Test save failed: email could not be retrieved with ID: {}", id);
                return ResponseEntity.status(500).body("Test save succeeded but email could not be retrieved with ID: " + id);
            }

            logger.info("Test save successful with ID: {}", id);
            return ResponseEntity.ok("Test save successful with ID: " + id);
        } catch (Exception e) {
            logger.error("Error in test save: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error in test save: " + e.getMessage() + "\n" + e.toString());
        }
    }
}
