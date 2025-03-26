package com.citi.custody.controller;

import com.citi.custody.entity.ReleaseInfo;
import com.citi.custody.service.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/release")
public class ReleaseController {

    @Autowired
    private ReleaseService releaseService;

    @GetMapping("/list")
    public List<ReleaseInfo> getAllVersions() {
        return releaseService.getAllVersions();
    }
}
