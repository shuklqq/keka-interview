package com.ranjeet.interview.client;

import com.ranjeet.interview.entity.Task;
import com.ranjeet.interview.exception.ExternalCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Outbound calls to another service. Callers get domain-named methods; HTTP details and
 * error translation stay in here. Add one public method per downstream operation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceClient {

    private final RestClient externalRestClient;

    /** Example downstream operation. Points at this app's own /internal/echo by default. */
    public Acknowledgement notifyTaskCompleted(Task task) {
        return post("/internal/echo", task, Acknowledgement.class);
    }

    /** Generic GET. Reuse for the next downstream read; keep the public method domain-named. */
    public <T> T get(String path, Class<T> responseType) {
        log.debug("GET {}", path);
        try {
            return externalRestClient.get()
                    .uri(path)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {
            throw new ExternalCallException("GET " + path + " failed with status " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ExternalCallException("GET " + path + " unreachable: " + e.getMessage(), e);
        }
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        log.debug("POST {} body={}", path, body);
        try {
            return externalRestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {   // 4xx / 5xx from the other side
            throw new ExternalCallException("POST " + path + " failed with status " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {       // timeout, connection refused, DNS
            throw new ExternalCallException("POST " + path + " unreachable: " + e.getMessage(), e);
        }
    }
}
