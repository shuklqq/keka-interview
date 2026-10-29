package com.ranjeet.interview.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * One shared, pre-configured outbound HTTP client.
 * Timeouts live here so no outbound call can hang the service.
 */
@Configuration
@EnableConfigurationProperties(ExternalServiceProperties.class)
public class HttpClientConfig {

    @Bean
    public RestClient externalRestClient(RestClient.Builder builder, ExternalServiceProperties props) {
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(props.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkClient);
        requestFactory.setReadTimeout(props.readTimeout());

        return builder
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
