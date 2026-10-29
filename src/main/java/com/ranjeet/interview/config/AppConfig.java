package com.ranjeet.interview.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Single source of "now". Services call Instant.now(clock) so tests can pin time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
