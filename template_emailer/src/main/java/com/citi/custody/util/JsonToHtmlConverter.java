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
            htmlBuilder.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
            htmlBuilder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">");
            
            // Add head with meta tags and styles for Outlook compatibility
            htmlBuilder.append("<head>");
            htmlBuilder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
            htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            htmlBuilder.append("<meta name=\"format-detection\" content=\"telephone=no\">");
            htmlBuilder.append("<!--[if mso]>");
            htmlBuilder.append("<xml>");
            htmlBuilder.append("<o:OfficeDocumentSettings>");
            htmlBuilder.append("<o:AllowPNG/>");
            htmlBuilder.append("<o:PixelsPerInch>96</o:PixelsPerInch>");
            htmlBuilder.append("</o:OfficeDocumentSettings>");
            htmlBuilder.append("</xml>");
            htmlBuilder.append("<![endif]-->");
            htmlBuilder.append("<style type=\"text/css\">");
            // Reset styles
            htmlBuilder.append("body, p, h1, h2, h3, h4, h5, h6, table, td {margin: 0; padding: 0;}");
            // Default font
            htmlBuilder.append("body {font-family: Arial, sans-serif; -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%;}");
            // Default table styles
            htmlBuilder.append("table, td {border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt;}");
            // Default image styles
            htmlBuilder.append("img {-ms-interpolation-mode: bicubic; border: 0; display: block; height: auto; line-height: 100%; outline: none; text-decoration: none;}");
            htmlBuilder.append("</style>");
            htmlBuilder.append("<title>Email Template</title>");
            htmlBuilder.append("</head>");
            
            htmlBuilder.append("<body style=\"margin: 0; padding: 0; background-color: #f7f7f7;\">");
            htmlBuilder.append("<div style=\"background-color: #f7f7f7; max-width: 600px; margin: 0 auto;\">");
            htmlBuilder.append("<!--[if mso]>");
            htmlBuilder.append("<table align=\"center\" role=\"presentation\" width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
            htmlBuilder.append("<tr><td>");
            htmlBuilder.append("<![endif]-->");

            // Parse the "body" node
            JsonNode bodyNode = rootNode.get("body");
            if (bodyNode != null) {
                JsonNode rows = bodyNode.get("rows");
                if (rows != null && rows.isArray()) {
                    // Begin email table container for Outlook compatibility
                    htmlBuilder.append("<table width='100%' border='0' cellspacing='0' cellpadding='0' style='border-collapse: collapse;'>");
                    
                    for (JsonNode row : rows) {
                        // Each row becomes a table row
                        htmlBuilder.append("<tr><td style='padding: 10px;'>");
                        
                        // Create a nested table for columns
                        htmlBuilder.append("<table width='100%' border='0' cellspacing='0' cellpadding='0' style='border-collapse: collapse;'><tr>");

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
                                
                                // Create column as table cell with calculated width
                                htmlBuilder.append("<td valign='top' style='padding: 10px; width: ")
                                    .append(widthPercentage)
                                    .append("%;'>");

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
                                            htmlBuilder.append("<p style='width: 100%; margin: 0 0 10px 0;'>").append(text).append("</p>");
                                        } else if ("heading".equals(type)) {
                                            htmlBuilder.append("<h1 style='width: 100%; margin: 0 0 10px 0;'>").append(text).append("</h1>");
                                        } else if ("button".equals(type)) {
                                            // Use MSO conditional comments for better Outlook button rendering
                                            htmlBuilder.append("<!--[if mso]>")
                                                    .append("<v:roundrect xmlns:v='urn:schemas-microsoft-com:vml' xmlns:w='urn:schemas-microsoft-com:office:word' style='height:36px;v-text-anchor:middle;width:150px;' arcsize='5%' strokecolor='#2e6da4' fillcolor='#337ab7'>")
                                                    .append("<w:anchorlock/>")
                                                    .append("<center style='color:#ffffff;font-family:sans-serif;font-size:13px;font-weight:bold;'>").append(text).append("</center>")
                                                    .append("</v:roundrect>")
                                                    .append("<![endif]-->")
                                                    .append("<!--[if !mso]><!-->")
                                                    .append("<table cellspacing='0' cellpadding='0' style='width:auto;margin:0 auto;'><tr>")
                                                    .append("<td style='border-radius:4px;background-color:#337ab7;padding:8px 16px;'>")
                                                    .append("<a href='#' style='background-color:#337ab7;color:#ffffff;font-family:sans-serif;font-size:13px;font-weight:bold;text-decoration:none;display:inline-block;'>").append(text).append("</a>")
                                                    .append("</td></tr></table>")
                                                    .append("<!--<![endif]-->");
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
                                                // Ensure proper image sizing for Outlook with width attribute
                                                htmlBuilder.append("<img src=\"").append(imageUrl).append("\" alt=\"")
                                                    .append(altText).append("\" width=\"100%\" style=\"display:block;width:100%;max-width:100%;height:auto;\"/>");
                                            }
                                        }
                                    }
                                }

                                htmlBuilder.append("</td>"); // Close column cell
                            }
                        }

                        htmlBuilder.append("</tr></table>"); // Close column table
                        htmlBuilder.append("</td></tr>"); // Close row
                    }
                    
                    htmlBuilder.append("</table>"); // Close email container table
                }
            } else {
                // If there is no correct body structure, add default content
                htmlBuilder.append("<table width='100%'><tr><td>").append(json).append("</td></tr></table>");
            }

            htmlBuilder.append("<!--[if mso]>");
            htmlBuilder.append("</td></tr>");
            htmlBuilder.append("</table>");
            htmlBuilder.append("<![endif]-->");
            htmlBuilder.append("</div>");
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
