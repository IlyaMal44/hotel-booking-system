package com.example.booking.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class HotelClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder hotelRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean(name = "hotelRestClient")
    public RestClient hotelRestClient(@LoadBalanced RestClient.Builder loadBalancedBuilder,
                                      @Value("${app.hotel.direct-base-url:}") String directBaseUrl,
                                      @Value("${app.hotel.connect-timeout:1s}") Duration connectTimeout,
                                      @Value("${app.hotel.read-timeout:2s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        if (directBaseUrl == null || directBaseUrl.isBlank()) {
            return loadBalancedBuilder.baseUrl("http://hotel-service").requestFactory(factory).build();
        }
        String baseUrl = directBaseUrl.endsWith("/") ? directBaseUrl.substring(0, directBaseUrl.length() - 1) : directBaseUrl;
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Bean(name = "hotelCallExecutor", destroyMethod = "shutdown")
    public ExecutorService hotelCallExecutor() {
        return Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable, "hotel-client");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    public RegistryEventConsumer<Retry> hotelRetryLogger() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> event) {
                event.getAddedEntry().getEventPublisher().onRetry(retryEvent ->
                        log.warn("hotel remote retry attempt={} waitMs={} error={}",
                                retryEvent.getNumberOfRetryAttempts(),
                                retryEvent.getWaitInterval().toMillis(),
                                retryEvent.getLastThrowable().toString()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> event) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> event) {
            }
        };
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> hotelCircuitLogger() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                event.getAddedEntry().getEventPublisher().onStateTransition(transition ->
                        log.warn("hotel circuit breaker transition={}", transition.getStateTransition()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
            }
        };
    }
}
