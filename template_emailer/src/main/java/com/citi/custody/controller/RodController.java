package com.citi.custody.controller;

import com.citi.custody.service.RodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@RestController
public class RodController {

    @Autowired
    private RodService apiService;

    @GetMapping("/fetch-data")
    public String fetchData(@RequestParam String releaseId) throws NoSuchAlgorithmException, KeyManagementException,
            IOException, InterruptedException {
        return apiService.fetchData(releaseId);
    }
}
