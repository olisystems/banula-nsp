package com.banula.navigationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

import com.banula.openlib.ocn.model.OcnVersionDetails;
import com.banula.openlib.ocpi.model.enums.Role;
import com.banula.openlib.ocpi.platform.PlatformConfiguration;

@Configuration
@EnableConfigurationProperties
@Getter
@Setter
public class ApplicationConfiguration implements PlatformConfiguration {

    @Value("${party.url}")
    private String partyUrl;

    @Value("${party.role}")
    private Role role;

    @Value("${party.api-prefix}")
    private String apiPrefix;

    @Value("${party.api-non-ocpi-prefix}")
    private String apiNonOcpiPrefix;

    @Value("${party.command-timeout}")
    private Integer commandTimeout;

    @Value("${party.zone-id}")
    private String zoneId;

    @Value("${party.collection-prefix}")
    private String collectionPrefix;

    @Value("${platform.url}")
    private String platformUrl;

    @Value("${remote-check.enabled:true}")
    private Boolean remoteCheckEnabled;

    @Value("${remote-check.interval:300000}")
    private Long remoteCheckInterval;

    @Value("${remote-check.timeout:10000}")
    private Long remoteCheckTimeout;

    private HashMap<String, OcnVersionDetails> ocnVersionDetails;

    @Override
    public void setOcnVersionDetails(String tenantId, OcnVersionDetails _ocnVersionDetails) {
        if (this.ocnVersionDetails == null) {
            this.ocnVersionDetails = new HashMap<String, OcnVersionDetails>();
        }
        this.ocnVersionDetails.put(tenantId, _ocnVersionDetails);
    }

}
