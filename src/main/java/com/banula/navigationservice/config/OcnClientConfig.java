package com.banula.navigationservice.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.banula.openlib.ocn.client.OcnClient;
import com.banula.openlib.ocn.client.OcnClientBuilder;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class OcnClientConfig {

    private final ApplicationConfiguration applicationConfiguration;

    @Bean
    public OcnClient myOcnConfig() {
        String backendUrl = applicationConfiguration.getPartyUrl() + applicationConfiguration.getApiPrefix()
                + "/2.2.1/versions";
        return new OcnClientBuilder()
                .setFrom(applicationConfiguration.getCountryCode(), applicationConfiguration.getPartyId())
                .setTo(applicationConfiguration.getCountryCode(), applicationConfiguration.getPartyId())
                .setNodeUrl(applicationConfiguration.getPlatformUrl())
                .setOcpiRoles(List.of(applicationConfiguration.getRole()))
                .setPartyBackendUrl(backendUrl)
                .build();
    }
}
