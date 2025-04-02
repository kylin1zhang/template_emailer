package com.citi.custody.service;

import com.citi.custody.util.JsonToHtmlConverter;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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

            // Schedule for future sending
            if (email.getSentTime() != null && email.getSentTime().after(now)) {
                email.setStatus("SCHEDULED");
                emailDao.saveEmail(email);
                logger.info("Email scheduled for future sending: {}", email.getId());
                return;
            }

            // Test Mode: Do not send email, just update the status
            if (testMode) {
                logger.info("TEST MODE: Email would be sent to: {} with subject: {}", email.getTo(),
                        email.getEmailName());
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

            // 设置发件人地址，确保地址格式正确
            try {
                // 清理邮件地址中的空格和控制字符
                String cleanSender = sender.trim().replaceAll("\\s+", "");
                helper.setFrom(cleanSender);
                logger.debug("Sender address set to: {}", cleanSender);
            } catch (MessagingException e) {
                logger.error("Error setting sender address: {}", e.getMessage(), e);
                email.setStatus("FAILED");
                email.setErrorMessage("Invalid sender address: " + sender);
                emailDao.saveEmail(email);
                throw e;
            }
            
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

            // Set content from template or default content
            String content = "This is an automated email.";
            try {
                if (template != null) {
                    if (template.getContent() != null) {
                        logger.debug("Converting template content to HTML for email: {}", email.getId());
                        content = JsonToHtmlConverter.convertJsonToHtml(template.getContent());
                        logger.debug("Template content converted successfully");
                    } else {
                        logger.warn("Template {} has null content, using default content for email: {}", 
                            template.getId(), email.getId());
                    }
                } else {
                    logger.warn("No template found for templateId: {}, using default content for email: {}", 
                        email.getContentTemplateId(), email.getId());
                }
            } catch (Exception e) {
                logger.error("Error converting template content to HTML for email {}: {}", 
                    email.getId(), e.getMessage(), e);
                content = "<html><body><p>Error parsing template: " + e.getMessage() + "</p></body></html>";
            }
            helper.setText(content, true); // true indicates HTML content
            
            // 处理模板中的内嵌图片
            try {
                if (template != null && template.getContent() != null) {
                    // 解析JSON以找出可能的图片引用
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(template.getContent());
                    
                    // 创建一个图片资源目录
                    String imageResourcePath = attachmentPath + "/images";
                    File imageDir = new File(imageResourcePath);
                    if (!imageDir.exists()) {
                        imageDir.mkdirs();
                    }
                    
                    // 查找并处理所有图片节点
                    processImagesInJsonNode(rootNode, helper, imageResourcePath);
                }
            } catch (Exception e) {
                logger.warn("Unable to process inline images in template: {}", e.getMessage());
            }

            // Add attachments if any
            if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
                for (String attachment : email.getAttachments()) {
                    File file = new File(attachmentPath + "/" + attachment);
                    if (file.exists()) {
                        FileSystemResource resource = new FileSystemResource(file);
                        helper.addAttachment(file.getName(), resource);
                        logger.info("Attachment added: {}", file.getName());
                    } else {
                        logger.warn("Attachment not found: {}", file.getAbsolutePath());
                    }
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
                logger.error("Failed to create email message: {}", e.getMessage(), e);
                email.setStatus("FAILED");
                email.setErrorMessage(e.getMessage());
                emailDao.saveEmail(email);

                // 如果是身份验证错误，给出更详细的提示

                if (e.getMessage() != null && e.getMessage().contains("Authentication")) {
                    logger.error(
                            "Mail server authentication failed. Please check your mailbox settings to ensure SMTP access is allowed or an app password is configured.");
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

    /**
     * 递归处理JSON节点中的所有图片
     */
    private void processImagesInJsonNode(JsonNode node, MimeMessageHelper helper, String imageResourcePath) throws Exception {
        // 在处理开始时输出整个节点结构，便于调试
        if (node.isObject() && node.has("body")) {
            logger.info("Processing template JSON structure: {}", node.toString().substring(0, Math.min(200, node.toString().length())) + "...");
        }
        
        if (node.isObject()) {
            // 检查当前节点是否为图片类型
            if (node.has("type") && "image".equals(node.get("type").asText())) {
                logger.info("Found image node: {}", node.toString());
                
                JsonNode values = null;
                if (node.has("values")) {
                    values = node.get("values");
                }
                
                String imageUrl = null;
                
                // 尝试从不同的属性中获取图片URL
                if (values != null) {
                    if (values.has("src") && !values.get("src").isNull()) {
                        if (values.get("src").isObject() && values.get("src").has("url")) {
                            // 处理 src 是一个对象的情况，取其中的 url 属性
                            imageUrl = values.get("src").get("url").asText();
                            logger.info("Found image URL from src.url property: {}", imageUrl);
                        } else {
                            // 直接获取 src 的值
                            imageUrl = values.get("src").asText();
                            logger.info("Found image URL from src property: {}", imageUrl);
                        }
                    } else if (values.has("url") && !values.get("url").isNull()) {
                        imageUrl = values.get("url").asText();
                        logger.info("Found image URL from url property: {}", imageUrl);
                    } else if (values.has("href") && !values.get("href").isNull()) {
                        imageUrl = values.get("href").asText();
                        logger.info("Found image URL from href property: {}", imageUrl);
                    }
                }
                
                // 处理找到的图片URL
                if (imageUrl != null && !imageUrl.isEmpty() && 
                    !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("data:")) {
                    logger.info("Processing local image: {}", imageUrl);
                    
                    // 提取图片文件名
                    String imgFileName = imageUrl;
                    if (imageUrl.contains("/")) {
                        imgFileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    }
                    
                    // 使用与 JsonToHtmlConverter 完全相同的方式生成 Content-ID
                    String contentId = imgFileName.replaceAll("[^a-zA-Z0-9.]", "_");
                    
                    // 注意：确保contentId不包含扩展名的点号，与JsonToHtmlConverter保持一致
                    if (contentId.contains(".")) {
                        contentId = contentId.substring(0, contentId.lastIndexOf('.'));
                    }
                    
                    // 添加固定的后缀，与 JsonToHtmlConverter 保持一致
                    contentId = contentId + "_img";
                    
                    logger.info("Image filename: {}, Generated ContentId: {}", imgFileName, contentId);
                    
                    try {
                        // 尝试从多个位置加载图片
                        File imageFile = null;
                        
                        // 1. 检查是否为绝对路径
                        File absoluteFile = new File(imageUrl);
                        if (absoluteFile.exists() && absoluteFile.isFile()) {
                            imageFile = absoluteFile;
                            logger.info("Found image from absolute path: {}", absoluteFile.getAbsolutePath());
                        }
                        
                        // 2. 检查图片是否在 images 目录下
                        if (imageFile == null) {
                            File imageResourceFile = new File(imageResourcePath, imgFileName);
                            if (imageResourceFile.exists() && imageResourceFile.isFile()) {
                                imageFile = imageResourceFile;
                                logger.info("Found image in image resource directory: {}", imageResourceFile.getAbsolutePath());
                            }
                        }
                        
                        // 3. 如果路径包含 images/ 前缀，检查相对于附件根目录的路径
                        if (imageFile == null && imageUrl.startsWith("images/")) {
                            File attachmentPathFile = new File(attachmentPath, imageUrl);
                            if (attachmentPathFile.exists() && attachmentPathFile.isFile()) {
                                imageFile = attachmentPathFile;
                                logger.info("Found image in attachment root directory: {}", attachmentPathFile.getAbsolutePath());
                            }
                        }
                        
                        // 4. 额外尝试附件根目录下的图片目录
                        if (imageFile == null) {
                            File imagesDir = new File(attachmentPath, "images");
                            File imageInImagesDir = new File(imagesDir, imgFileName);
                            if (imageInImagesDir.exists() && imageInImagesDir.isFile()) {
                                imageFile = imageInImagesDir;
                                logger.info("Found image in images directory under attachment root: {}", imageInImagesDir.getAbsolutePath());
                            }
                        }
                        
                        if (imageFile != null) {
                            FileSystemResource resource = new FileSystemResource(imageFile);
                            
                            // 尝试确定图片的MIME类型
                            String mimeType = determineMimeType(imageFile.getName());
                            logger.info("MIME type for image {}: {}", imageFile.getName(), mimeType);
                            
                            // 添加内联图片附件，设置Content-ID
                            helper.addInline(contentId, resource, mimeType);
                            
                            logger.info("Successfully added inline image: FilePath={}, contentId={}, FileSize={}KB, FileType={}", 
                                imageFile.getAbsolutePath(), contentId, imageFile.length()/1024, 
                                getFileExtension(imageFile.getName()));
                        } else {
                            logger.error("Could not find image file: {}. Locations checked: absolute path, {}, {}", 
                                imageUrl, imageResourcePath, attachmentPath + "/images");
                            
                            // 列出图片目录内容以帮助调试
                            File imagesDir = new File(attachmentPath, "images");
                            if (imagesDir.exists() && imagesDir.isDirectory()) {
                                File[] files = imagesDir.listFiles();
                                if (files != null && files.length > 0) {
                                    logger.info("Image directory contains the following files:");
                                    for (File f : files) {
                                        logger.info(" - {} ({}KB)", f.getName(), f.length()/1024);
                                    }
                                } else {
                                    logger.info("Image directory is empty or files could not be listed");
                                }
                            } else {
                                logger.info("Image directory does not exist: {}", imagesDir.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error adding inline image: {}", e.getMessage(), e);
                    }
                }
            }
            
            // 处理所有字段
            node.fields().forEachRemaining(entry -> {
                try {
                    processImagesInJsonNode(entry.getValue(), helper, imageResourcePath);
                } catch (Exception e) {
                    logger.error("Error processing image field: {}", e.getMessage(), e);
                }
            });
        } else if (node.isArray()) {
            // 处理数组中的每个元素
            for (JsonNode item : node) {
                processImagesInJsonNode(item, helper, imageResourcePath);
            }
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return null;
        }
        return fileName.substring(dotIndex + 1);
    }

    public String determineMimeType(String fileName) {
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return "application/octet-stream";
        }
        
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "tiff":
            case "tif":
                return "image/tiff";
            case "ico":
                return "image/x-icon";
            default:
                return "application/octet-stream";
        }
    }
}
