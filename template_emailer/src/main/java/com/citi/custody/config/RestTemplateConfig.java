package com.citi.custody.config;


import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;


@Configuration
public class RestTemplateConfig {

    @Bean
    public CloseableHttpClient createTrustingHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (chain, authType) -> true) // Trust all certificates
                .build();
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        
        return HttpClients.custom()
            .setSSLSocketFactory(sslsf)
            .build();

    }

}