package com.citi.custody.service;

import com.citi.custody.dao.EmailDao;
import com.citi.custody.entity.Email;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class EmailService {

    @Autowired
    private EmailDao emailDao;

    public String saveEmail(Email email) {
        email.setModifiedTime(new Date());
        email.setCreatedBy("TEST");
        if (StringUtils.isEmpty(email.getId())) {
            email.setCreateTime(new Date());
        }
        emailDao.saveEmail(email);
        return email.getId();
    }

    public Page<Email> getEmailsList(String name, Pageable pageable) {
        return emailDao.findAllByName(name, pageable);
    }

    public Email getEmailById(String id) {
        return emailDao.findById(id);
    }
}
