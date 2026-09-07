package com.banula.navigationservice.service;

import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface HubClientInfoService {
  List<HubClientInfoDTO> getPaginatedHubClientInfos(LocalDateTime dateFrom, LocalDateTime dateTo, Integer offset,
      Integer limit);

  List<HubClientInfoDTO> getHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode);

  HubClientInfoDTO updateHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode,
      HubClientInfoDTO clientInfoDTO);

  void syncAllHubClientInfoParties();

  /**
   * Pull the party list from the hub and apply it, returning how many records were applied. Throws
   * when the hub cannot be reached or rejects the request, so on-demand callers can report it.
   */
  int pullHubClientInfoFromHub();

  List<HubClientInfoDTO> getHubClientInfosByStatus(List<ConnectionStatus> statuses);
}