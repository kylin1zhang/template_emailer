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
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);

            // 创建极简的HTML结构
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>");
            htmlBuilder.append("<html>");
            htmlBuilder.append("<head>");
            htmlBuilder.append("<meta charset=\"UTF-8\">");
            htmlBuilder.append("<title>Email Template</title>");
            
            // 只保留绝对必要的样式
            htmlBuilder.append("<style>");
            htmlBuilder.append("body{margin:0;padding:0;font-family:Arial,sans-serif;}");
            htmlBuilder.append("table{border-collapse:collapse;width:100%;}");
            htmlBuilder.append("img{max-width:100%;height:auto;}");
            // 添加表格样式，保持边框和内边距的一致性
            htmlBuilder.append("table.content-table{border:1px solid #ddd;width:100%;margin-bottom:15px;}");
            htmlBuilder.append("table.content-table th{background-color:#f8f9fa;font-weight:bold;text-align:left;padding:8px;border:1px solid #ddd;}");
            htmlBuilder.append("table.content-table td{padding:8px;border:1px solid #ddd;}");
            // 添加表格行的悬停效果
            htmlBuilder.append("table.content-table tr:hover{background-color:#f5f5f5;}");
            htmlBuilder.append("</style>");
            
            htmlBuilder.append("</head>");
            htmlBuilder.append("<body>");

            // 生成内容主表格
            htmlBuilder.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">");

            // 检查是否有原始HTML内容节点被错误处理
            JsonNode origHtmlNode = findHtmlContentNode(rootNode);
            if (origHtmlNode != null) {
                System.out.println("警告: 发现原始HTML内容节点，可能会覆盖其他内容");
                String htmlValue = origHtmlNode.asText();
                logHtmlContentSample("HTML内容样本", htmlValue);
            }

            // 解析body节点
            JsonNode bodyNode = rootNode.get("body");
            if (bodyNode != null) {
                JsonNode rows = bodyNode.get("rows");
                if (rows != null && rows.isArray()) {
                    System.out.println("解析到模板行数: " + rows.size());
                    for (JsonNode row : rows) {
                        // 每行作为一个表格行
                        htmlBuilder.append("<tr><td>");
                        
                        // 此行的内容表格
                        htmlBuilder.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\"><tr>");

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
                                
                                // 列作为表格单元格
                                htmlBuilder.append("<td width=\"").append(widthPercent).append("%\" valign=\"top\" style=\"padding:10px\">");

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
                                            htmlBuilder.append("<p style=\"margin:0 0 10px 0;text-align:").append(alignment).append(";\">")
                                                    .append(text).append("</p>");
                                        } else if ("heading".equals(type)) {
                                            // 标题 - 确保标题独立一行，前后有清除浮动
                                            htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                            htmlBuilder.append("<h1 style=\"margin:0 0 10px 0;font-size:24px;text-align:")
                                                    .append(alignment).append(";\">").append(text).append("</h1>");
                                            htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                        } else if ("image".equals(type)) {
                                            // 图片处理
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
                                                    
                                                    // 简单地生成内联图片ID
                                                    String contentId = imgFileName.replaceAll("[^a-zA-Z0-9.]", "_");
                                                    if (contentId.contains(".")) {
                                                        contentId = contentId.substring(0, contentId.lastIndexOf('.'));
                                                    }
                                                    contentId = contentId + "_img";
                                                    
                                                    imageUrl = "cid:" + contentId;
                                                }
                                                
                                                String altText = text.isEmpty() ? "Image" : text;
                                                
                                                // 为图片添加清除浮动，确保它在自己的行中显示
                                                htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                                
                                                // 为图片创建一个自包含的div，避免与其他元素混合
                                                htmlBuilder.append("<div style=\"display:block; width:100%; margin:0 0 10px 0;\">");
                                                
                                                if ("left".equals(alignment)) {
                                                    // 左对齐
                                                    htmlBuilder.append("<div style=\"text-align:left;\">");
                                                    htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                        .append(altText).append("\" style=\"display:block; border:0; max-width:100%;\" />");
                                                    htmlBuilder.append("</div>");
                                                } else if ("right".equals(alignment)) {
                                                    // 右对齐
                                                    htmlBuilder.append("<div style=\"text-align:right;\">");
                                                    htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                        .append(altText).append("\" style=\"display:block; border:0; max-width:100%;\" />");
                                                    htmlBuilder.append("</div>");
                                                } else {
                                                    // 居中对齐
                                                    htmlBuilder.append("<div style=\"text-align:center;\">");
                                                    htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                        .append(altText).append("\" style=\"display:block; border:0; max-width:100%; margin:0 auto;\" />");
                                                    htmlBuilder.append("</div>");
                                                }
                                                
                                                htmlBuilder.append("</div>");
                                                htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                            }
                                        } else if ("html".equals(type)) {
                                            // 直接HTML内容
                                            String htmlContent = values.path("html").asText("");
                                            if (!htmlContent.isEmpty()) {
                                                // 添加清除浮动确保HTML内容在自己的行中
                                                htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                                
                                                // 检查HTML内容是否包含表格
                                                boolean containsTable = htmlContent.contains("<table") || htmlContent.contains("<TABLE");
                                                
                                                if (containsTable) {
                                                    // 如果包含表格，我们需要特殊处理以确保表格样式被保留
                                                    // 将任何没有class的表格添加content-table类
                                                    htmlContent = htmlContent.replaceAll("<table(?![^>]*class=)", "<table class=\"content-table\"");
                                                    
                                                    // 尝试保留表格的原始对齐方式
                                                    htmlBuilder.append("<div style=\"width:100%;");
                                                    if ("center".equals(alignment)) {
                                                        htmlBuilder.append("text-align:center;");
                                                    } else if ("right".equals(alignment)) {
                                                        htmlBuilder.append("text-align:right;");
                                                    } else {
                                                        htmlBuilder.append("text-align:left;");
                                                    }
                                                    htmlBuilder.append("\">");
                                                    
                                                    // 直接插入表格内容
                                                    htmlBuilder.append(htmlContent);
                                                    
                                                    htmlBuilder.append("</div>");
                                                } else {
                                                    // 对于非表格的HTML内容
                                                    htmlBuilder.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr><td align=\"")
                                                            .append(alignment).append("\" style=\"text-align:").append(alignment).append(";\">");
                                                    
                                                    // 直接插入HTML内容
                                                    htmlBuilder.append(htmlContent);
                                                    
                                                    htmlBuilder.append("</td></tr></table>");
                                                }
                                                
                                                htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                            }
                                        } else if ("button".equals(type)) {
                                            // 添加清除浮动
                                            htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                            
                                            // 简化按钮，但使用更可靠的表格布局
                                            if ("left".equals(alignment)) {
                                                htmlBuilder.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td align=\"left\">");
                                            } else if ("right".equals(alignment)) {
                                                htmlBuilder.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td align=\"right\">");
                                            } else {
                                                htmlBuilder.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td align=\"center\">");
                                            }
                                            
                                            // 按钮本身
                                            htmlBuilder.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td style=\"padding:10px 15px;background-color:#337ab7;border-radius:4px;\">");
                                            htmlBuilder.append("<a href=\"#\" style=\"color:#ffffff;text-decoration:none;display:block;\">")
                                                    .append(text).append("</a>");
                                            htmlBuilder.append("</td></tr></table>");
                                            
                                            htmlBuilder.append("</td></tr></table>");
                                            htmlBuilder.append("<div style=\"clear:both;\"></div>");
                                        }
                                    }
                                }

                                htmlBuilder.append("</td>"); // 关闭列单元格
                            }
                        }

                        htmlBuilder.append("</tr></table>"); // 关闭行表格
                        htmlBuilder.append("</td></tr>"); // 关闭行容器
                    }
                }
            } else {
                System.out.println("警告: 找不到body节点，无法解析内容结构");
                // 没有正确的body结构时添加默认内容
                htmlBuilder.append("<tr><td>").append(json).append("</td></tr>");
            }

            htmlBuilder.append("</table>"); // 关闭主容器表格
            htmlBuilder.append("</body></html>");
            String result = htmlBuilder.toString();
            System.out.println("JSON转HTML完成, 生成HTML长度: " + result.length());
            return result;
        } catch (Exception e) {
            // 记录错误并返回简单HTML
            System.err.println("Error converting JSON to HTML: " + e.getMessage());
            e.printStackTrace();
            return "<html><body><p>Error parsing template content: " + e.getMessage() + "</p></body></html>";
        }
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
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);
            
            // 查找HTML内容节点
            JsonNode htmlNode = findHtmlContentNode(rootNode);
            if (htmlNode != null) {
                String htmlContent = htmlNode.asText();
                System.out.println("成功提取HTML内容，长度: " + htmlContent.length());
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
