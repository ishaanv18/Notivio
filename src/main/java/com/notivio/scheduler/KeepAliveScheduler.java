package com.notivio.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Keep-alive scheduler to prevent free-tier hosting from sleeping.
 * Pings the /health endpoint every 5 minutes (configurable).
 *
 * Recommended for: Render, Railway, Fly.io free tiers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeepAliveScheduler {

    @Value("${scheduler.keepalive-url:http://localhost:8080/health}")
    private String keepAliveUrl;

    @Value("${scheduler.keepalive-interval-minutes:5}")
    private int intervalMinutes;

    // Every 5 minutes (default)
    @Scheduled(fixedRateString = "#{${scheduler.keepalive-interval-minutes:5} * 60 * 1000}",
               initialDelay = 60000)
    public void ping() {
        try {
            WebClient.create()
                    .get()
                    .uri(keepAliveUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            resp -> log.debug("Keep-alive ping OK: {}", keepAliveUrl),
                            err  -> log.warn("Keep-alive ping failed: {}", err.getMessage())
                    );
        } catch (Exception e) {
            log.warn("Keep-alive scheduler error: {}", e.getMessage());
        }
    }
}
