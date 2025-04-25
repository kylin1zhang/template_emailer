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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;

import com.citi.custody.config.RestTemplateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class RodService {

    private static final Logger logger = LoggerFactory.getLogger(RodService.class);

    @Autowired
    private RestTemplateConfig restTemplateConfig;

    public RestTemplate createRestTemplatewithTrustingHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(restTemplateConfig.createTrustingHttpClient());

        return new RestTemplate(factory);
    }

    public String fetchData(String releaseId) throws NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        // 获取当前日期并格式化
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedEndDate = currentDate.format(formatter);
        
        // 获取一个月前的日期并格式化
        LocalDate startDate = currentDate.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        String formattedStartDate = startDate.format(formatter);
        
        // 构建 URL
        String url = "https://release-on-demand-svc.ls.dyn.nsroot.net/api/external/v1/release-summaries/c1-1570l&startDate=" + formattedStartDate + "&endDate=" + formattedEndDate;
        // 示例 URL
        //url = "https://release-on-demand-svc.ls.dyn.nsroot.net/api/external/v1/release-summaries/c1-1570l&startDate=2023-01-01&endDate=2023-01-30";
        RestTemplate restTemplate = createRestTemplatewithTrustingHttpClient();

        
        // Fetch the access token
        String accessToken = getAccessToken();
        
        // Create headers
        HttpHeaders headers = new HttpHeaders();


        headers.set("Authorization", "Bearer " + accessToken);
        logger.info("accessToken: " + accessToken);
        
        // Create HTTP entity
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
            String endpointStr = "https://oidc.ls.dyn.nsroot.net/token"; // 假设为实际的 Token Endpoint
            URI endpoint = new URI(endpointStr);
            
            // Construct the client credentials grant
            AuthorizationGrant clientGrant = new ClientCredentialsGrant();
            
            
            String grantType = "client_credentials";
            String clientId = "citi-tax-suite"; // 假设为实际的 client_id
            String clientSecret = "7szHeKesiSfnInvLnhedWgjvIfa"; // 假设为实际的 client_secret
            
            // Set the client credentials
            ClientID clientID = new ClientID(clientId);
            Secret clientSecretObj = new Secret(clientSecret);
            // Client credential data is added to the POST request body
            ClientAuthentication clientAuth = new ClientSecretPost(clientID, clientSecretObj);
            // Add the required scopes
            Scope scope = new Scope("api_read_csis");
            

            // Make the token request
            TokenRequest request = new TokenRequest(endpoint, clientAuth, clientGrant, scope);
            
            // Send the request and parse the response
            TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
            logger.info("Token response received: " + response.toHTTPResponse().getContent());
            

            if (response.indicatesSuccess()) {
                // We got an error response
                TokenErrorResponse errorResponse = response.toErrorResponse();
                logger.error("Token request failed with error: " + errorResponse.getErrorObject().getDescription() + 
                          " with HTTP code: " + errorResponse.toHTTPResponse().getStatusCode());
                return null;
            } else {
                // All good!
                AccessTokenResponse successResponse = response.toSuccessResponse();
                
                
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
        // Implement JSON parsing logic to extract the access token from the response body
        // For example, using a JSON library like Jackson or Gson
        // Here is a simple example using regex (not recommended for production use)
        String token = responseBody.replaceAll(".*\"access_token\":\"([^\"]+)\".*", "$1");
        return token;
    }

    
}