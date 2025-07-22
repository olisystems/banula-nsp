package com.banula.navigationservice.service;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.config.MongoCollectionMapper;
import com.banula.navigationservice.mapper.ClientInfoMapper;
import com.banula.navigationservice.model.MongoClientInfo;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.repository.HubClientInfoRepository;
import com.banula.openlib.ocn.client.OcnClient;
import com.banula.openlib.ocn.client.OcnEndpoints;
import com.banula.openlib.ocpi.exception.OCPICustomException;

import com.banula.openlib.ocpi.model.OcpiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;

@Slf4j
@Service
@AllArgsConstructor
public class HubClientInfoServiceImpl implements HubClientInfoService {

  private final HubClientInfoRepository hubClientInfoRepository;
  private final OcnClient ocnClient;
  private final MongoTemplate mongoTemplate;
  private final ApplicationConfiguration applicationConfiguration;
  private final MongoCollectionMapper mongoCollectionMapper;

  @Override
  public List<HubClientInfoDTO> getPaginatedHubClientInfos(LocalDateTime dateFrom, LocalDateTime dateTo, Integer offset,
      Integer limit) {
    try {
      // Create query with date filtering
      Query query = createQueryForHubClientInfo(dateFrom, dateTo);
      query.skip(offset != null ? offset : 0);
      query.limit(limit != null ? limit : Integer.MAX_VALUE);
      query.with(Sort.by(Sort.Direction.DESC, "lastUpdated"));

      // Execute query using MongoTemplate
      List<MongoClientInfo> hubClientInfos = mongoTemplate.find(query, MongoClientInfo.class,
          mongoCollectionMapper.getHubClientInfoCollectionName());

      return hubClientInfos.stream()
          .map(ClientInfoMapper::toHubClientInfoDTO)
          .collect(Collectors.toList());

    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching paginated hub client infos: " + e.getLocalizedMessage();
      log.error(errorMessage, e);
      throw new OCPICustomException(errorMessage);
    }
  }

  /**
   * Create MongoDB query for hub client info with date filtering
   *
   * @param dateFrom Start date for filtering (can be null)
   * @param dateTo   End date for filtering (can be null)
   * @return MongoDB Query object
   */
  private Query createQueryForHubClientInfo(LocalDateTime dateFrom, LocalDateTime dateTo) {
    Query query = new Query();
    Criteria criteria = new Criteria();
    if (dateFrom != null && dateTo != null) {
      criteria = Criteria.where("createdAt").gte(dateFrom).lte(dateTo);
    } else if (dateFrom != null) {
      criteria = Criteria.where("createdAt").gte(dateFrom);
    } else if (dateTo != null) {
      criteria = Criteria.where("createdAt").lte(dateTo);
    }

    query.addCriteria(criteria);
    return query;
  }

  @Override
  public List<HubClientInfoDTO> getHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode) {
    return hubClientInfoRepository.findByPartyIdAndCountryCode(partyId, countryCode).stream()
        .map(ClientInfoMapper::toHubClientInfoDTO).collect(Collectors.toList());
  }

  @Override
  public HubClientInfoDTO updateHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode,
      HubClientInfoDTO clientInfoDTO) {
    MongoClientInfo mongoClientInfo = hubClientInfoRepository
        .findByPartyIdAndCountryCodeAndRole(partyId, countryCode, clientInfoDTO.getRole()).orElse(null);
    if (mongoClientInfo == null) {
      mongoClientInfo = new MongoClientInfo();
      mongoClientInfo.setPartyId(partyId);
      mongoClientInfo.setCountryCode(countryCode);
      mongoClientInfo.setRole(clientInfoDTO.getRole());
    }
    mongoClientInfo.setStatus(clientInfoDTO.getStatus());
    mongoClientInfo.setLastUpdated(LocalDateTime.now());
    return ClientInfoMapper.toHubClientInfoDTO(hubClientInfoRepository.save(mongoClientInfo));
  }

  public HubClientInfoDTO updateHubClientInfo(HubClientInfoDTO clientInfoDTO) {
    MongoClientInfo mongoClientInfo = hubClientInfoRepository.findByPartyIdAndCountryCodeAndRole(
        clientInfoDTO.getPartyId(), clientInfoDTO.getCountryCode(), clientInfoDTO.getRole()).orElse(null);
    if (mongoClientInfo == null) {
      mongoClientInfo = new MongoClientInfo();
      mongoClientInfo.setPartyId(clientInfoDTO.getPartyId());
      mongoClientInfo.setCountryCode(clientInfoDTO.getCountryCode());
      mongoClientInfo.setRole(clientInfoDTO.getRole());
    }
    mongoClientInfo.setStatus(clientInfoDTO.getStatus());
    mongoClientInfo.setLastUpdated(LocalDateTime.now());
    return ClientInfoMapper.toHubClientInfoDTO(hubClientInfoRepository.save(mongoClientInfo));
  }

  @Override
  public void syncAllHubClientInfoParties() {
    String outflowUrl = applicationConfiguration.getPlatformUrl() + "/ocpi/outflow/ocpi/2.2/hubclientinfo";
    try {
      OcpiResponse<List<HubClientInfoDTO>> hubClientInfoParties = ocnClient.executeOcpiOperation(
          outflowUrl,
          null,
          "CH",
          "OCN",
          new ParameterizedTypeReference<>() {
          },
          HttpMethod.GET,
          List.of());

      if (hubClientInfoParties.getStatus_code() > 2000) {
        throw new Exception(hubClientInfoParties.getStatus_message());
      }

      for (HubClientInfoDTO hubClientInfo : hubClientInfoParties.getData()) {
        this.updateHubClientInfo(hubClientInfo);
      }

    } catch (Exception ex) {
      log.warn("Initial HubClientInfo sync failed, NSP will start creating the list dynamically "
          + ex.getLocalizedMessage());
    }

  }

  @Override
  public List<HubClientInfoDTO> getHubClientInfosByStatus(List<ConnectionStatus> statuses) {
    try {
      Query query = new Query();
      query.addCriteria(Criteria.where("status").in(statuses));
      query.with(Sort.by(Sort.Direction.DESC, "lastUpdated"));

      List<MongoClientInfo> hubClientInfos = mongoTemplate.find(query, MongoClientInfo.class,
          mongoCollectionMapper.getHubClientInfoCollectionName());

      return hubClientInfos.stream()
          .map(ClientInfoMapper::toHubClientInfoDTO)
          .collect(Collectors.toList());

    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching hub client infos by status: " + e.getLocalizedMessage();
      log.error(errorMessage, e);
      throw new OCPICustomException(errorMessage);
    }
  }
}