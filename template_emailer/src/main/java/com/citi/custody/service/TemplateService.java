package com.citi.custody.service;

import com.citi.custody.entity.TemplateInfo;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        
        if(StringUtils.isNotEmpty(objectId) && isTemplateExist(objectId)) {
            // Delete the existing file
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(objectId)));
        }

        try (InputStream inputStream = file.getInputStream()) {
            String originalFileName = file.getOriginalFilename();
            Document metadata = generateMetaData(originalFileName);
            ObjectId newObjectId = gridFsTemplate.store(inputStream, originalFileName, file.getContentType(), metadata);
            log.info("Successfully uploaded file: {}, id: {}", originalFileName, newObjectId.toHexString());
        }

    }

    private Document generateMetaData(String originalFileName) {
        String fileName = originalFileName.replaceAll("\\.json$", "");
        // Create metadata
        Document metadata = new Document();

        metadata.put("updateBy", "TEST");
        metadata.put("updateTime", new Date());
        metadata.put("filename", fileName);
        return metadata;
    }

    private boolean isTemplateExist(String objectId) {
        GridFSFile existingFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(objectId)));
        if (existingFile != null){
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

    public TemplateInfo setTemplateInfo(GridFSFile file) {
        if(file != null) {
            Document metadata = file.getMetadata();
            TemplateInfo templateInfo = new TemplateInfo();
            templateInfo.setId(file.getObjectId().toHexString());
            if(metadata != null){
            templateInfo.setFilename(metadata.getString("filename"));
            templateInfo.setUpdateBy(metadata.getString("updateBy"));
            templateInfo.setUpdateTime((Date) metadata.get("updateTime"));
            return templateInfo;
            }
        }
        return null;
    }

    public Page<TemplateInfo> getTemplates(String name, Pageable pageable) {
        List<TemplateInfo> templates = new ArrayList<>();
        Query query = new Query(Criteria.where("metadata.updateBy").is("TEST"));
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
            if(templateInfo != null){
            templates.add(templateInfo);
            }
        }
        return templates;
    }
}
