package com.ranjeet.interview.controller;

import com.ranjeet.interview.dto.EchoResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Stand-in for an external system, so the outbound client can be demonstrated offline. */
@RestController
@RequestMapping("/internal/echo")
@RequiredArgsConstructor
@Slf4j
public class EchoController {

    private final Clock clock;

    @PostMapping
    public EchoResponse echo(@RequestBody Map<String, Object> body) {
        log.info("Echo received keys={}", body.keySet());
        return new EchoResponse(Instant.now(clock), body);
    }
}
