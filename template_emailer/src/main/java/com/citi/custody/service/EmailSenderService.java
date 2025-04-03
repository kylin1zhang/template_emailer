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
            
            // Add additional headers for Outlook compatibility
            MimeMessage mimeMessage = helper.getMimeMessage();
            mimeMessage.addHeader("X-Unsent", "1");
            mimeMessage.addHeader("X-Priority", "3");
            // Add Content-Type header to specify HTML content with UTF-8 encoding
            mimeMessage.addHeader("Content-Type", "text/html; charset=UTF-8");
            
            // Use setText with true parameter to indicate HTML content
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
                logger.warn("无法处理模板中的内嵌图片: {}", e.getMessage());
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
            logger.info("处理模板JSON结构: {}", node.toString().substring(0, Math.min(200, node.toString().length())) + "...");
        }
        
        if (node.isObject()) {
            // 检查当前节点是否为图片类型
            if (node.has("type") && "image".equals(node.get("type").asText())) {
                logger.info("发现图片节点: {}", node.toString());
                
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
                            logger.info("从src.url属性找到图片URL: {}", imageUrl);
                        } else {
                            // 直接获取 src 的值
                            imageUrl = values.get("src").asText();
                            logger.info("从src属性找到图片URL: {}", imageUrl);
                        }
                    } else if (values.has("url") && !values.get("url").isNull()) {
                        imageUrl = values.get("url").asText();
                        logger.info("从url属性找到图片URL: {}", imageUrl);
                    } else if (values.has("href") && !values.get("href").isNull()) {
                        imageUrl = values.get("href").asText();
                        logger.info("从href属性找到图片URL: {}", imageUrl);
                    }
                }
                
                // 处理找到的图片URL
                if (imageUrl != null && !imageUrl.isEmpty() && 
                    !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("data:")) {
                    logger.info("正在处理本地图片: {}", imageUrl);
                    
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
                    
                    logger.info("图片文件名: {}, 生成的ContentId: {}", imgFileName, contentId);
                    
                    // 使用提取的公共方法处理图片
                    processImageFile(imgFileName, imageUrl, contentId, helper, imageResourcePath);
                } else if (node.has("type") && "html".equals(node.get("type").asText())) {
                    // 处理HTML类型的节点，查找其中的图片标签
                    String htmlContent = "";
                    if (node.has("values") && !node.get("values").isNull()) {
                        JsonNode htmlValues = node.get("values");
                        if (htmlValues.has("html") && !htmlValues.get("html").isNull()) {
                            htmlContent = htmlValues.get("html").asText();
                        } else if (htmlValues.has("text") && !htmlValues.get("text").isNull()) {
                            htmlContent = htmlValues.get("text").asText();
                        }
                    }
                    
                    // 处理HTML中的img标签
                    if (!htmlContent.isEmpty()) {
                        logger.info("Processing HTML content for images: {}", 
                            htmlContent.length() > 100 ? htmlContent.substring(0, 100) + "..." : htmlContent);
                        
                        // 简单解析HTML查找img标签
                        // 这里使用正则表达式来匹配img标签的src属性
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
                        java.util.regex.Matcher matcher = pattern.matcher(htmlContent);
                        
                        while (matcher.find()) {
                            String htmlImageUrl = matcher.group(1);
                            logger.info("Found image URL in HTML content: {}", htmlImageUrl);
                            
                            // 只处理非HTTP和HTTPS的本地图片
                            if (htmlImageUrl != null && !htmlImageUrl.isEmpty() && 
                                !htmlImageUrl.startsWith("http://") && !htmlImageUrl.startsWith("https://") && !htmlImageUrl.startsWith("data:")) {
                                
                                // 提取图片文件名
                                String htmlImgFileName = htmlImageUrl;
                                if (htmlImageUrl.contains("/")) {
                                    htmlImgFileName = htmlImageUrl.substring(htmlImageUrl.lastIndexOf("/") + 1);
                                }
                                
                                // 保持与JsonToHtmlConverter中相同的contentId生成逻辑
                                String htmlContentId = htmlImgFileName.replaceAll("[^a-zA-Z0-9.]", "_");
                                if (htmlContentId.contains(".")) {
                                    htmlContentId = htmlContentId.substring(0, htmlContentId.lastIndexOf('.'));
                                }
                                htmlContentId = htmlContentId + "_img";
                                
                                logger.info("处理HTML中的图片: {} -> contentId: {}", htmlImgFileName, htmlContentId);
                                
                                // 使用与常规图片节点相同的逻辑处理图片文件
                                processImageFile(htmlImgFileName, htmlImageUrl, htmlContentId, helper, imageResourcePath);
                            }
                        }
                    }
                }
            }
            
            // 处理所有字段
            node.fields().forEachRemaining(entry -> {
                try {
                    processImagesInJsonNode(entry.getValue(), helper, imageResourcePath);
                } catch (Exception e) {
                    logger.error("处理图片字段时出错: {}", e.getMessage(), e);
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

    // 提取图片处理逻辑到单独的方法，以便重用
    private void processImageFile(String imgFileName, String imageUrl, String contentId, 
                                MimeMessageHelper helper, String imageResourcePath) {
        try {
            // 尝试从多个位置加载图片
            File imageFile = null;
            
            // 1. 检查是否为绝对路径
            File absoluteFile = new File(imageUrl);
            if (absoluteFile.exists() && absoluteFile.isFile()) {
                imageFile = absoluteFile;
                logger.info("从绝对路径找到图片: {}", absoluteFile.getAbsolutePath());
            }
            
            // 2. 检查图片是否在 images 目录下
            if (imageFile == null) {
                File imageResourceFile = new File(imageResourcePath, imgFileName);
                if (imageResourceFile.exists() && imageResourceFile.isFile()) {
                    imageFile = imageResourceFile;
                    logger.info("在图片资源目录找到图片: {}", imageResourceFile.getAbsolutePath());
                }
            }
            
            // 3. 如果路径包含 images/ 前缀，检查相对于附件根目录的路径
            if (imageFile == null && imageUrl.startsWith("images/")) {
                File attachmentPathFile = new File(attachmentPath, imageUrl);
                if (attachmentPathFile.exists() && attachmentPathFile.isFile()) {
                    imageFile = attachmentPathFile;
                    logger.info("在附件根目录下找到图片: {}", attachmentPathFile.getAbsolutePath());
                }
            }
            
            // 4. 额外尝试附件根目录下的图片目录
            if (imageFile == null) {
                File imagesDir = new File(attachmentPath, "images");
                File imageInImagesDir = new File(imagesDir, imgFileName);
                if (imageInImagesDir.exists() && imageInImagesDir.isFile()) {
                    imageFile = imageInImagesDir;
                    logger.info("在附件根目录的images目录下找到图片: {}", imageInImagesDir.getAbsolutePath());
                }
            }
            
            if (imageFile != null) {
                FileSystemResource resource = new FileSystemResource(imageFile);
                
                // 尝试确定图片的MIME类型
                String mimeType = determineMimeType(imageFile.getName());
                logger.info("图片 {} 的MIME类型: {}", imageFile.getName(), mimeType);
                
                // 添加内联图片附件，设置Content-ID - 使用不带尖括号的ContentId，保持与HTML中相同
                helper.addInline(contentId, resource, mimeType);
                
                logger.info("成功添加内嵌图片: 文件路径={}, contentId={}, 文件大小={}KB, 文件类型={}", 
                    imageFile.getAbsolutePath(), contentId, imageFile.length()/1024, 
                    getFileExtension(imageFile.getName()));
            } else {
                logger.error("无法找到图片文件: {}. 尝试查找的位置: 绝对路径, {}, {}", 
                    imageUrl, imageResourcePath, attachmentPath + "/images");
                
                // 列出图片目录内容以帮助调试
                File imagesDir = new File(attachmentPath, "images");
                if (imagesDir.exists() && imagesDir.isDirectory()) {
                    File[] files = imagesDir.listFiles();
                    if (files != null && files.length > 0) {
                        logger.info("图片目录包含以下文件:");
                        for (File f : files) {
                            logger.info(" - {} ({}KB)", f.getName(), f.length()/1024);
                        }
                    } else {
                        logger.info("图片目录为空或无法列出文件");
                    }
                } else {
                    logger.info("图片目录不存在: {}", imagesDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            logger.error("添加内嵌图片时出错: {}", e.getMessage(), e);
        }
    }
}
