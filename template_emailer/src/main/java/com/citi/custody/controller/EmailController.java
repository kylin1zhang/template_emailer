package com.citi.custody.controller;

import com.citi.custody.constant.ErrorCodeConstants;
import com.citi.custody.entity.Email;
import com.citi.custody.entity.FilterParams;
import com.citi.custody.service.EmailService;
import com.citi.custody.util.AssertUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    EmailService emailService;

    @PostMapping("/save")
    public String saveEmail(@RequestBody Email email) {
        AssertUtils.isTrue(StringUtils.isNotBlank(email.getEmailName()) && StringUtils.isNotBlank(email.getContentTemplateId()),
                ErrorCodeConstants.INVALID_PARAMETER_ERROR, "emailName, contentTemplateId, and createdBy can not be empty!");
        return emailService.saveEmail(email);
    }

    @PostMapping("/emailsList")
    public Page<Email> getEmailsList(@RequestBody FilterParams params) {
        Pageable pageable = PageRequest.of(params.getPage(), params.getSize());
        Page<Email> emailsList = emailService.getEmailsList(params.getName(), pageable);
        return emailsList;
    }

    @GetMapping("/{id}")
    public Email getEmailById(@PathVariable String id) {
        return emailService.getEmailById(id);
    }
}
