package com.neighbourhub.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    // Credentials now come from environment variables, not source code.
    @Bean
    public Cloudinary cloudinary(@Value("${cloudinary.cloud-name:}") String cloudName,
                                 @Value("${cloudinary.api-key:}") String apiKey,
                                 @Value("${cloudinary.api-secret:}") String apiSecret) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    // Shared HTTP client for Groq and Gemini (replaces node-fetch / groq-sdk transport).
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        return RestClient.builder().requestFactory(factory).build();
    }
}
