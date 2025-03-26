package com.citi.custody.service;

import com.citi.custody.dao.EmailDao;
import com.citi.custody.entity.Email;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private EmailDao emailDao;

    @Transactional
    public String saveEmail(Email email) {
        logger.info("Saving email: {}", email);
        
        try {
            // Validate email object
            if (email == null) {
                logger.error("Cannot save null email object");
                throw new IllegalArgumentException("Email object cannot be null");
            }
            
            // Validate required fields
            if (StringUtils.isBlank(email.getEmailName())) {
                logger.error("Email name is required");
                throw new IllegalArgumentException("Email name is required");
            }
            
            if (StringUtils.isBlank(email.getContentTemplateId())) {
                logger.error("Content template ID is required");
                throw new IllegalArgumentException("Content template ID is required");
            }
            
            // 确保ID字段为空或null时，MongoDB会自动生成
            if (StringUtils.isEmpty(email.getId())) {
                email.setId(null); // 显式设置为null让MongoDB生成ID
                logger.debug("ID is empty, MongoDB will generate a new ID");
            }
            
            email.setModifiedTime(new Date());
            email.setCreatedBy("TEST");
            
            if (email.getId() == null) {
                email.setCreateTime(new Date());
                logger.debug("Creating new email with subject: {}", email.getEmailName());
            } else {
                logger.debug("Updating existing email with ID: {}", email.getId());
            }
            
            logger.debug("Calling emailDao.saveEmail with email: {}", email);
            Email savedEmail = emailDao.saveEmail(email);
            logger.debug("Result from emailDao.saveEmail: {}", savedEmail);
            
            if (savedEmail != null) {
                String savedId = savedEmail.getId();
                if (StringUtils.isNotBlank(savedId)) {
                    logger.info("Email saved successfully with ID: {}", savedId);
                    return savedId;
                } else {
                    logger.error("Failed to save email: MongoDB returned null or empty ID");
                    throw new RuntimeException("Failed to save email: MongoDB returned null or empty ID");
                }
            } else {
                logger.error("Failed to save email: MongoDB returned null object");
                throw new RuntimeException("Failed to save email: MongoDB returned null object");
            }
        } catch (Exception e) {
            logger.error("Error saving email: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving email: " + e.getMessage(), e);
        }
    }

    public Page<Email> getEmailsList(String name, Pageable pageable) {
        logger.debug("Getting emails list with name filter: {}, page: {}, size: {}", 
                    name, pageable.getPageNumber(), pageable.getPageSize());
        return emailDao.findAllByNameSafely(name, pageable);
    }

    public Email getEmailById(String id) {
        logger.debug("Getting email by ID: {}", id);
        Email email = emailDao.findEmailById(id);
        if (email == null) {
            logger.warn("Email not found with ID: {}", id);
        }
        return email;
    }

    public boolean deleteEmail(String id) {
        logger.info("Deleting email with ID: {}", id);
        try {
            if (StringUtils.isBlank(id)) {
                logger.error("Cannot delete email with null or empty ID");
                return false;
            }
            
            // 检查邮件是否存在
            Email email = getEmailById(id);
            if (email == null) {
                logger.warn("Email not found with ID: {}", id);
                return false;
            }
            
            // 如果邮件已发送，不允许删除
            if ("SENT".equals(email.getStatus())) {
                logger.warn("Cannot delete email that has been sent, ID: {}", id);
                return false;
            }
            
            emailDao.deleteById(id);
            logger.info("Email deleted successfully, ID: {}", id);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting email with ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }
}
