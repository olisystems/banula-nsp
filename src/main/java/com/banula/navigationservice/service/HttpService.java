package com.banula.navigationservice.service;

import com.banula.openlib.ocpi.model.dto.response.VersionResponseDTO;

import java.util.List;

public interface HttpService {

    List<VersionResponseDTO> getVersions(String url, String authorizationToken);
}
