package com.citi.custody.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import com.citi.custody.config.RestTemplateConfig;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


@Service
public class RodService {

    private static final Logger logger = LoggerFactory.getLogger(RodService.class);

    @Autowired
    private RestTemplateConfig restTemplateConfig;

    public RestTemplate createRestTemplateWithTrustingHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(restTemplateConfig.createTrustingHttpClient());
        
        return new RestTemplate(factory);
    }

    public String fetchData() throws NoSuchAlgorithmException, KeyManagementException {
        String url = "https://release-on-demand-svs.dyn.mrsot.net/api/v2/releases/RELEASE:168501:ABAC4378-C7FC-40E8-A7CC-28736C9BA25";
        RestTemplate restTemplate = createRestTemplateWithTrustingHttpClient();


        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJXU1ZkdTVuMk5sZE9NeF9hUjExNk0iLCJzdWIiOiJHZkFNSjFSKjh4REpHcG9ZYmRjIiwic3ViX2RhdGEiOnsiY2lkIjoiY2lkXzI1NiJ9fQ.MkMNNqRdxV6dNE8IsQxSzZXeaHwGH5-V7kQTfCkYoT2g1D74khQHckzm4CeiTVGqaG17dyOda08-V9yflhdYgT3GGjy7f3FMK1fT5eFIv6sOk2N4RLOhdmY04APbo1bZb73HqaXkOrbwpvm6AM1Jq3kY8x4iXa7YQZgeuOHhHpt4E6UJIZ0cC8Knct91VmZjFdMG77Q==");


        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println(response.getBody());
            return response.getBody();

        } catch (Exception e) {
            System.err.println("There has been a problem with your fetch operation: " + e.getMessage());
            return null;
        }
    }
}
