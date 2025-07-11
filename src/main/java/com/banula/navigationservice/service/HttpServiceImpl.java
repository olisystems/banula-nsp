package com.banula.navigationservice.service;

import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.model.dto.response.VersionResponseDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class HttpServiceImpl implements HttpService {

    private final RestTemplate restTemplate;

    // #TODO Add the below constanst to the library
    private static final int OCPI_INVALID_TOKEN_ERROR_CODE = 2004;

    public List<VersionResponseDTO> getVersions(String url, String authorizationToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorizationToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<VersionResponseDTO>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.info("Received successful response with data: {}", responseEntity.getBody());
                return responseEntity.getBody();
            } else {
                throw new OCPICustomException("Invalid CREDENTIALS_TOKEN_B", OCPI_INVALID_TOKEN_ERROR_CODE);
            }
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("Error while making the HTTP request: {}", ex.getMessage());
            throw new OCPICustomException("Failed to retrieve data from the server.");
        }
    }
}
