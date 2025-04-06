package com.example.demo.Service;

import com.example.demo.Util.IpAddressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
public class GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);

    private final WebClient webClient;
    private final IpAddressUtil ipAddressUtil;

    @Value("${geoip.api.urlTemplate}")
    private String apiUrlTemplate;

    @Value("${geoip.api.timeoutSeconds:5}")
    private long apiTimeoutSeconds;

    @Autowired
    public GeoIpService(WebClient.Builder webClientBuilder, IpAddressUtil ipAddressUtil) {
        this.webClient = webClientBuilder.build();
        this.ipAddressUtil = ipAddressUtil;
    }

    public Optional<String> getCountryFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank() || "Unknown".equalsIgnoreCase(ipAddress)) {
            log.debug("Skipping GeoIP lookup for address: {}", ipAddress);
            return Optional.of("Unknown");
        }

        if (ipAddressUtil.isLocalhost(ipAddress)) {
            log.debug("Skipping GeoIP lookup for localhost address: {}", ipAddress);
            return Optional.of("Localhost");
        }

        String requestUrl = apiUrlTemplate.replace("{ip}", ipAddress);
        log.debug("Requesting GeoIP info from: {}", requestUrl);

        try {
            Optional<JsonNode> responseBodyOpt = webClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(apiTimeoutSeconds))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.warn("api.country.is request for {} failed with status {}: {}", ipAddress, ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.empty();
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("Error calling api.country.is service for {}: {}", ipAddress, ex.getMessage());
                        return Mono.empty();
                    })
                    .blockOptional();

            if (responseBodyOpt.isPresent()) {
                JsonNode body = responseBodyOpt.get();
                if (body.hasNonNull("country") && body.get("country").isTextual()) {
                    String countryCode = body.get("country").asText();
                    if (!countryCode.isBlank()) {
                        log.debug("Resolved country code '{}' for IP {}", countryCode, ipAddress);
                        return Optional.of(countryCode);
                    } else {
                        log.warn("api.country.is response for {} had blank 'country' field.", ipAddress);
                    }
                } else {
                    log.warn("api.country.is response for {} missing or invalid 'country' field: {}", ipAddress, body);
                }
            }
            return Optional.empty();

        } catch (IllegalStateException e) {
            log.error("Error processing api.country.is response (likely due to prior error): {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during api.country.is lookup for {}: {}", ipAddress, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
