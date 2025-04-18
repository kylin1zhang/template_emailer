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
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;

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
                    // 添加日志记录原始 JSON
                    logger.info("原始模板 JSON: {}", template.getContent());
                    
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
            if (template != null) {
                try {
                    if (template.getContent() != null) {
                        // 添加日志记录原始 JSON
                        logger.info("原始模板 JSON: {}", template.getContent());
                        
                        // 解析 GrapesJS 生成的 JSON 格式
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(template.getContent());
                        
                        // 从 JSON 中提取 HTML 和 CSS
                        String html = "";
                        String css = "";
                        
                        if (rootNode.has("html")) {
                            html = rootNode.get("html").asText();
                        }
                        
                        if (rootNode.has("css")) {
                            css = rootNode.get("css").asText();
                        }
                        
                        StringBuilder sb = new StringBuilder();
                        sb.append("<!DOCTYPE html>");
                        sb.append("<html>");
                        sb.append("<head>");
                        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
                        
                        // 添加来自JSON的CSS样式和基本样式
                        sb.append("<style>");
                        sb.append(css);
                        sb.append("</style>");
                        
                        sb.append("</head>");
                        sb.append("<body>");
                        
                        // 直接使用原始HTML内容
                        sb.append(html);
                        
                        sb.append("</body></html>");
                        
                        content = sb.toString();
                        
                        // 移除开头的格式标记
                        content = content.replaceAll("B̲|I̲|U̲|S̲|/|_|x", "");
                        
                        // 确保内容中对表格、图片和文本的对齐方式正确应用
                        content = ensureAlignmentStyles(content);
                    } else {
                        logger.warn("Template {} has null content, using default content for email: {}", 
                            template.getId(), email.getId());
                    }
                } catch (Exception e) {
                    logger.error("Error converting template content to HTML for email {}: {}", 
                        email.getId(), e.getMessage(), e);
                    content = "<html><body><p>Error parsing template: " + e.getMessage() + "</p></body></html>";
                }
            }
            
            // 设置邮件内容
            logger.info("正在设置邮件内容: 长度={}, 类型=HTML", content.length());
            analyzeHtmlContent(content, "最终设置内容");

            // 简化设置，只使用一次setText方法，确保内容不被替换
            helper.setText(content, true);
            logger.info("邮件内容设置完成，最终内容长度: {}", content.length());

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
                
                // 添加内联图片 - 使用FileSystemResource包装File对象
                FileSystemResource resource = new FileSystemResource(imageFile);
                helper.addInline(contentId, resource, mimeType);
                
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

    /**
     * 添加专门用于记录和诊断HTML内容的方法
     */
    private void analyzeHtmlContent(String content, String stage) {
        if (content == null || content.isEmpty()) {
            logger.warn("HTML内容分析 [{}]: 内容为空", stage);
            return;
        }

        logger.info("HTML内容分析 [{}]: 内容长度={}", stage, content.length());
        
        // 检查是否包含完整的HTML结构
        boolean hasHtmlTag = content.contains("<html") && content.contains("</html>");
        boolean hasBodyTag = content.contains("<body") && content.contains("</body>");
        boolean hasHeadTag = content.contains("<head") && content.contains("</head>");
        boolean hasStyleTag = content.contains("<style") && content.contains("</style>");
        
        logger.info("HTML结构分析 [{}]: HTML标签={}, HEAD标签={}, BODY标签={}, STYLE标签={}", 
            stage, hasHtmlTag, hasHeadTag, hasBodyTag, hasStyleTag);
        
        // 检查表格结构
        int tableCount = countOccurrences(content, "<table");
        int tableCloseCount = countOccurrences(content, "</table>");
        int tableRowCount = countOccurrences(content, "<tr");
        int tableCellCount = countOccurrences(content, "<td");
        
        logger.info("表格结构分析 [{}]: 表格数量={}, 表格行数量={}, 表格单元格数量={}", 
            stage, tableCount, tableRowCount, tableCellCount);
        
        if (tableCount != tableCloseCount) {
            logger.warn("表格结构不匹配 [{}]: 开始标签={}, 结束标签={}", stage, tableCount, tableCloseCount);
        }
        
        // 检查图片标签
        int imgCount = countOccurrences(content, "<img");
        logger.info("图片分析 [{}]: 图片标签数量={}", stage, imgCount);
        
        // 检查是否有自定义HTML内容
        if (content.contains("type=\"html\"") || content.contains("type=\\\"html\\\"")) {
            logger.info("发现自定义HTML内容类型 [{}]", stage);
        }
        
        // 检查内容中是否有MIME分隔符或部分标记
        if (content.contains("part_") || content.contains("boundary=")) {
            logger.warn("内容中可能包含MIME分隔符 [{}]", stage);
        }
        
        // 记录内容预览
        String contentPreview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        logger.debug("内容预览 [{}]: {}", stage, contentPreview);
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * 验证字符串是否为有效的JSON格式
     */
    private boolean isValidJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            logger.warn("JSON格式验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 尝试修复JSON格式问题
     */
    private String fixJsonFormat(String json) {
        if (json == null) return null;
        
        try {
            // 替换可能的错误格式
            json = json.replaceAll("\\\\\"", "\"")  // 修复转义的引号
                     .replaceAll("\\n", " ")      // 删除换行符
                     .replaceAll("\\r", " ");     // 删除回车符
                     
            // 检查括号平衡
            int curlyBraces = 0;
            int squareBrackets = 0;
            
            for (char c : json.toCharArray()) {
                if (c == '{') curlyBraces++;
                else if (c == '}') curlyBraces--;
                else if (c == '[') squareBrackets++;
                else if (c == ']') squareBrackets--;
            }
            
            // 添加缺失的括号
            while (curlyBraces > 0) {
                json += "}";
                curlyBraces--;
            }
            
            while (squareBrackets > 0) {
                json += "]";
                squareBrackets--;
            }
            
            return json;
        } catch (Exception e) {
            logger.error("修复JSON格式时出错: {}", e.getMessage());
            return json; // 返回原始JSON，让后续处理尝试解析
        }
    }
    
    /**
     * 确保对齐方式样式正确应用于HTML内容
     */
    private String ensureAlignmentStyles(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }
        
        try {
            // 确保表格对齐方式正确
            htmlContent = htmlContent.replaceAll("<table([^>]*)align=\"center\"([^>]*)>", 
                    "<table$1align=\"center\"$2 style=\"margin-left:auto;margin-right:auto;\">")
                .replaceAll("<table([^>]*)align=\"right\"([^>]*)>", 
                    "<table$1align=\"right\"$2 style=\"margin-left:auto;margin-right:0;\">")
                .replaceAll("<tr([^>]*)align=\"center\"([^>]*)>", 
                    "<tr$1align=\"center\"$2 style=\"text-align:center;\">")
                .replaceAll("<tr([^>]*)align=\"right\"([^>]*)>", 
                    "<tr$1align=\"right\"$2 style=\"text-align:right;\">");
            
            // 强化图片对齐方式
            htmlContent = htmlContent.replaceAll("<img([^>]*)class=\"img-left\"([^>]*)>", 
                    "<img$1class=\"img-left\"$2 style=\"float:left;margin-right:15px;margin-bottom:10px;\">")
                .replaceAll("<img([^>]*)class=\"img-center\"([^>]*)>", 
                    "<img$1class=\"img-center\"$2 style=\"display:block;margin-left:auto;margin-right:auto;margin-bottom:10px;\">")
                .replaceAll("<img([^>]*)class=\"img-right\"([^>]*)>", 
                    "<img$1class=\"img-right\"$2 style=\"float:right;margin-left:15px;margin-bottom:10px;\">");
                
            return htmlContent;
        } catch (Exception e) {
            logger.error("应用对齐样式时出错: {}", e.getMessage());
            return htmlContent; // 返回原始内容
        }
    }
}
