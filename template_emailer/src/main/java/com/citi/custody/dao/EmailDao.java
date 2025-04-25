package com.citi.custody.dao;

import com.citi.custody.entity.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailDao extends MongoRepository<Email, String> {
    Logger logger = LoggerFactory.getLogger(EmailDao.class);
    
    @Query("{ 'emailName': { $regex: ?0, $options: 'i' } }")
    Page<Email> findAllByName(String name, Pageable pageable);
    
    // 添加安全的查询方法
    default Page<Email> findAllByNameSafely(String name, Pageable pageable) {
        logger.debug("Finding emails by name (safely): {}", name);
        if (name == null || name.trim().isEmpty()) {
            // 如果name为null或空，则查找所有邮件
            logger.debug("Name is null or empty, finding all emails");
            return findAll(pageable);
        } else {
            try {
                logger.debug("Finding emails with name pattern: {}", name);
                return findAllByName(name, pageable);
            } catch (Exception e) {
                logger.error("Error finding emails by name {}: {}", name, e.getMessage(), e);
                // 出错时返回所有邮件
                return findAll(pageable);
            }
        }
    }
    
    
    // 添加自定义方法
    default Email findEmailById(String id) {
        logger.debug("Finding email by ID: {}", id);
        if (id == null || id.trim().isEmpty()) {
            logger.warn("Attempted to find email with null or empty ID");
            return null;
        }
        
        try {
            Optional<Email> email = findById(id);
            if (email.isPresent()) {
                logger.debug("Found email with ID: {}", id);
                return email.get();
            } else {
                logger.debug("No email found with ID: {}", id);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error finding email with ID {}: {}", id, e.getMessage(), e);
            return null;
        }
    }
    
    // 修改saveEmail方法
    default Email saveEmail(Email email) {
        logger.debug("Saving email: {}", email);
        if (email == null) {
            logger.warn("Attempted to save null email");
            throw new IllegalArgumentException("Email cannot be null");
        }
        
        try {
            // 确保ID为null时MongoDB会自动生成新ID
            if (email.getId() != null && email.getId().trim().isEmpty()) {
                email.setId(null);
                logger.debug("Empty ID converted to null for MongoDB to generate ID");
            }
            
            Email savedEmail = save(email);
            
            if (savedEmail == null) {
                logger.error("MongoDB returned null after save operation");
                throw new RuntimeException("Failed to save email: MongoDB returned null");
            }
            
            if (savedEmail.getId() == null || savedEmail.getId().trim().isEmpty()) {
                logger.error("MongoDB did not generate ID for the saved email");
                throw new RuntimeException("Failed to save email: MongoDB did not generate ID");
            }
            
            logger.debug("Email saved successfully with ID: {}", savedEmail.getId());
            return savedEmail;
        } catch (Exception e) {
            logger.error("Error saving email: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving email: " + e.getMessage(), e);
        }
    }
    
    @Query("{ 'status': ?0, 'sentTime': { $lte: ?1 } }")
    List<Email> findByStatusAndSentTimeBefore(String status, Date sentTime);
}
