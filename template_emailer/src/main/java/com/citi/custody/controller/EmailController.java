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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private com.citi.custody.service.TemplateService templateService;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String sender;

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

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("接收到图片上传请求");
            
            if (file.isEmpty()) {
                logger.warn("上传的图片文件为空");
                return ResponseEntity.badRequest().body("请选择一个图片文件");
            }
            
            // 确保文件是图片
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                logger.warn("上传的文件不是图片类型: {}", contentType);
                return ResponseEntity.badRequest().body("仅支持上传图片文件");
            }
            
            // 创建图片存储目录
            String imageDir = attachmentPath + "/images";
            File directory = new File(imageDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // 保存图片文件
            Path filepath = Paths.get(imageDir, newFilename);
            Files.write(filepath, file.getBytes());
            
            logger.info("图片上传成功: {}", newFilename);
            
            // 返回图片的相对路径，可以在模板中使用
            return ResponseEntity.ok("images/" + newFilename);
        } catch (IOException e) {
            logger.error("图片上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("图片上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/check-image")
    public ResponseEntity<Map<String, Object>> checkImage(@RequestParam("path") String imagePath) {
        Map<String, Object> result = new HashMap<>();
        result.put("requestedPath", imagePath);
        
        try {
            logger.info("检查图片文件: {}", imagePath);
            
            // 创建图片存储目录的完整路径
            String imageDir = attachmentPath + "/images";
            result.put("imageDir", imageDir);
            
            // 检查各种可能的文件路径
            List<Map<String, Object>> fileChecks = new ArrayList<>();
            
            // 1. 检查直接路径
            File directFile = new File(imagePath);
            fileChecks.add(checkImageFile(directFile, "直接路径"));
            
            // 2. 检查相对于图片目录的路径
            File imageResourceFile = new File(imageDir, imagePath);
            fileChecks.add(checkImageFile(imageResourceFile, "图片目录相对路径"));
            
            // 3. 如果路径包含 "images/" 前缀，检查相对于附件根目录的路径
            if (imagePath.startsWith("images/")) {
                File attachmentPathFile = new File(attachmentPath, imagePath);
                fileChecks.add(checkImageFile(attachmentPathFile, "附件根目录相对路径"));
            }
            
            // 4. 如果只是文件名，检查在图片目录下是否存在
            if (!imagePath.contains("/")) {
                File fileNameOnly = new File(imageDir, imagePath);
                fileChecks.add(checkImageFile(fileNameOnly, "仅文件名"));
            }
            
            result.put("fileChecks", fileChecks);
            
            // 检查目录内容
            File imageFolder = new File(imageDir);
            if (imageFolder.exists() && imageFolder.isDirectory()) {
                List<Map<String, String>> files = new ArrayList<>();
                File[] listFiles = imageFolder.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        Map<String, String> fileInfo = new HashMap<>();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("size", file.length() + " bytes");
                        fileInfo.put("lastModified", new Date(file.lastModified()).toString());
                        files.add(fileInfo);
                    }
                }
                result.put("directoryListing", files);
            } else {
                result.put("directoryExists", false);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("检查图片时出错: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    private Map<String, Object> checkImageFile(File file, String pathType) {
        Map<String, Object> check = new HashMap<>();
        check.put("pathType", pathType);
        check.put("path", file.getAbsolutePath());
        check.put("exists", file.exists());
        
        if (file.exists()) {
            check.put("isFile", file.isFile());
            check.put("isDirectory", file.isDirectory());
            check.put("size", file.length() + " bytes");
            check.put("lastModified", new Date(file.lastModified()).toString());
            check.put("canRead", file.canRead());
        }
        
        return check;
    }

    @GetMapping("/test-template")
    public ResponseEntity<String> testTemplate(@RequestParam("id") String templateId) {
        try {
            logger.info("测试模板内容转换，模板ID: {}", templateId);
            
            // 获取模板
            com.citi.custody.entity.TemplateInfo template = templateService.getTemplateById(templateId);
            
            if (template == null) {
                return ResponseEntity.status(404).body("未找到模板: " + templateId);
            }
            
            if (template.getContent() == null) {
                return ResponseEntity.status(400).body("模板内容为空");
            }
            
            // 转换为HTML
            String htmlContent = com.citi.custody.util.JsonToHtmlConverter.convertJsonToHtml(template.getContent());
            
            // 打印JSON和HTML以便调试
            logger.info("模板原始JSON:\n{}", template.getContent());
            logger.info("转换后的HTML:\n{}", htmlContent);
            
            return ResponseEntity.ok(htmlContent);
        } catch (Exception e) {
            logger.error("测试模板转换时出错: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("转换失败: " + e.getMessage());
        }
    }

    @PostMapping("/test-send")
    public ResponseEntity<String> testSendEmail(@RequestParam("id") String emailId) {
        try {
            logger.info("测试发送邮件，ID: {}", emailId);
            
            // 获取邮件
            Email email = emailService.getEmailById(emailId);
            if (email == null) {
                return ResponseEntity.status(404).body("未找到邮件: " + emailId);
            }
            
            // 确保测试目录存在
            File attachDir = new File(attachmentPath);
            if (!attachDir.exists()) {
                attachDir.mkdirs();
                logger.info("创建附件目录: {}", attachDir.getAbsolutePath());
            }
            
            File imagesDir = new File(attachmentPath + "/images");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
                logger.info("创建图片目录: {}", imagesDir.getAbsolutePath());
            }
            
            // 发送邮件
            emailSenderService.sendEmail(emailId);
            
            return ResponseEntity.ok("邮件发送请求已提交，请查看日志了解详情");
        } catch (Exception e) {
            logger.error("测试发送邮件时出错: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/manage-images")
    public ResponseEntity<Map<String, Object>> manageImages(@RequestParam("action") String action) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", action);
        
        try {
            String imageDir = attachmentPath + "/images";
            File imagesFolder = new File(imageDir);
            
            switch (action.toLowerCase()) {
                case "check":
                    // 检查图片目录是否存在
                    result.put("directoryExists", imagesFolder.exists());
                    if (imagesFolder.exists()) {
                        result.put("isDirectory", imagesFolder.isDirectory());
                        result.put("canRead", imagesFolder.canRead());
                        result.put("canWrite", imagesFolder.canWrite());
                        
                        // 列出所有图片文件
                        File[] files = imagesFolder.listFiles();
                        List<Map<String, Object>> fileInfos = new ArrayList<>();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile()) {
                                    Map<String, Object> fileInfo = new HashMap<>();
                                    fileInfo.put("name", file.getName());
                                    fileInfo.put("path", file.getAbsolutePath());
                                    fileInfo.put("size", file.length());
                                    fileInfo.put("lastModified", new Date(file.lastModified()));
                                    fileInfo.put("canRead", file.canRead());
                                    // 检测文件 MIME 类型
                                    String extension = file.getName();
                                    int dotIndex = extension.lastIndexOf('.');
                                    if (dotIndex > 0) {
                                        extension = extension.substring(dotIndex + 1).toLowerCase();
                                    }
                                    fileInfo.put("extension", extension);
                                    fileInfos.add(fileInfo);
                                }
                            }
                        }
                        result.put("files", fileInfos);
                        result.put("fileCount", fileInfos.size());
                    }
                    break;
                    
                case "rebuild":
                    // 清空并重建图片目录
                    if (imagesFolder.exists()) {
                        // 清空目录中的所有文件
                        File[] files = imagesFolder.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile()) {
                                    boolean deleted = file.delete();
                                    logger.info("删除文件: {} - {}", file.getName(), deleted ? "成功" : "失败");
                                }
                            }
                        }
                        result.put("cleaned", true);
                    } else {
                        imagesFolder.mkdirs();
                        result.put("created", true);
                    }
                    break;
                
                default:
                    return ResponseEntity.badRequest().body(Collections.singletonMap("error", "未知操作: " + action));
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("管理图片目录时出错: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @PostMapping("/test-image-email")
    public ResponseEntity<String> testImageEmail(
            @RequestParam("to") String to,
            @RequestParam("imageFile") String imageFile) {
        try {
            // 验证参数
            if (StringUtils.isBlank(to)) {
                return ResponseEntity.badRequest().body("收件人不能为空");
            }
            
            if (StringUtils.isBlank(imageFile)) {
                return ResponseEntity.badRequest().body("图片文件不能为空");
            }
            
            // 确保图片目录存在
            String imageDir = attachmentPath + "/images";
            File imagesFolder = new File(imageDir);
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs();
            }
            
            // 检查图片文件是否存在
            File imageFileObj = new File(imageDir, imageFile);
            if (!imageFileObj.exists() || !imageFileObj.isFile()) {
                return ResponseEntity.status(404).body("图片文件不存在: " + imageFile);
            }
            
            logger.info("开始测试图片邮件发送...");
            logger.info("收件人: {}", to);
            logger.info("图片文件: {}", imageFileObj.getAbsolutePath());
            
            // 创建并发送带图片的测试邮件
            String emailId = createAndSendTestImageEmail(to, imageFileObj);
            
            return ResponseEntity.ok("测试邮件已发送，ID: " + emailId);
        } catch (Exception e) {
            logger.error("发送测试图片邮件时出错: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("发送测试邮件失败: " + e.getMessage());
        }
    }
    
    private String createAndSendTestImageEmail(String to, File imageFile) throws Exception {
        // 创建测试邮件
        Email email = new Email();
        email.setEmailName("测试内嵌图片邮件 - " + new Date());
        email.setTo(Arrays.asList(to.split(",")));
        email.setCc(new ArrayList<>());
        email.setCreatedBy("SYSTEM");
        email.setStatus("DRAFT");
        
        // 生成一个简单的HTML内容，包含图片引用
        String imageName = imageFile.getName();
        String contentId = imageName.replaceAll("[^a-zA-Z0-9.]", "_");
        if (contentId.contains(".")) {
            contentId = contentId.substring(0, contentId.lastIndexOf('.'));
        }
        contentId = contentId + "_img";
        
        logger.info("测试邮件使用图片: {}, 生成的Content-ID: {}", imageName, contentId);
        
        // 创建一个简单的模板并保存
        String templateContent = "{\n" +
            "  \"body\": {\n" +
            "    \"rows\": [\n" +
            "      {\n" +
            "        \"columns\": [\n" +
            "          {\n" +
            "            \"contents\": [\n" +
            "              {\n" +
            "                \"type\": \"heading\",\n" +
            "                \"values\": {\n" +
            "                  \"text\": \"测试内嵌图片\"\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"type\": \"text\",\n" +
            "                \"values\": {\n" +
            "                  \"text\": \"这是一个测试内嵌图片的邮件\"\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"type\": \"image\",\n" +
            "                \"values\": {\n" +
            "                  \"src\": \"" + imageName + "\",\n" +
            "                  \"text\": \"测试图片\"\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"type\": \"text\",\n" +
            "                \"values\": {\n" +
            "                  \"text\": \"如果您能看到上面的图片，则说明内嵌图片功能正常工作。\"\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
        
        logger.info("创建的测试模板内容: {}", templateContent);
        
        // 保存模板并获取ID
        com.citi.custody.entity.TemplateInfo templateInfo = new com.citi.custody.entity.TemplateInfo();
        templateInfo.setFilename("测试图片模板");
        templateInfo.setUpdateBy("SYSTEM");
        templateInfo.setUpdateTime(new Date());
        templateInfo.setContent(templateContent);
        
        String templateId = templateService.storeTestTemplate(templateInfo);
        logger.info("创建测试模板成功，ID: {}", templateId);
        
        // 设置邮件模板ID
        email.setContentTemplateId(templateId);
        
        // 保存邮件
        String emailId = emailService.saveEmail(email);
        logger.info("创建测试邮件成功，ID: {}", emailId);
        
        // 发送邮件
        emailSenderService.sendEmail(emailId);
        logger.info("已触发测试邮件发送，ID: {}", emailId);
        
        return emailId;
    }

    @PostMapping("/direct-image-test")
    public ResponseEntity<Map<String, Object>> directImageTest(
            @RequestParam("to") String to,
            @RequestParam("imageFile") String imageFile) {
        Map<String, Object> result = new HashMap<>();
        result.put("to", to);
        result.put("imageFile", imageFile);
        
        try {
            logger.info("开始直接图片邮件测试...");
            
            // 验证参数
            if (StringUtils.isBlank(to)) {
                throw new IllegalArgumentException("收件人不能为空");
            }
            
            if (StringUtils.isBlank(imageFile)) {
                throw new IllegalArgumentException("图片文件名不能为空");
            }
            
            // 确保图片目录存在
            String imageDir = attachmentPath + "/images";
            File imagesFolder = new File(imageDir);
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs();
                result.put("imagesFolderCreated", true);
            }
            
            // 构建图片文件路径
            File imageFileObj = new File(imagesFolder, imageFile);
            if (!imageFileObj.exists()) {
                throw new IllegalArgumentException("图片文件不存在: " + imageFileObj.getAbsolutePath());
            }
            
            result.put("imageFilePath", imageFileObj.getAbsolutePath());
            result.put("imageFileSize", imageFileObj.length());
            result.put("imageFileExists", true);
            
            // 直接创建MimeMessage并发送
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 设置基本属性
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject("直接图片测试邮件 - " + new Date());
            
            // 创建简单HTML内容，引用图片
            String contentId = imageFile.replaceAll("[^a-zA-Z0-9.]", "_");
            if (contentId.contains(".")) {
                contentId = contentId.substring(0, contentId.lastIndexOf('.'));
            }
            contentId = contentId + "_img";
            
            String htmlContent = "<html><body>" +
                "<h1>图片内嵌测试</h1>" +
                "<p>这是一个直接测试内嵌图片的邮件。如果您看到下面的图片，则说明功能正常。</p>" +
                "<img src='cid:" + contentId + "' alt='测试图片' style='max-width:100%'/>" +
                "<p>图片文件名: " + imageFile + "</p>" +
                "<p>测试时间: " + new Date() + "</p>" +
                "</body></html>";
            
            helper.setText(htmlContent, true);
            
            // 添加内嵌图片
            FileSystemResource resource = new FileSystemResource(imageFileObj);
            String mimeType = emailSenderService.determineMimeType(imageFile);
            helper.addInline(contentId, resource, mimeType);
            
            result.put("contentId", contentId);
            result.put("mimeType", mimeType);
            
            // 发送邮件
            mailSender.send(message);
            
            result.put("success", true);
            result.put("htmlContent", htmlContent);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("直接图片测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @PostMapping("/simple-image-test")
    public ResponseEntity<Map<String, Object>> simpleImageTest(
            @RequestParam("to") String to,
            @RequestParam("imageFile") String imageFile) {
        Map<String, Object> result = new HashMap<>();
        result.put("to", to);
        result.put("imageFile", imageFile);
        
        try {
            logger.info("开始简单图片邮件测试...");
            
            // 验证参数
            if (StringUtils.isBlank(to)) {
                throw new IllegalArgumentException("收件人不能为空");
            }
            
            if (StringUtils.isBlank(imageFile)) {
                throw new IllegalArgumentException("图片文件名不能为空");
            }
            
            // 确保图片目录存在
            String imageDir = attachmentPath + "/images";
            File imagesFolder = new File(imageDir);
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs();
                result.put("imagesFolderCreated", true);
            }
            
            // 构建图片文件路径
            File imageFileObj = new File(imagesFolder, imageFile);
            if (!imageFileObj.exists()) {
                throw new IllegalArgumentException("图片文件不存在: " + imageFileObj.getAbsolutePath());
            }
            
            result.put("imageFilePath", imageFileObj.getAbsolutePath());
            result.put("imageFileSize", imageFileObj.length());
            result.put("imageFileExists", true);
            
            // 打印图片目录内容
            logger.info("图片目录内容:");
            File[] files = imagesFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    logger.info(" - {} ({}KB)", file.getName(), file.length()/1024);
                }
            }
            
            // 直接创建MimeMessage并发送
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 设置基本属性
            String cleanSender = sender.trim().replaceAll("\\s+", "");
            helper.setFrom(cleanSender);
            helper.setTo(to);
            helper.setSubject("超简单图片测试邮件 - " + new Date());
            
            // 创建简单HTML内容，引用图片
            String contentId = imageFile;
            if (contentId.contains(".")) {
                contentId = contentId.substring(0, contentId.lastIndexOf('.'));
            }
            
            // 添加标识符以区分不同的图片
            contentId = contentId + "_simple_test";
            
            logger.info("使用ContentId: {}", contentId);
            
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <title>简单图片邮件测试</title>" +
                "</head>" +
                "<body>" +
                "  <h1 style=\"color: blue;\">超简单图片测试</h1>" +
                "  <p>这是一个<strong>非常简单</strong>的HTML邮件，包含一个图片：</p>" +
                "  <div style=\"border: 1px solid #ccc; padding: 10px; text-align: center;\">" +
                "    <img src=\"cid:" + contentId + "\" alt=\"测试图片\" style=\"max-width: 100%; height: auto;\">" +
                "  </div>" +
                "  <p>图片信息:</p>" +
                "  <ul>" +
                "    <li>文件名: " + imageFile + "</li>" +
                "    <li>Content-ID: " + contentId + "</li>" +
                "    <li>发送时间: " + new Date() + "</li>" +
                "  </ul>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            logger.info("设置HTML内容: {}", htmlContent);
            
            // 添加内嵌图片
            FileSystemResource resource = new FileSystemResource(imageFileObj);
            String mimeType = emailSenderService.determineMimeType(imageFile);
            helper.addInline(contentId, resource, mimeType);
            
            logger.info("添加内嵌图片: contentId={}, 文件路径={}, MIME类型={}", 
                contentId, imageFileObj.getAbsolutePath(), mimeType);
            
            // 发送邮件
            mailSender.send(message);
            logger.info("邮件已发送");
            
            result.put("success", true);
            result.put("contentId", contentId);
            result.put("mimeType", mimeType);
            result.put("htmlContent", htmlContent);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("简单图片测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
