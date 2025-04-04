package com.citi.custody.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;

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
            return "<html><body><p>No content available</p></body></html>";
        }

        try {
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
            htmlBuilder.append("</style>");
            
            htmlBuilder.append("</head>");
            htmlBuilder.append("<body>");

            // 生成内容主表格
            htmlBuilder.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">");

            // 解析body节点
            JsonNode bodyNode = rootNode.get("body");
            if (bodyNode != null) {
                JsonNode rows = bodyNode.get("rows");
                if (rows != null && rows.isArray()) {
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
                                                htmlBuilder.append("<div style=\"text-align:").append(alignment).append(";\">")
                                                        .append(htmlContent).append("</div>");
                                            }
                                        } else if ("button".equals(type)) {
                                            // 简化按钮
                                            String alignStyle = "left".equals(alignment) ? "" : 
                                                              ("center".equals(alignment) ? "margin:0 auto;" : "margin-left:auto;");
                                            
                                            htmlBuilder.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:10px 0;")
                                                    .append(alignStyle).append("\"><tr><td style=\"padding:10px 15px;background-color:#337ab7;border-radius:4px;\">")
                                                    .append("<a href=\"#\" style=\"color:#ffffff;text-decoration:none;display:block;\">")
                                                    .append(text).append("</a></td></tr></table>");
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
                // 没有正确的body结构时添加默认内容
                htmlBuilder.append("<tr><td>").append(json).append("</td></tr>");
            }

            htmlBuilder.append("</table>"); // 关闭主容器表格
            htmlBuilder.append("</body></html>");
            return htmlBuilder.toString();
        } catch (Exception e) {
            // 记录错误并返回简单HTML
            System.err.println("Error converting JSON to HTML: " + e.getMessage());
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
}
