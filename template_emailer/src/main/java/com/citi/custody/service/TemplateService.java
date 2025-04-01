package com.citi.custody.service;

import com.citi.custody.entity.TemplateInfo;
import com.citi.custody.util.SystemUserUtil;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class TemplateService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    public void storeTemplate(MultipartFile file, String objectId) throws IOException {
        if (StringUtils.isNotEmpty(objectId) && isTemplateExist(objectId)) {
            // Delete the existing file
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(objectId)));
        }

        try (InputStream inputStream = file.getInputStream()) {
            String originalFileName = file.getOriginalFilename();
            Document metadata = generateMetaData(originalFileName);
            ObjectId newObjectId = gridFsTemplate.store(inputStream, originalFileName, file.getContentType(), metadata);
            log.info("Successfully uploaded file: {} (id: {})", originalFileName, newObjectId.toHexString());
        }

    }

    private Document generateMetaData(String originalFileName) {
        String fileName = originalFileName.replaceAll("\\.json$", "");
        // Create metadata
        Document metadata = new Document();

        metadata.put("updateBy", SystemUserUtil.getCurrentUsername());
        metadata.put("updateTime", new Date());
        metadata.put("filename", fileName);
        return metadata;
    }

    private boolean isTemplateExist(String objectId) {
        GridFSFile existingFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(objectId)));
        if (existingFile != null) {
            return true;
        }
        return false;
    }

    public InputStreamResource loadTemplate(String objectId) throws IOException {
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(objectId)));
        if (gridFSFile == null) {
            throw new IOException("Template not found");
        }
        InputStream inputStream = gridFsTemplate.getResource(gridFSFile).getInputStream();
        return new InputStreamResource(inputStream);
    }

    private String getTemplateContent(String objectId) {
        try {
            GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(objectId)));
            if (gridFSFile == null) {
                return null;
            }
            InputStream inputStream = gridFsTemplate.getResource(gridFSFile).getInputStream();
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading template content: ", e);
            return null;
        }
    }

    public TemplateInfo setTemplateInfo(GridFSFile file) {
        if (file != null) {
            Document metadata = file.getMetadata();
            TemplateInfo templateInfo = new TemplateInfo();
            templateInfo.setId(file.getObjectId().toHexString());
            if (metadata != null) {
                templateInfo.setFilename(metadata.getString("filename"));
                templateInfo.setUpdateBy(metadata.getString("updateBy"));
                templateInfo.setUpdateTime((Date) metadata.get("updateTime"));
                // 设置模板内容
                String content = getTemplateContent(file.getObjectId().toHexString());
                templateInfo.setContent(content);
                return templateInfo;
            }
        }
        return null;
    }

    public Page<TemplateInfo> getTemplates(String name, Pageable pageable) {
        List<TemplateInfo> templates = new ArrayList<>();
        Query query = new Query(Criteria.where("metadata.updateBy").is(SystemUserUtil.getCurrentUsername()));
        if (name != null && !name.isEmpty()) {
            query.addCriteria(Criteria.where("metadata.filename").regex(name, "i"));
        }
        query.with(pageable);
        List<GridFSFile> files = gridFsTemplate.find(query).into(new ArrayList<>());

        for (GridFSFile file : files) {
            TemplateInfo templateInfo = setTemplateInfo(file);
            if (templateInfo != null) {
                templates.add(templateInfo);
            }
        }
        return new PageImpl<>(templates, pageable, files.size());
    }

    public TemplateInfo getTemplateById(String id) {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
        TemplateInfo templateInfo = setTemplateInfo(file);
        return templateInfo;
    }

    public List<TemplateInfo> getTemplateByUpdatedBy(String updateBy) {
        List<TemplateInfo> templates = new ArrayList<>();
        Query query = new Query(Criteria.where("metadata.updateBy").is(updateBy));
        List<GridFSFile> files = gridFsTemplate.find(query).into(new ArrayList<>());

        for (GridFSFile file : files) {
            TemplateInfo templateInfo = setTemplateInfo(file);
            if (templateInfo != null) {
                templates.add(templateInfo);
            }
        }
        return templates;
    }

    /**
     * 存储测试模板并返回模板ID
     */
    public String storeTestTemplate(TemplateInfo templateInfo) {
        try {
            // 确保模板信息不为空
            if (templateInfo == null) {
                throw new IllegalArgumentException("Template information cannot be empty");
            }
            
            // 确保模板内容不为空
            if (StringUtils.isEmpty(templateInfo.getContent())) {
                throw new IllegalArgumentException("Template content cannot be empty");
            }
            
            // 生成元数据
            Document metadata = new Document();
            metadata.put("updateBy", templateInfo.getUpdateBy() != null ? templateInfo.getUpdateBy() : "SYSTEM");
            metadata.put("updateTime", templateInfo.getUpdateTime() != null ? templateInfo.getUpdateTime() : new Date());
            metadata.put("filename", templateInfo.getFilename() != null ? templateInfo.getFilename() : "Test-Template-" + System.currentTimeMillis());
            
            // 存储模板内容并获取ID
            ObjectId objectId = gridFsTemplate.store(
                new ByteArrayInputStream(templateInfo.getContent().getBytes(StandardCharsets.UTF_8)),
                templateInfo.getFilename(),
                "application/json",
                metadata
            );
            
            log.info("Test template saved successfully, ID: {}", objectId.toHexString());
            return objectId.toHexString();
        } catch (Exception e) {
            log.error("Error storing test template: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store test template: " + e.getMessage(), e);
        }
    }

    /**
     * 删除指定ID的模板
     * @param id 模板ID
     * @return 删除结果，true表示成功，false表示失败
     */
    public boolean deleteTemplate(String id) {
        try {
            log.info("Attempting to delete template, ID: {}", id);
            
            if (StringUtils.isBlank(id)) {
                log.warn("Failed to delete template: ID is empty");
                return false;
            }
            
            // 检查模板是否存在
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
            if (file == null) {
                log.warn("Failed to delete template: Template with ID {} not found", id);
                return false;
            }
            
            // 获取模板元数据
            Document metadata = file.getMetadata();
            String filename = file.getFilename();
            log.info("Template found: ID={}, filename={}, size={} bytes", id, filename, file.getLength());
            
            // 检查当前用户是否有权限删除
            // 注意：这里使用了用户权限检查，可根据需要调整或删除
            String currentUser = SystemUserUtil.getCurrentUsername();
            if (metadata != null) {
                String updateBy = metadata.getString("updateBy");
                if (!currentUser.equals(updateBy)) {
                    log.warn("Failed to delete template: User {} is not the template creator {}", currentUser, updateBy);
                    return false;
                }
                log.info("User {} has permission to delete this template", currentUser);
            }
            
            // 检查模板是否被邮件引用
            // 如果需要，可以在这里添加检查逻辑
            
            // 执行删除操作
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(id)));
            log.info("Template deleted successfully: ID={}, filename={}", id, filename);
            return true;
        } catch (Exception e) {
            log.error("Error deleting template {}: {}", id, e.getMessage(), e);
            return false;
        }
    }
}
