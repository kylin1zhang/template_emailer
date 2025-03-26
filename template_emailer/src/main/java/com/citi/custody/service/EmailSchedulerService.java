package com.citi.custody.service;

import com.citi.custody.dao.EmailDao;
import com.citi.custody.entity.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@EnableScheduling
public class EmailSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(EmailSchedulerService.class);
    
    @Autowired
    private EmailDao emailDao;
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void sendScheduledEmails() {
        logger.info("Checking for scheduled emails...");
        Date now = new Date();
        
        // 找出状态为 SCHEDULED 且发送时间已到或过去的邮件
        List<Email> emailsToSend = emailDao.findByStatusAndSentTimeBefore("SCHEDULED", now);
        
        logger.info("Found {} emails to send", emailsToSend.size());
        
        for (Email email : emailsToSend) {
            try {
                emailSenderService.sendEmail(email.getId());
            } catch (Exception e) {
                logger.error("Error sending scheduled email {}: {}", email.getId(), e.getMessage(), e);
                email.setStatus("FAILED");
                email.setErrorMessage(e.getMessage());
                emailDao.saveEmail(email);
            }
        }
    }
} 