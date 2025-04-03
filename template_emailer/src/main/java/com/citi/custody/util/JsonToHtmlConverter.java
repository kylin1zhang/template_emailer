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

            // Start building the HTML
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html><head><title>Email Template</title></head><body>");

            // Parse the "body" node
            JsonNode bodyNode = rootNode.get("body");
            if (bodyNode != null) {
                JsonNode rows = bodyNode.get("rows");
                if (rows != null && rows.isArray()) {
                    for (JsonNode row : rows) {
                        htmlBuilder.append("<div style='margin: 10px;'>"); // Row container

                        JsonNode columns = row.get("columns");
                        if (columns != null && columns.isArray()) {
                            for (JsonNode column : columns) {
                                htmlBuilder.append("<div style='display: inline-block; margin: 5px;'>"); // Column container

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
                                            htmlBuilder.append("<p>").append(text).append("</p>");
                                        } else if ("heading".equals(type)) {
                                            htmlBuilder.append("<h1>").append(text).append("</h1>");
                                        } else if ("button".equals(type)) {
                                            htmlBuilder.append("<button>").append(text).append("</button>");
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
                                                
                                                String altText = text.isEmpty() ? "Image" : text;
                                                htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                    .append(altText).append("\" style=\"max-width:100%;height:auto;\"/>");
                                            }
                                        }
                                    }
                                }

                                htmlBuilder.append("</div>"); // Close column container
                            }
                        }

                        htmlBuilder.append("</div>"); // Close row container
                    }
                }
            } else {
                // 如果没有正确的body结构，添加默认内容
                htmlBuilder.append("<div><p>").append(json).append("</p></div>");
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
