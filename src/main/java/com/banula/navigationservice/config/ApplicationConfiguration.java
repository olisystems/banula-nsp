package com.banula.navigationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.banula.openlib.ocpi.model.enums.Role;
import com.banula.openlib.ocpi.model.enums.VersionNumber;
import com.banula.openlib.ocpi.platform.PlatformConfiguration;

import lombok.Data;

@Configuration
@EnableConfigurationProperties
@Data
public class ApplicationConfiguration implements PlatformConfiguration {

    @Value("${api.url}")
    private String partyUrl;

    @Value("${api.role}")
    private Role ocpiRole;

    @Value("${api.command-timeout}")
    private Integer commandTimeout;

    @Value("${api.zone-id}")
    private String zoneId;

    @Value("${api.collection-prefix}")
    private String collectionPrefix;

    @Value("${api.log-curl-command}")
    private boolean logCurlCommand;

    @Value("${platform.url}")
    private String platformUrl;

    @Value("${remote-check.enabled:true}")
    private Boolean remoteCheckEnabled;

    @Value("${remote-check.interval:300000}")
    private Long remoteCheckInterval;

    @Value("${remote-check.timeout:10000}")
    private Long remoteCheckTimeout;

    @Override
    public VersionNumber getOcpiVersion() {
        return VersionNumber.fromValue(ocpiVersion);
    }
    
    @Value("${api.ocpi-version}")
    private String ocpiVersion;

   
    public boolean isToLogCurlCommands() {
        return logCurlCommand;
    }

}
