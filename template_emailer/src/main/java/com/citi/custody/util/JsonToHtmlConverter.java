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
                        htmlBuilder.append("<div style='display: flex; flex-wrap: wrap; margin: 10px; width: 100%;'>"); // Row container as flex

                        JsonNode columns = row.get("columns");
                        if (columns != null && columns.isArray()) {
                            // Calculate column proportions and widths
                            int totalColumns = columns.size();
                            int[] columnRatios = new int[totalColumns];
                            int totalRatio = 0;
                            
                            // Check if columns have explicit width ratios
                            boolean hasExplicitRatios = false;
                            for (int i = 0; i < totalColumns; i++) {
                                JsonNode column = columns.get(i);
                                if (column.has("ratio") && column.get("ratio").isInt()) {
                                    hasExplicitRatios = true;
                                    columnRatios[i] = column.get("ratio").asInt();
                                    totalRatio += columnRatios[i];
                                } else {
                                    columnRatios[i] = 1; // Default ratio
                                    totalRatio += 1;
                                }
                            }
                            
                            // If no explicit ratios, use equal distribution
                            if (!hasExplicitRatios) {
                                totalRatio = totalColumns;
                            }

                            // Render each column with appropriate width
                            for (int i = 0; i < totalColumns; i++) {
                                JsonNode column = columns.get(i);
                                int ratio = columnRatios[i];
                                float widthPercentage = (ratio * 100.0f) / totalRatio;
                                
                                // Create column with calculated width
                                htmlBuilder.append("<div style='flex: ")
                                    .append(ratio)
                                    .append("; min-width: ")
                                    .append(widthPercentage)
                                    .append("%; padding: 10px; box-sizing: border-box;'>"); // Column container with flex ratio

                                JsonNode contents = column.get("contents");
                                if (contents != null && contents.isArray()) {
                                    for (JsonNode content : contents) {
                                        // Get content type safely
                                        String type = "text"; // Default to text type
                                        if (content.has("type") && !content.get("type").isNull()) {
                                            type = content.get("type").asText();
                                        }
                                        
                                        // Get text value safely
                                        String text = "";
                                        if (content.has("values") && !content.get("values").isNull()) {
                                            JsonNode values = content.get("values");
                                            if (values.has("text") && !values.get("text").isNull()) {
                                                text = values.get("text").asText();
                                            }
                                        }

                                        if ("text".equals(type)) {
                                            htmlBuilder.append("<p style='width: 100%;'>").append(text).append("</p>");
                                        } else if ("heading".equals(type)) {
                                            htmlBuilder.append("<h1 style='width: 100%;'>").append(text).append("</h1>");
                                        } else if ("button".equals(type)) {
                                            htmlBuilder.append("<button style='width: 100%;'>").append(text).append("</button>");
                                        } else if ("image".equals(type)) {
                                            // Process image type
                                            String imageUrl = "";
                                            if (content.has("values") && !content.get("values").isNull()) {
                                                JsonNode values = content.get("values");
                                                if (values.has("src") && !values.get("src").isNull()) {
                                                    if (values.get("src").isObject() && values.get("src").has("url")) {
                                                        // If src is an object, get its url property
                                                        imageUrl = values.get("src").get("url").asText();
                                                        System.out.println("Image src is an object, using src.url: " + imageUrl);
                                                    } else {
                                                        // Use src value directly
                                                        imageUrl = values.get("src").asText();
                                                    }
                                                } else if (values.has("url") && !values.get("url").isNull()) {
                                                    imageUrl = values.get("url").asText();
                                                } else if (values.has("href") && !values.get("href").isNull()) {
                                                    imageUrl = values.get("href").asText();
                                                }
                                            }
                                            
                                            if (!imageUrl.isEmpty()) {
                                                // Ensure image URL is a complete URL
                                                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("data:")) {
                                                    // Get image filename for content-id
                                                    String imgFileName = imageUrl;
                                                    if (imageUrl.contains("/")) {
                                                        imgFileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                                                    }
                                                    // Use a simpler Content-ID format to avoid special characters
                                                    String contentId = imgFileName.replaceAll("[^a-zA-Z0-9.]", "_");
                                                    
                                                    // Note: Make sure contentId doesn't contain extension dot
                                                    if (contentId.contains(".")) {
                                                        contentId = contentId.substring(0, contentId.lastIndexOf('.'));
                                                    }
                                                    
                                                    // Don't add timestamp as we don't know the exact time of email sending
                                                    // But we can add a fixed prefix to ensure it can match with generated ID
                                                    contentId = contentId + "_img";
                                                    
                                                    imageUrl = "cid:" + contentId;
                                                    System.out.println("Processing image: " + imgFileName + " -> contentId: " + contentId + " -> imageUrl: " + imageUrl);
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
                // If there is no correct body structure, add default content
                htmlBuilder.append("<div><p>").append(json).append("</p></div>");
            }

            htmlBuilder.append("</body></html>");
            return htmlBuilder.toString();
        } catch (Exception e) {
            // Log error but don't throw exception, return simple HTML
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
