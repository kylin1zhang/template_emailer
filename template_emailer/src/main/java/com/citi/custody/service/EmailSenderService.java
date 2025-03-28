package com.citi.custody.service;

import com.citi.custody.dao.EmailDao;
import com.citi.custody.entity.Email;
import com.citi.custody.entity.TemplateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Date;
import java.util.List;

@Service
public class EmailSenderService {
    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private EmailDao emailDao;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${attachment.storage.path:/temp/attachments}")
    private String attachmentPath;
    
    @Value("${email.test.mode:false}")
    private boolean testMode;

    public void sendEmail(String emailId) {
        Email email = emailDao.findEmailById(emailId);
        if (email == null) {
            logger.error("Email not found with id: {}", emailId);
            return;
        }

        try {
            Date now = new Date();
            if (email.getSentTime() != null && email.getSentTime().after(now)) {
                // Schedule for future sending
                email.setStatus("SCHEDULED");
                emailDao.saveEmail(email);
                logger.info("Email scheduled for future sending: {}", email.getId());
                return;
            }
            
            // 测试模式：不实际发送邮件，只更新状态
            if (testMode) {
                logger.info("TEST MODE: Email would be sent to: {} with subject: {}", 
                    email.getTo(), email.getEmailName());
                email.setStatus("SENT");
                email.setSentTime(now);
                emailDao.saveEmail(email);
                logger.info("TEST MODE: Email marked as sent: {}", email.getId());
                return;
            }

            // Fetch template
            TemplateInfo template = null;
            if (email.getContentTemplateId() != null && !email.getContentTemplateId().isEmpty()) {
                template = templateService.getTemplateById(email.getContentTemplateId());
            }

            // Create mail message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setSubject(email.getEmailName());
            
            // Set recipients
            if (email.getTo() != null && !email.getTo().isEmpty()) {
                helper.setTo(email.getTo().toArray(new String[0]));
            } else {
                logger.warn("No recipients specified for email: {}", email.getId());
                throw new MessagingException("No recipients specified");
            }
            
            if (email.getCc() != null && !email.getCc().isEmpty()) {
                helper.setCc(email.getCc().toArray(new String[0]));
            }

            // Set content from template or default
            String content = "This is an automated email.";
            if (template != null && template.getContent() != null) {
                content = template.getContent();
            }
            helper.setText(content, true); // true indicates HTML content

            // Add attachments if any
            if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
                boolean hasValidAttachments = false;
                for (String attachment : email.getAttachments()) {
                    File file = new File(attachmentPath + "/" + attachment);
                    if (file.exists()) {
                        FileSystemResource resource = new FileSystemResource(file);
                        helper.addAttachment(file.getName(), resource);
                        hasValidAttachments = true;
                    } else {
                        logger.warn("Attachment not found: {}", attachment);
                    }
                }
                
                if (!hasValidAttachments && !email.getAttachments().isEmpty()) {
                    logger.warn("None of the specified attachments were found for email: {}", email.getId());
                }
            }

            try {
                // Send email
                mailSender.send(message);
                
                // Update email status
                email.setStatus("SENT");
                email.setSentTime(now);
                email.setErrorMessage(null);
                emailDao.saveEmail(email);
                
                logger.info("Email sent successfully: {}", email.getId());
            } catch (Exception e) {
                logger.error("Failed to send email: {}", e.getMessage(), e);
                email.setStatus("FAILED");
                email.setErrorMessage(e.getMessage());
                emailDao.saveEmail(email);
                
                // 如果是身份验证错误，给出更详细的提示
                if (e.getMessage() != null && e.getMessage().contains("Authentication")) {
                    logger.error("邮件服务器身份验证失败。请检查邮箱设置，确保允许SMTP访问或已配置应用密码。");
                }
            }
        } catch (MessagingException e) {
            logger.error("Failed to create email message: {}", e.getMessage(), e);
            email.setStatus("FAILED");
            email.setErrorMessage(e.getMessage());
            emailDao.saveEmail(email);
        } catch (Exception e) {
            logger.error("Unexpected error when sending email: {}", e.getMessage(), e);
            email.setStatus("FAILED");
            email.setErrorMessage("Unexpected error: " + e.getMessage());
            emailDao.saveEmail(email);
        }
    }
    
    public void resendFailedEmail(String emailId) {
        Email email = emailDao.findEmailById(emailId);
        if (email != null && "FAILED".equals(email.getStatus())) {
            email.setErrorMessage(null);
            emailDao.saveEmail(email);
            sendEmail(emailId);
        } else if (email == null) {
            logger.error("Cannot resend - email not found with id: {}", emailId);
        } else {
            logger.warn("Cannot resend - email is not in FAILED status: {}", emailId);
        }
    }
} 