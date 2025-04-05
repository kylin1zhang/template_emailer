package com.citi.custody.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JsonToHtmlConverter {

    /**
     * Converts JSON data to an HTML string.
     * @param json The JSON string describing the HTML structure.
     * @return The generated HTML string.
     * @throws IOException If JSON parsing fails.
     */
    public static String convertJsonToHtml(String json) throws IOException {
        // 检查输入JSON是否为空
        if (json == null || json.trim().isEmpty()) {
            System.out.println("警告: 输入的JSON为空，返回默认HTML");
            return "<html><body><p>No content available</p></body></html>";
        }

        try {
            System.out.println("开始解析模板JSON: 长度=" + json.length());
            
            // 尝试格式化和修复JSON字符串，以便更可靠地解析
            json = preProcessJson(json);
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);

            // 记录JSON结构
            System.out.println("解析的JSON根节点类型: " + rootNode.getNodeType());
            if (rootNode.isObject()) {
                Iterator<String> fieldNames = rootNode.fieldNames();
                StringBuilder fieldList = new StringBuilder("[");
                boolean first = true;
                while (fieldNames.hasNext()) {
                    if (!first) fieldList.append(", ");
                    fieldList.append(fieldNames.next());
                    first = false;
                }
                fieldList.append("]");
                System.out.println("根节点字段: " + fieldList.toString());
            }

            // 创建极简的HTML结构
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>");
            htmlBuilder.append("<html>");
            htmlBuilder.append("<head>");
            htmlBuilder.append("<meta charset=\"UTF-8\">");
            htmlBuilder.append("<title>Email Template</title>");
            
            // 增强样式定义，确保各种元素的对齐方式生效
            htmlBuilder.append("<style>");
            htmlBuilder.append("body{margin:0;padding:0;font-family:Arial,sans-serif;}");
            htmlBuilder.append("table{border-collapse:collapse;width:100%;}");
            htmlBuilder.append("img{max-width:100%;height:auto;}");
            // 增加对齐样式的定义
            htmlBuilder.append(".align-left{text-align:left;}");
            htmlBuilder.append(".align-center{text-align:center;}");
            htmlBuilder.append(".align-right{text-align:right;}");
            // 强化图片对齐样式
            htmlBuilder.append(".img-left{float:left;margin-right:15px;margin-bottom:10px;}");
            htmlBuilder.append(".img-center{display:block;margin-left:auto;margin-right:auto;margin-bottom:10px;}");
            htmlBuilder.append(".img-right{float:right;margin-left:15px;margin-bottom:10px;}");
            // 表格样式，确保边框和内边距的一致性
            htmlBuilder.append("table.content-table{border:1px solid #ddd;width:100%;margin-bottom:15px;}");
            htmlBuilder.append("table.content-table th{background-color:#f8f9fa;font-weight:bold;padding:8px;border:1px solid #ddd;}");
            htmlBuilder.append("table.content-table td{padding:8px;border:1px solid #ddd;}");
            // 标题样式
            htmlBuilder.append("h1,h2,h3,h4,h5,h6{margin-top:10px;margin-bottom:10px;}");
            htmlBuilder.append("</style>");
            
            htmlBuilder.append("</head>");
            htmlBuilder.append("<body>");

            // 先检查是否有原始HTML内容
            JsonNode htmlNode = findHtmlContentNode(rootNode);
            if (htmlNode != null && !htmlNode.asText().trim().isEmpty()) {
                System.out.println("找到原始HTML内容，长度: " + htmlNode.asText().length());
                String htmlContent = htmlNode.asText();
                
                // 检查原始HTML是否包含完整的HTML结构
                boolean hasHtmlTag = htmlContent.toLowerCase().contains("<html") && htmlContent.toLowerCase().contains("</html>");
                boolean hasBodyTag = htmlContent.toLowerCase().contains("<body") && htmlContent.toLowerCase().contains("</body>");
                
                if (hasHtmlTag && hasBodyTag) {
                    // 完整的HTML文档，直接返回
                    System.out.println("使用完整的原始HTML内容");
                    return htmlContent;
                } else {
                    // 包含HTML片段但不完整，需要嵌入到结构中
                    System.out.println("使用HTML片段内容");
                    htmlBuilder.append("<div class=\"raw-html-content\">");
                    htmlBuilder.append(htmlContent);
                    htmlBuilder.append("</div>");
                    htmlBuilder.append("</body></html>");
                    return htmlBuilder.toString();
                }
            }
            
            // 生成内容主表格 - 使用div结构更容易控制对齐
            htmlBuilder.append("<div class=\"main-container\">");

            // 解析body节点
            JsonNode bodyNode = rootNode.get("body");
            if (bodyNode != null) {
                JsonNode rows = bodyNode.get("rows");
                if (rows != null && rows.isArray()) {
                    System.out.println("解析到模板行数: " + rows.size());
                    for (JsonNode row : rows) {
                        // 每行作为一个div区域
                        htmlBuilder.append("<div class=\"row\" style=\"clear:both;width:100%;margin-bottom:15px;\">");
                        
                        JsonNode columns = row.get("columns");
                        if (columns != null && columns.isArray()) {
                            int totalColumns = columns.size();
                            
                            // 计算列宽
                            int[] widths = new int[totalColumns];
                            int totalWidth = 0;
                            
                            for (int i = 0; i < totalColumns; i++) {
                                JsonNode column = columns.get(i);
                                if (column.has("ratio") && column.get("ratio").isInt()) {
                                    widths[i] = column.get("ratio").asInt();
                                } else {
                                    widths[i] = 1;
                                }
                                totalWidth += widths[i];
                            }
                            
                            for (int i = 0; i < totalColumns; i++) {
                                JsonNode column = columns.get(i);
                                
                                // 计算百分比宽度
                                int widthPercent = (widths[i] * 100) / totalWidth;
                                
                                // 列作为div
                                htmlBuilder.append("<div class=\"column\" style=\"float:left;width:")
                                         .append(widthPercent).append("%;padding:10px;box-sizing:border-box;\">");

                                JsonNode contents = column.get("contents");
                                if (contents != null && contents.isArray()) {
                                    for (JsonNode content : contents) {
                                        String type = content.path("type").asText("");
                                        
                                        // 获取内容值
                                        JsonNode values = content.path("values");
                                        String text = values.path("text").asText("");
                                        String alignment = values.path("align").asText("left");
                                        
                                        if ("text".equals(type)) {
                                            // 文本段落
                                            htmlBuilder.append("<p class=\"align-").append(alignment)
                                                    .append("\" style=\"margin:0 0 10px 0;text-align:").append(alignment).append(";\">")
                                                    .append(text).append("</p>");
                                        } else if ("heading".equals(type)) {
                                            // 标题 - 使用div包装确保对齐方式生效
                                            int headingLevel = values.path("level").asInt(1);
                                            String tag = "h" + (headingLevel > 0 && headingLevel <= 6 ? headingLevel : 1);
                                            
                                            htmlBuilder.append("<div class=\"heading-container align-").append(alignment).append("\" style=\"text-align:")
                                                    .append(alignment).append(";\">");
                                            htmlBuilder.append("<").append(tag).append(" style=\"margin:10px 0;font-weight:bold;\">")
                                                    .append(text).append("</").append(tag).append(">");
                                            htmlBuilder.append("</div>");
                                        } else if ("image".equals(type)) {
                                            // 图片处理，确保对齐方式生效
                                            String imageUrl = "";
                                            if (values.has("src")) {
                                                if (values.path("src").isObject()) {
                                                    imageUrl = values.path("src").path("url").asText();
                                                } else {
                                                    imageUrl = values.path("src").asText();
                                                }
                                            } else if (values.has("url")) {
                                                imageUrl = values.path("url").asText();
                                            }
                                            
                                            if (!imageUrl.isEmpty()) {
                                                // 处理本地图片引用
                                                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("data:")) {
                                                    String imgFileName = imageUrl;
                                                    if (imageUrl.contains("/")) {
                                                        imgFileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                                                    }
                                                    
                                                    // 生成内联图片ID
                                                    String contentId = imgFileName.replaceAll("[^a-zA-Z0-9.]", "_");
                                                    if (contentId.contains(".")) {
                                                        contentId = contentId.substring(0, contentId.lastIndexOf('.'));
                                                    }
                                                    contentId = contentId + "_img";
                                                    
                                                    imageUrl = "cid:" + contentId;
                                                }
                                                
                                                String altText = text.isEmpty() ? "Image" : text;
                                                
                                                // 更可靠的图片对齐方法
                                                String imgClass = "img-" + alignment;
                                                String imgStyle = "";
                                                
                                                if ("left".equals(alignment)) {
                                                    imgStyle = "float:left;margin-right:15px;margin-bottom:10px;";
                                                } else if ("right".equals(alignment)) {
                                                    imgStyle = "float:right;margin-left:15px;margin-bottom:10px;";
                                                } else { // center
                                                    imgStyle = "display:block;margin:0 auto 10px auto;";
                                                }
                                                
                                                htmlBuilder.append("<div style=\"width:100%;overflow:hidden;margin-bottom:10px;\">");
                                                htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                    .append(altText).append("\" class=\"").append(imgClass).append("\" ")
                                                    .append("style=\"").append(imgStyle).append("max-width:100%;height:auto;\" />");
                                                htmlBuilder.append("</div>");
                                            }
                                        } else if ("html".equals(type)) {
                                            // 直接HTML内容
                                            String htmlContent = values.path("html").asText("");
                                            if (!htmlContent.isEmpty()) {
                                                // 检查HTML内容是否包含表格
                                                boolean containsTable = htmlContent.contains("<table") || htmlContent.contains("<TABLE");
                                                
                                                htmlBuilder.append("<div class=\"html-content align-").append(alignment)
                                                        .append("\" style=\"width:100%;text-align:").append(alignment).append(";\">");
                                                
                                                if (containsTable) {
                                                    // 处理表格内容，确保表格class和对齐方式生效
                                                    htmlContent = htmlContent.replaceAll("<table(?![^>]*class=)", "<table class=\"content-table\"");
                                                    
                                                    // 为表格添加对齐样式
                                                    if ("center".equals(alignment)) {
                                                        htmlContent = htmlContent.replaceAll("<table([^>]*)>", "<table$1 style=\"margin:0 auto;\">");
                                                    } else if ("right".equals(alignment)) {
                                                        htmlContent = htmlContent.replaceAll("<table([^>]*)>", "<table$1 style=\"margin-left:auto;\">");
                                                    }
                                                    
                                                    // 直接插入表格内容
                                                    htmlBuilder.append(htmlContent);
                                                } else {
                                                    // 直接插入HTML内容
                                                    htmlBuilder.append(htmlContent);
                                                }
                                                
                                                htmlBuilder.append("</div>");
                                            }
                                        } else if ("button".equals(type)) {
                                            // 按钮元素
                                            String buttonUrl = values.path("url").asText("#");
                                            String buttonColor = values.path("color").asText("#337ab7");
                                            String textColor = values.path("textColor").asText("#ffffff");
                                            
                                            String buttonClass = "align-" + alignment;
                                            String buttonContainerStyle = "text-align:" + alignment + ";margin:10px 0;";
                                            
                                            htmlBuilder.append("<div class=\"").append(buttonClass).append("\" style=\"")
                                                    .append(buttonContainerStyle).append("\">");
                                            
                                            // 按钮样式
                                            htmlBuilder.append("<a href=\"").append(buttonUrl).append("\" ")
                                                    .append("style=\"display:inline-block;padding:10px 20px;background-color:")
                                                    .append(buttonColor).append(";color:").append(textColor)
                                                    .append(";text-decoration:none;border-radius:4px;font-weight:bold;\">")
                                                    .append(text).append("</a>");
                                            
                                            htmlBuilder.append("</div>");
                                        }
                                    }
                                }

                                htmlBuilder.append("</div>"); // 关闭列div
                            }
                        }

                        htmlBuilder.append("<div style=\"clear:both;\"></div>"); // 清除浮动
                        htmlBuilder.append("</div>"); // 关闭行div
                    }
                }
            } else {
                System.out.println("警告: 找不到body节点，无法解析内容结构");
                
                // 没有正确的body结构，尝试直接格式化JSON为可读形式
                try {
                    ObjectMapper prettyMapper = new ObjectMapper();
                    Object jsonObject = prettyMapper.readValue(json, Object.class);
                    String prettyJson = prettyMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                    
                    htmlBuilder.append("<div style=\"font-family:monospace;white-space:pre;\">");
                    htmlBuilder.append(escapeHtml(prettyJson));
                    htmlBuilder.append("</div>");
                } catch (Exception e) {
                    // 格式化失败，直接显示原始JSON
                    htmlBuilder.append("<div>").append(escapeHtml(json)).append("</div>");
                }
            }

            htmlBuilder.append("</div>"); // 关闭主容器
            htmlBuilder.append("</body></html>");
            
            String result = htmlBuilder.toString();
            System.out.println("JSON转HTML完成, 生成HTML长度: " + result.length());
            return result;
        } catch (Exception e) {
            // 记录错误并返回简单HTML
            System.err.println("Error converting JSON to HTML: " + e.getMessage());
            e.printStackTrace();
            
            // 返回更详细的错误信息
            return "<html><body><h2>Error parsing template content</h2><p>" + e.getMessage() + "</p></body></html>";
        }
    }

    /**
     * 预处理JSON字符串，修复可能的格式问题
     */
    private static String preProcessJson(String json) {
        if (json == null) return null;
        
        try {
            // 尝试解析和重新格式化，这可以修复某些格式问题
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            return json; // 如果解析成功，返回原始JSON
        } catch (Exception e) {
            System.out.println("JSON格式有问题，尝试修复: " + e.getMessage());
            
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
        }
    }
    
    /**
     * 将迭代器转换为字符串，用于调试
     */
    private static String iterableToString(Iterator<String> iterator) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        while (iterator.hasNext()) {
            if (!first) sb.append(", ");
            sb.append(iterator.next());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 转义HTML特殊字符
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    /**
     * Saves the generated HTML string to a file.
     * @param html The HTML string.
     * @param filePath The file path to save the HTML.
     * @throws IOException If file writing fails.
     */
    public static void saveHtmlToFile(String html, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(html);
            System.out.println("HTML file saved to: " + filePath);
        }
    }

    public static void main(String[] args) {
        // Example JSON input
        String json = "..."; // Replace with your JSON string

        try {
            // Convert JSON to HTML
            String html = convertJsonToHtml(json);

            // Save HTML to a file
            saveHtmlToFile(html, "output.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 添加新的调试方法
    private static void logHtmlContentSample(String message, String content) {
        String sample = content.length() > 100 ? content.substring(0, 100) + "..." : content;
        System.out.println(message + ": " + sample);
    }

    // 用于在JSON中查找HTML内容节点的辅助方法
    private static JsonNode findHtmlContentNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        
        if (node.isObject()) {
            // 检查当前节点是否为HTML类型
            if (node.has("type") && "html".equals(node.path("type").asText())) {
                if (node.has("values") && node.path("values").has("html")) {
                    return node.path("values").path("html");
                }
            }
            
            // 递归检查所有字段
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                JsonNode result = findHtmlContentNode(fields.next().getValue());
                if (result != null) {
                    return result;
                }
            }
        } else if (node.isArray()) {
            // 递归检查数组中的所有元素
            for (JsonNode element : node) {
                JsonNode result = findHtmlContentNode(element);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }

    /**
     * 提取JSON中的原始HTML内容，不添加额外的结构
     * @param json The JSON string containing HTML content
     * @return 提取的HTML内容，如果没有找到则返回null
     */
    public static String extractRawHtmlContent(String json) {
        try {
            System.out.println("尝试从JSON中提取原始HTML内容");
            
            // 预处理JSON修复可能的格式问题
            json = preProcessJson(json);
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);
            
            // 查找HTML内容节点
            JsonNode htmlNode = findHtmlContentNode(rootNode);
            if (htmlNode != null) {
                String htmlContent = htmlNode.asText();
                System.out.println("成功提取HTML内容，长度: " + htmlContent.length());
                
                // 检查提取的HTML内容
                if (htmlContent.trim().isEmpty()) {
                    System.out.println("警告: 提取的HTML内容为空");
                    return null;
                }
                
                // 确保HTML内容有基本结构
                boolean hasHtmlTag = htmlContent.toLowerCase().contains("<html");
                boolean hasBodyTag = htmlContent.toLowerCase().contains("<body");
                
                if (!hasHtmlTag || !hasBodyTag) {
                    System.out.println("提取的HTML内容缺少完整结构，添加基本HTML标签");
                    htmlContent = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>" + htmlContent + "</body></html>";
                }
                
                return htmlContent;
            }
            
            System.out.println("在JSON中未找到HTML内容节点");
            return null;
        } catch (Exception e) {
            System.err.println("从JSON提取HTML内容时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断JSON中是否包含HTML内容节点
     * @param json The JSON string to check
     * @return 是否包含HTML内容
     */
    public static boolean containsHtmlContent(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode htmlNode = findHtmlContentNode(rootNode);
            return htmlNode != null;
        } catch (Exception e) {
            System.err.println("检查JSON中的HTML内容时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 重载convertJsonToHtml方法，添加提取原始内容的选项
     */
    public static String convertJsonToHtml(String json, boolean extractRawHtmlOnly) throws IOException {
        if (extractRawHtmlOnly) {
            String rawHtml = extractRawHtmlContent(json);
            if (rawHtml != null) {
                return rawHtml;
            }
            // 如果没有找到原始HTML，继续使用标准转换
            System.out.println("未找到原始HTML内容，将使用标准转换");
        }
        
        return convertJsonToHtml(json);
    }
}
