package com.banula.navigationservice.service;

import com.banula.navigationservice.model.dto.HubClientInfoDTO;

import java.util.List;

public interface HubClientInfoService {

  List<HubClientInfoDTO> getAllHubClientInfos();

  HubClientInfoDTO getHubClientInfoById(String id);

  HubClientInfoDTO getHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode);

  List<HubClientInfoDTO> getHubClientInfosByPartyId(String partyId);

  List<HubClientInfoDTO> getHubClientInfosByCountryCodeAndPartyId(String countryCode, String partyId);

  List<HubClientInfoDTO> getHubClientInfosByStatus(String status);

  HubClientInfoDTO createHubClientInfo(HubClientInfoDTO hubClientInfoDTO);

  HubClientInfoDTO updateHubClientInfo(String id, HubClientInfoDTO hubClientInfoDTO);

  HubClientInfoDTO updateHubClientInfoStatus(String id, String status);

  void deleteHubClientInfo(String id);

  void deleteHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode);
}