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
