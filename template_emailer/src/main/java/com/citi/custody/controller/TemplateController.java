package com.citi.custody.controller;

import com.citi.custody.entity.FilterParams;
import com.citi.custody.entity.TemplateInfo;
import com.citi.custody.entity.User;
import com.citi.custody.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/template")
public class TemplateController {
    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TemplateService templateService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, @RequestParam(value = "objectId", required = false) String objectId) {
        try {
            templateService.storeTemplate(file, objectId);
            return ResponseEntity.ok("File uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/load/{objectId}")
    public ResponseEntity<InputStreamResource> loadTemplate(@PathVariable String objectId) {
        try {
            InputStreamResource resource = templateService.loadTemplate(objectId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + objectId)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/templatesList")
    public Page<TemplateInfo> getTemplates(@RequestBody FilterParams params) {
        Pageable pageable = PageRequest.of(params.getPage(), params.getSize());
        return templateService.getTemplates(params.getName(), pageable);
    }

    @GetMapping("/{id}")
    public TemplateInfo getTemplateById(@PathVariable String id) {
        return templateService.getTemplateById(id);
    }

    @GetMapping("/update/{updateBy}")
    public List<TemplateInfo> getTemplateByUpdatedBy(@PathVariable String updateBy) {
        return templateService.getTemplateByUpdatedBy(updateBy);
    }

    /**
     * 删除指定ID的模板
     * @param id 要删除的模板ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable String id) {
        log.info("Received request to delete template, ID: {}", id);
        
        if (id == null || id.trim().isEmpty()) {
            log.warn("Failed to delete template: ID is empty");
            return ResponseEntity.badRequest().body("Template ID cannot be empty");
        }
        
        try {
            boolean deleted = templateService.deleteTemplate(id);
            if (deleted) {
                log.info("Template deleted successfully, ID: {}", id);
                return ResponseEntity.ok().body("Template deleted successfully");
            } else {
                log.warn("Failed to delete template, ID: {}", id);
                return ResponseEntity.status(403).body("Failed to delete template, insufficient permissions or template does not exist");
            }
        } catch (Exception e) {
            log.error("Exception occurred while deleting template, ID: {}, error: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body("Error occurred while deleting template: " + e.getMessage());
        }
    }
}
