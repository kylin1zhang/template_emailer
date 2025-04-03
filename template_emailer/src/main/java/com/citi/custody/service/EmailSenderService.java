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
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
            // 使用第三个参数为true来启用multipart模式，这对内嵌图片很重要
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 设置发件人地址，确保地址格式正确
            try {
                // 清理邮件地址中的空格和控制字符
                String cleanSender = sender.trim().replaceAll("\\s+", "");
                helper.setFrom(cleanSender);
                logger.debug("发件人地址设置为: {}", cleanSender);
            } catch (MessagingException e) {
                logger.error("设置发件人地址时出错: {}", e.getMessage(), e);
                email.setStatus("FAILED");
                email.setErrorMessage("无效的发件人地址: " + sender);
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

            // 先处理模板中的内嵌图片，确保图片在内容设置前已准备好
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
                logger.warn("无法处理模板中的内嵌图片: {}", e.getMessage());
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
            
            // 设置邮件内容
            String emailContent = content;
            logger.debug("正在设置邮件内容: 长度={}", emailContent.length());

            // 简化设置，只使用一次setText方法，避免重复设置造成的问题
            helper.setText(emailContent, true);
            // 不要添加额外的头信息，避免混淆邮件客户端
            logger.debug("邮件内容设置完成");

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
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            String nodeType = node.path("type").asText();
            
            // 特殊处理HTML节点，查找<img>标签
            if ("html".equals(nodeType) && node.has("values") && node.path("values").has("html")) {
                String htmlContent = node.path("values").path("html").asText();
                processImagesInHtml(htmlContent, helper, imageResourcePath);
            }
            
            // 处理图片节点
            if ("image".equals(nodeType) && node.has("values") && node.path("values").has("src")) {
                String imageUrl = node.path("values").path("src").asText();
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("data:")) {
                    String imgFileName = getFileNameFromPath(imageUrl);
                    if (imgFileName != null && !imgFileName.isEmpty()) {
                        String contentId = generateContentId(imgFileName);
                        processImageFile(imgFileName, imageUrl, contentId, helper, imageResourcePath);
                    }
                }
            }

            // 递归处理所有字段
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                processImagesInJsonNode(entry.getValue(), helper, imageResourcePath);
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                processImagesInJsonNode(element, helper, imageResourcePath);
            }
        }
    }

    // 添加新方法：处理HTML内容中的图片
    private void processImagesInHtml(String htmlContent, MimeMessageHelper helper, String imageResourcePath) throws Exception {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return;
        }
        
        // 使用正则表达式查找所有<img>标签的src属性
        Pattern pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlContent);
        
        while (matcher.find()) {
            String imageUrl = matcher.group(1);
            // 只处理本地图片（非http/https/data:开头的URL）
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("data:")) {
                String imgFileName = getFileNameFromPath(imageUrl);
                if (imgFileName != null && !imgFileName.isEmpty()) {
                    String contentId = generateContentId(imgFileName);
                    logger.debug("Found image in HTML: {}, contentId: {}", imgFileName, contentId);
                    processImageFile(imgFileName, imageUrl, contentId, helper, imageResourcePath);
                }
            }
        }
    }

    // 从路径中提取文件名
    private String getFileNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
            return path.substring(lastSlashIndex + 1);
        }
        
        return path;
    }

    // 生成ContentID
    private String generateContentId(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_") + "@" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 简化版的图片处理方法
     */
    private void processImageFile(String imgFileName, String imageUrl, String contentId, 
                                 MimeMessageHelper helper, String imageResourcePath) {
        logger.debug("处理图片: {} -> {}", imgFileName, contentId);
        
        try {
            // 尝试多个可能的图片路径
            File imageFile = null;
            String[] possiblePaths = {
                imageUrl,  // 直接使用提供的路径
                imageResourcePath + File.separator + imgFileName,  // 图片资源目录
                attachmentPath + File.separator + imgFileName,  // 附件根目录
                attachmentPath + File.separator + "images" + File.separator + imgFileName  // 附件下的images目录
            };
            
            for (String path : possiblePaths) {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    imageFile = file;
                    break;
                }
            }
            
            if (imageFile != null && imageFile.exists()) {
                // 确定图片的MIME类型
                String mimeType = determineMimeType(imgFileName);
                
                // 添加内联图片
                helper.addInline(contentId, imageFile, mimeType);
                
                logger.debug("成功添加内联图片: {}", contentId);
            } else {
                logger.warn("找不到图片文件: {}", imgFileName);
            }
        } catch (Exception e) {
            logger.error("处理图片时出错 {}: {}", imgFileName, e.getMessage());
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
