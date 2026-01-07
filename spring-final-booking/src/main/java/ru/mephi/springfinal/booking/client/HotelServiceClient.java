package ru.mephi.springfinal.booking.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HotelServiceClient {

    private final WebClient webClient;

    @Value("${hotel-service.timeout:5000}")
    private int timeout;

    @Value("${hotel-service.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${hotel-service.retry.initial-interval:200}")
    private long initialInterval;

    @Value("${hotel-service.retry.multiplier:2}")
    private int multiplier;

    public HotelServiceClient(@Value("${hotel-service.url}") String hotelServiceUrl, WebClient.Builder webClientBuilder) {
        log.info("Initializing HotelServiceClient with URL: {}", hotelServiceUrl);
        // Создаем WebClient с baseUrl для LoadBalanced клиента
        this.webClient = webClientBuilder.baseUrl(hotelServiceUrl).build();
        log.info("HotelServiceClient initialized successfully");
    }

    /**
     * Получить JWT токен из SecurityContext
     */
    private String getAuthToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            return authentication.getCredentials().toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecommendedRooms(Long hotelId, String roomType,
                                                          LocalDate startDate, LocalDate endDate) {
        log.info("Fetching recommended rooms: hotelId={}, type={}, dates={} to {}",
                 hotelId, roomType, startDate, endDate);

        String uri = String.format("/api/rooms/recommend?startDate=%s&endDate=%s%s%s",
                startDate, endDate,
                (hotelId != null ? "&hotelId=" + hotelId : ""),
                (roomType != null ? "&roomType=" + roomType : ""));
        log.debug("Request URI: {}", uri);

        String token = getAuthToken();

        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/rooms/recommend")
                            .queryParam("startDate", startDate.toString())
                            .queryParam("endDate", endDate.toString());
                    if (hotelId != null) {
                        builder.queryParam("hotelId", hotelId);
                    }
                    if (roomType != null) {
                        builder.queryParam("roomType", roomType);
                    }
                    return builder.build();
                })
                .headers(headers -> {
                    if (token != null) {
                        headers.setBearerAuth(token);
                        log.debug("Added Authorization header to request");
                    } else {
                        log.warn("No authentication token found in SecurityContext");
                    }
                })
                .retrieve()
                .bodyToMono(List.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxAttempts, Duration.ofMillis(initialInterval))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest))
                        .doBeforeRetry(rs -> log.warn("Retrying request, attempt: {}, error: {}",
                                rs.totalRetries() + 1, rs.failure().getClass().getSimpleName())))
                .doOnError(e -> log.error("Failed to fetch recommended rooms: {} - {}",
                        e.getClass().getSimpleName(), e.getMessage(), e))
                .onErrorReturn(List.of())
                .block();
    }

    public Map<String, Object> confirmAvailability(Long roomId, String requestId, Long bookingId,
                                                    LocalDate startDate, LocalDate endDate) {
        log.info("Confirming availability: roomId={}, requestId={}, bookingId={}",
                 roomId, requestId, bookingId);


        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("bookingId", bookingId);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());

        String token = getAuthToken();

        return webClient.post()
                .uri("/api/rooms/{id}/confirm-availability", roomId)
                .headers(headers -> {
                    if (token != null) {
                        headers.setBearerAuth(token);
                    }
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxAttempts, Duration.ofMillis(initialInterval))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest) &&
                                           !(throwable instanceof WebClientResponseException.Conflict)))
                .doOnError(e -> log.error("Failed to confirm availability: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(Map.of("available", false, "message", e.getMessage())))
                .block();
    }

    public void releaseReservation(Long roomId, String requestId) {
        log.info("Releasing reservation: roomId={}, requestId={}", roomId, requestId);

        String token = getAuthToken();

        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/api/rooms/{id}/release")
                            .queryParam("requestId", requestId)
                            .build(roomId))
                    .headers(headers -> {
                        if (token != null) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(100)))
                    .block();
            log.info("Successfully released reservation for roomId={}", roomId);
        } catch (Exception e) {
            log.error("Failed to release reservation for roomId={}: {}", roomId, e.getMessage());
        }
    }
}

