package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatsClient {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RetryTemplate retryTemplate = RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(3000L)
            .build();

    private URI getStatsServerUri(String path) {
        var instance = retryTemplate.execute(ctx ->
                discoveryClient.getInstances("stats-server")
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Stats server not found")));
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public void saveHit(EndpointHitDto dto) {
        URI uri = getStatsServerUri("/hit");
        HttpEntity<EndpointHitDto> request = new HttpEntity<>(dto, new HttpHeaders());
        try {
            restTemplate.postForEntity(uri, request, Void.class);
        } catch (Exception e) {
            log.error("Failed to save hit", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        URI uri = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique)
                .queryParam("uris", uris != null ? String.join(",", uris) : null)
                .build(true)   // включает кодирование параметров
                .toUri();

        URI fullUri = getStatsServerUri(uri.toString());
        ResponseEntity<ViewStatsDto[]> response = restTemplate.getForEntity(fullUri, ViewStatsDto[].class);
        return List.of(response.getBody());
    }
}