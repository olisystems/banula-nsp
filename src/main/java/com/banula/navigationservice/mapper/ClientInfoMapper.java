package com.banula.navigationservice.mapper;

import com.banula.navigationservice.model.MongoClientInfo;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;

public class ClientInfoMapper {

  public static HubClientInfoDTO toHubClientInfoDTO(MongoClientInfo mongoClientInfo) {
    return HubClientInfoDTO.builder()
        .id(mongoClientInfo.getMongoId())
        .partyId(mongoClientInfo.getPartyId())
        .countryCode(mongoClientInfo.getCountryCode())
        .status(mongoClientInfo.getStatus())
        .role(mongoClientInfo.getRole())
        .lastUpdated(mongoClientInfo.getLastUpdated())
        .build();
  }

  public static MongoClientInfo toMongoClientInfo(HubClientInfoDTO hubClientInfoDTO) {
    return MongoClientInfo.builder()
        .mongoId(hubClientInfoDTO.getId())
        .partyId(hubClientInfoDTO.getPartyId())
        .countryCode(hubClientInfoDTO.getCountryCode())
        .status(hubClientInfoDTO.getStatus())
        .role(hubClientInfoDTO.getRole())
        .lastUpdated(hubClientInfoDTO.getLastUpdated())
        .build();
  }

}
