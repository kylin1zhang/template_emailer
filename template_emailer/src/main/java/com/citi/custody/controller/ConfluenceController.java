package com.citi.custody.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.http.*;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/confluence")
public class ConfluenceController {

    @Value("${confluence.api.url}")
    private String confluenceApiUrl;
    
    @Value("${confluence.api.token}")
    private String apiToken;
    
    @PostMapping("/createPage")
    @Description("Create a new page in Confluence, parentId = ID of the parent page, title = Title of the new page")
    public ResponseEntity<String> createPage(@RequestParam(required = false) String parentId,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) String spacekey) {
        try {
            // Load JSON template
            File file = ResourceUtils.getFile("classpath:confluence-template.json");
            String jsonTemplate = new String(Files.readAllBytes(file.toPath()));
            
            // Replace placeholders
            String requestBody = jsonTemplate
                .replace("{{title}}", "Test2")
                .replace("{{parentid}}", "2574293788")
                .replace("{{spacekey}}", "t8667f");
                
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);
            
            // Create HttpEntity
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // Send POST request
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                confluenceApiUrl + "/rest/api/content",
                HttpMethod.POST,
                entity,
                String.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating page: " + e.getMessage());
        }
    }
    
    @GetMapping("/getPageContent/{pageId}")
    @Description("Retrieve the content of a specific Confluence page")
    public ResponseEntity<String> getPageContent(@PathVariable String pageId) {
        try {
            // Construct the URL with the page ID and expand parameter
            String url = confluenceApiUrl + "/rest/api/content/" + pageId + "?expand=body.storage";
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            
            // Create HttpEntity
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Send GET request
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving page content: " + e.getMessage());
        }
    }
}