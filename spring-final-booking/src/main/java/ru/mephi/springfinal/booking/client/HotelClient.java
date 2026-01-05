package ru.mephi.springfinal.booking.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class HotelClient {
    private static final Logger log = LoggerFactory.getLogger(HotelClient.class);
    private final WebClient webClient;

    public HotelClient(WebClient.Builder webClientBuilder) {
        // используем logical service id spring-final-hotel через load balancer
        this.webClient = webClientBuilder.baseUrl("http://spring-final-hotel").build();
    }

    public boolean confirmAvailability(Long hotelId, Long roomId) {
        String path = String.format("/internal/hotels/%d/rooms/%d/confirm-availability", hotelId, roomId);
        try {
            webClient.post()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity()
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200))
                            .filter(throwable -> {
                                // retry on 5xx and network errors, do not retry 4xx
                                if (throwable instanceof WebClientResponseException) {
                                    int code = ((WebClientResponseException) throwable).getStatusCode().value();
                                    return code >= 500 && code < 600;
                                }
                                return true; // network / other transient
                            })
                            .doAfterRetry(rs -> log.warn("confirmAvailability retry due to: {}", rs.failure().toString()))
                    )
                    .timeout(Duration.ofSeconds(3))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("confirmAvailability failed for hotelId={} roomId={} : {}", hotelId, roomId, e.toString());
            return false;
        }
    }

    public void release(Long hotelId, Long roomId) {
        String path = String.format("/internal/hotels/%d/rooms/%d/release", hotelId, roomId);
        try {
            webClient.post()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity()
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                            .filter(throwable -> {
                                if (throwable instanceof WebClientResponseException) {
                                    int code = ((WebClientResponseException) throwable).getStatusCode().value();
                                    return code >= 500 && code < 600;
                                }
                                return true;
                            })
                            .doAfterRetry(rs -> log.warn("release retry due to: {}", rs.failure().toString()))
                    )
                    .timeout(Duration.ofSeconds(3))
                    .block();
        } catch (Exception e) {
            log.warn("release call failed for hotelId={} roomId={} : {}", hotelId, roomId, e.toString());
        }
    }
}
