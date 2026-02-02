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

    @Value("${api.url}")
    private String partyUrl;

    @Value("${api.role}")
    private Role role;

    @Value("${api.command-timeout}")
    private Integer commandTimeout;

    @Value("${api.zone-id}")
    private String zoneId;

    @Value("${api.collection-prefix}")
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
