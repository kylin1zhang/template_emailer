package com.citi.custody.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;

import com.citi.custody.config.RestTemplateConfig;

import org.apache.tomcat.util.http.parser.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    public String fetchData(String releaseId) throws NoSuchAlgorithmException, KeyManagementException, IOException {
       
        String url = "https://release-on-demand-svc.ls.dyn.nsroot.net/api/v2/releases/" + releaseId + "/summaries?startDate=2025-01-01&endDate=2025-03-30";
        RestTemplate restTemplate = createRestTemplateWithTrustingHttpClient();



        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        logger.info("accessToken:" + accessToken);
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

public String getAccessToken() {
    try {
        // The token endpoint
        String EndpointStr = "https://oidc.ls.dyn.nsroot.net/token"; // 替换为实际的 Token Endpoint
        URI Endpoint = new URI(EndpointStr);

        // Construct the client credentials grant
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // 设置各个参数变量
        String grantType = "client_credentials";
        String clientId = "cit-tax-suite"; // 替换为实际的 client_id
        String clientSecret = "F32REx6I5fn1mVlc0h0D4R8g3VIfXf"; // 替换为实际的 client_secret

        // Set the client credentials
        ClientID clientID = new ClientID(clientId);
        Secret clientSecretObj = new Secret(clientSecret);
        // Client credential data is added to the POST request body
        ClientAuthentication clientAuth = new ClientSecretPost(clientID, clientSecretObj);
        // Add the required scopes
        Scope scope = new Scope("api_read_cssis");


        // Make the token request
        TokenRequest request = new TokenRequest(Endpoint, clientAuth, clientGrant, scope);

        // Send the request and parse the response
        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
        logger.info("Token response received: " + response.toHTTPResponse().getContent());


        if (response.indicatesSuccess()) {
            // We got an error response
            TokenErrorResponse errorResponse = response.toErrorResponse();
            logger.error("Error: " + errorResponse.toHTTPResponse().getContent() + 
                         " with HTTP Code: " + errorResponse.toHTTPResponse().getStatusCode());
            return null;
        } else {
            // All good!
            AccessTokenResponse successResponse = response.toSuccessResponse();

            // Get the access token
            AccessToken accessToken = successResponse.getTokens().getAccessToken();
            logger.info("Success. JWT token is: " + accessToken.toString());
            return accessToken.toString();
        }
    } catch (Exception e) {
        logger.error("Error while fetching access token", e);
        return null;
    }
}

    private static String parseAccessToken(String responseBody) {
        String token = responseBody.replaceAll("\"access_token\":\"([^\"]+)\"","$1");
        return token;
    }
}
