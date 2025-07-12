package com.banula.navigationservice.service;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface HubClientInfoService {
  List<HubClientInfoDTO> getPaginatedHubClientInfos(LocalDateTime dateFrom, LocalDateTime dateTo, Integer offset, Integer limit);
  List<HubClientInfoDTO> getHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode);
  HubClientInfoDTO updateHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode, HubClientInfoDTO clientInfoDTO);
  void syncAllHubClientInfoParties();
}