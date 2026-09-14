package com.banula.navigationservice.client;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * CDR Adapter client: the NSP is the source of truth for smart locations, the CDR
 * Adapter keeps a mirror of them. This is the only call the NSP makes into it.
 */
@Slf4j
@Component
@AllArgsConstructor
public class CdrAdapterClient {

    private static final String UPDATE_SMART_LOCATIONS_PATH = "/api/v1/internal/smart-locations/update";

    private final RestTemplate restTemplate;
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * Asks the CDR Adapter to re-pull every smart location from this service, so its
     * mirror catches up with whatever just changed here. Same endpoint the dashboard's
     * "Update From NSP" button calls.
     */
    public void updateSmartLocations() {
        String baseUrl = applicationConfiguration.getCdrAdapterUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("Skipping CDR Adapter smart location update: cdr-adapter.url is not configured");
            return;
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(UPDATE_SMART_LOCATIONS_PATH)
                .encode()
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            log.info("CDR Adapter smart location update returned {}: {}", response.getStatusCode(),
                    response.getBody());
        } catch (Exception e) {
            log.error("CDR Adapter smart location update {} failed: {}", url, e.getMessage());
            throw new OCPICustomException("CDR Adapter smart location update failed: " + e.getMessage());
        }
    }
}
