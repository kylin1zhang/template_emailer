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

            // Start building the HTML with DOCTYPE and namespace declarations for Outlook
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html>");
            htmlBuilder.append("<head>");
            htmlBuilder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
            htmlBuilder.append("<title>Email Template</title>");
            
            // 简化CSS，只保留最基本的样式
            htmlBuilder.append("<style type=\"text/css\">");
            htmlBuilder.append("body { margin: 0; padding: 0; }");
            htmlBuilder.append("img { border: 0; height: auto; outline: none; }");
            htmlBuilder.append("table { border-collapse: collapse; }");
            htmlBuilder.append("</style>");
            
            htmlBuilder.append("</head>");
            htmlBuilder.append("<body>");

            // Parse the "body" node
            JsonNode bodyNode = rootNode.get("body");
            if (bodyNode != null) {
                JsonNode rows = bodyNode.get("rows");
                if (rows != null && rows.isArray()) {
                    // Create main container table
                    htmlBuilder.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
                    
                    for (JsonNode row : rows) {
                        // Row as table row
                        htmlBuilder.append("<tr><td>");
                        
                        // Table for this row
                        htmlBuilder.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");

                        JsonNode columns = row.get("columns");
                        if (columns != null && columns.isArray()) {
                            int totalColumns = columns.size();
                            
                            // Determine column widths
                            int[] ratios = new int[totalColumns];
                            int totalRatio = 0;
                            boolean hasRatios = false;
                            
                            // First check if any columns have ratio specified
                            for (int i = 0; i < totalColumns; i++) {
                                JsonNode column = columns.get(i);
                                if (column.has("ratio") && column.get("ratio").isInt()) {
                                    hasRatios = true;
                                    ratios[i] = column.get("ratio").asInt();
                                    totalRatio += ratios[i];
                                } else {
                                    ratios[i] = 1; // Default ratio
                                    totalRatio += 1;
                                }
                            }
                            
                            for (int i = 0; i < totalColumns; i++) {
                                JsonNode column = columns.get(i);
                                
                                // Calculate width percentage
                                float widthPct = (ratios[i] * 100.0f) / totalRatio;
                                
                                // Column as table cell
                                htmlBuilder.append("<td width=\"").append(widthPct).append("%\" valign=\"top\" style=\"padding:10px\">");

                                JsonNode contents = column.get("contents");
                                if (contents != null && contents.isArray()) {
                                    for (JsonNode content : contents) {
                                        // 安全获取内容类型
                                        String type = "text"; // 默认为文本类型
                                        if (content.has("type") && !content.get("type").isNull()) {
                                            type = content.get("type").asText();
                                        }
                                        
                                        // 安全获取文本值
                                        String text = "";
                                        if (content.has("values") && !content.get("values").isNull()) {
                                            JsonNode values = content.get("values");
                                            if (values.has("text") && !values.get("text").isNull()) {
                                                text = values.get("text").asText();
                                            }
                                        }

                                        if ("text".equals(type)) {
                                            // 获取文本对齐方式
                                            String alignment = "left"; // 默认左对齐
                                            if (content.has("values") && !content.get("values").isNull()) {
                                                JsonNode values = content.get("values");
                                                if (values.has("align") && !values.get("align").isNull()) {
                                                    String alignValue = values.get("align").asText();
                                                    if ("left".equals(alignValue) || "right".equals(alignValue) || 
                                                        "center".equals(alignValue) || "justify".equals(alignValue)) {
                                                        alignment = alignValue;
                                                    }
                                                }
                                            }
                                            htmlBuilder.append("<p style=\"margin:0 0 10px 0; text-align:").append(alignment).append(";\">").append(text).append("</p>");
                                        } else if ("heading".equals(type)) {
                                            // 获取标题对齐方式
                                            String alignment = "left"; // 默认左对齐
                                            if (content.has("values") && !content.get("values").isNull()) {
                                                JsonNode values = content.get("values");
                                                if (values.has("align") && !values.get("align").isNull()) {
                                                    String alignValue = values.get("align").asText();
                                                    if ("left".equals(alignValue) || "right".equals(alignValue) || 
                                                        "center".equals(alignValue) || "justify".equals(alignValue)) {
                                                        alignment = alignValue;
                                                    }
                                                }
                                            }
                                            htmlBuilder.append("<h1 style=\"margin:0 0 10px 0; font-size:24px; text-align:").append(alignment).append(";\">").append(text).append("</h1>");
                                        } else if ("button".equals(type)) {
                                            // 获取按钮对齐方式
                                            String alignment = "left"; // 默认左对齐
                                            if (content.has("values") && !content.get("values").isNull()) {
                                                JsonNode values = content.get("values");
                                                if (values.has("align") && !values.get("align").isNull()) {
                                                    String alignValue = values.get("align").asText();
                                                    if ("left".equals(alignValue) || "right".equals(alignValue) || 
                                                        "center".equals(alignValue) || "justify".equals(alignValue)) {
                                                        alignment = alignValue;
                                                    }
                                                }
                                            }
                                            
                                            // 按钮对齐方式通过包装的表格实现
                                            String alignStyle = "";
                                            if ("center".equals(alignment)) {
                                                alignStyle = "margin:0 auto;";
                                            } else if ("right".equals(alignment)) {
                                                alignStyle = "margin-left:auto;";
                                            }
                                            
                                            // Button as a styled link for better compatibility
                                            htmlBuilder.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:10px 0; " + alignStyle + "\"><tr><td align=\"center\" bgcolor=\"#337ab7\" style=\"padding:10px 15px;border-radius:4px;\">");
                                            htmlBuilder.append("<a href=\"#\" target=\"_blank\" style=\"color:#ffffff;text-decoration:none;display:block;font-weight:bold;\">").append(text).append("</a>");
                                            htmlBuilder.append("</td></tr></table>");
                                        } else if ("image".equals(type)) {
                                            // 处理图片类型
                                            String imageUrl = "";
                                            if (content.has("values") && !content.get("values").isNull()) {
                                                JsonNode values = content.get("values");
                                                if (values.has("src") && !values.get("src").isNull()) {
                                                    if (values.get("src").isObject() && values.get("src").has("url")) {
                                                        // 如果src是一个对象，获取其url属性
                                                        imageUrl = values.get("src").get("url").asText();
                                                        System.out.println("图片src是对象，使用src.url: " + imageUrl);
                                                    } else {
                                                        // 直接使用src值
                                                        imageUrl = values.get("src").asText();
                                                    }
                                                } else if (values.has("url") && !values.get("url").isNull()) {
                                                    imageUrl = values.get("url").asText();
                                                } else if (values.has("href") && !values.get("href").isNull()) {
                                                    imageUrl = values.get("href").asText();
                                                }
                                            }
                                            
                                            if (!imageUrl.isEmpty()) {
                                                // 确保图片URL是完整的URL
                                                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("data:")) {
                                                    // 获取图片文件名作为content-id
                                                    String imgFileName = imageUrl;
                                                    if (imageUrl.contains("/")) {
                                                        imgFileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                                                    }
                                                    // 使用一个更简单的Content-ID格式，避免特殊字符
                                                    String contentId = imgFileName.replaceAll("[^a-zA-Z0-9.]", "_");
                                                    
                                                    // 注意：确保contentId不包含扩展名的点号
                                                    if (contentId.contains(".")) {
                                                        contentId = contentId.substring(0, contentId.lastIndexOf('.'));
                                                    }
                                                    
                                                    // 不添加时间戳，因为在HTML生成时还不知道邮件发送时的确切时间戳
                                                    // 但是我们可以添加一个固定的前缀，确保能匹配到后面生成的ID
                                                    contentId = contentId + "_img";
                                                    
                                                    imageUrl = "cid:" + contentId;
                                                    System.out.println("处理图片: " + imgFileName + " -> contentId: " + contentId + " -> imageUrl: " + imageUrl);
                                                }
                                                
                                                // 获取对齐方式
                                                String alignment = "center"; // 默认居中
                                                if (content.has("values") && !content.get("values").isNull()) {
                                                    JsonNode values = content.get("values");
                                                    if (values.has("align") && !values.get("align").isNull()) {
                                                        String alignValue = values.get("align").asText();
                                                        if ("left".equals(alignValue) || "right".equals(alignValue) || 
                                                            "center".equals(alignValue) || "justify".equals(alignValue)) {
                                                            alignment = alignValue;
                                                        }
                                                    }
                                                }
                                                
                                                String altText = text.isEmpty() ? "Image" : text;
                                                
                                                // 特殊处理图片对齐方式 - 使用最简单有效的方法
                                                if ("left".equals(alignment)) {
                                                    // 左对齐 - 使用表格方式
                                                    htmlBuilder.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr>");
                                                    htmlBuilder.append("<td align=\"left\">");
                                                    htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                        .append(altText).append("\" style=\"max-width:100%; height:auto;\"/>");
                                                    htmlBuilder.append("</td>");
                                                    htmlBuilder.append("<td></td>"); // 空单元格占位
                                                    htmlBuilder.append("</tr></table>");
                                                } else if ("right".equals(alignment)) {
                                                    // 右对齐 - 使用表格方式
                                                    htmlBuilder.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr>");
                                                    htmlBuilder.append("<td></td>"); // 空单元格占位
                                                    htmlBuilder.append("<td align=\"right\">");
                                                    htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                        .append(altText).append("\" style=\"max-width:100%; height:auto;\"/>");
                                                    htmlBuilder.append("</td>");
                                                    htmlBuilder.append("</tr></table>");
                                                } else {
                                                    // 居中对齐 - 使用表格方式
                                                    htmlBuilder.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr>");
                                                    htmlBuilder.append("<td align=\"center\">");
                                                    htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                        .append(altText).append("\" style=\"max-width:100%; height:auto;\"/>");
                                                    htmlBuilder.append("</td>");
                                                    htmlBuilder.append("</tr></table>");
                                                }
                                            }
                                        } else if ("html".equals(type)) {
                                            // 直接处理HTML内容，保留原始HTML代码
                                            // 从values中获取HTML内容
                                            String htmlContent = "";
                                            String alignment = "left"; // 默认左对齐
                                            
                                            if (content.has("values") && !content.get("values").isNull()) {
                                                JsonNode values = content.get("values");
                                                if (values.has("html") && !values.get("html").isNull()) {
                                                    htmlContent = values.get("html").asText();
                                                } else if (values.has("text") && !values.get("text").isNull()) {
                                                    htmlContent = values.get("text").asText();
                                                }
                                                
                                                // 获取HTML内容对齐方式
                                                if (values.has("align") && !values.get("align").isNull()) {
                                                    String alignValue = values.get("align").asText();
                                                    if ("left".equals(alignValue) || "right".equals(alignValue) || 
                                                        "center".equals(alignValue) || "justify".equals(alignValue)) {
                                                        alignment = alignValue;
                                                    }
                                                }
                                            }
                                            
                                            // 直接将HTML内容插入到输出中，不做任何处理或过滤
                                            if (!htmlContent.isEmpty()) {
                                                // 包装在一个div中，确保HTML内容能够正确渲染，并应用对齐样式
                                                htmlBuilder.append("<div style=\"width:100%; text-align:").append(alignment).append(";\">").append(htmlContent).append("</div>");
                                            }
                                        }
                                    }
                                }

                                htmlBuilder.append("</td>"); // Close column cell
                            }
                        }

                        htmlBuilder.append("</tr></table>"); // Close row table
                        htmlBuilder.append("</td></tr>"); // Close row container
                    }
                    
                    htmlBuilder.append("</table>"); // Close main container table
                }
            } else {
                // 如果没有正确的body结构，添加默认内容
                htmlBuilder.append("<table width=\"100%\"><tr><td>").append(json).append("</td></tr></table>");
            }

            htmlBuilder.append("</body></html>");
            return htmlBuilder.toString();
        } catch (Exception e) {
            // 记录错误但不抛出异常，而是返回简单的HTML
            System.err.println("Error converting JSON to HTML: " + e.getMessage());
            return "<html><body><p>Error parsing template content: " + e.getMessage() + "</p><pre>" + json + "</pre></body></html>";
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
