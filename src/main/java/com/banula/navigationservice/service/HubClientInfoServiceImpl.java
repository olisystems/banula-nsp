package com.banula.navigationservice.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.config.MongoCollectionMapper;
import com.banula.navigationservice.event.PartyConnectedEvent;
import com.banula.navigationservice.mapper.ClientInfoMapper;
import com.banula.navigationservice.model.MongoClientInfo;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.repository.HubClientInfoRepository;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.model.OcpiResponse;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;
import com.banula.openlib.ocpi.model.enums.InterfaceRole;
import com.banula.openlib.ocpi.model.enums.ModuleID;
import com.banula.openlib.ocpi.model.enums.Role;
import com.banula.openlib.ocpi.platform.PlatformClient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class HubClientInfoServiceImpl implements HubClientInfoService {

  private final HubClientInfoRepository hubClientInfoRepository;
  private final PlatformClient platformClient;
  private final MongoTemplate mongoTemplate;
  private final ApplicationConfiguration applicationConfiguration;
  private final MongoCollectionMapper mongoCollectionMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public List<HubClientInfoDTO> getPaginatedHubClientInfos(LocalDateTime dateFrom, LocalDateTime dateTo, Integer offset,
      Integer limit) {
    try {
      // Create query with date filtering
      Query query = createQueryForHubClientInfo(dateFrom, dateTo);
      query.skip(offset != null ? offset : 0);
      query.limit(limit != null ? limit : Integer.MAX_VALUE);
      // Connected parties first. The status is persisted as its enum name and the OCPI
      // ConnectionStatus set is closed - CONNECTED, OFFLINE, PLANNED, SUSPENDED - so ascending
      // order already puts the usable parties on top. Sorting here rather than after the fact
      // keeps it correct across pages, since the database applies it before skip/limit.
      query.with(Sort.by(Sort.Order.asc("status"), Sort.Order.desc("lastUpdated")));

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
    // Filter on lastUpdated: it is the only timestamp these documents carry, and OCPI defines
    // date_from/date_to over last_updated. Filtering on createdAt matched nothing at all.
    if (dateFrom != null && dateTo != null) {
      criteria = Criteria.where("lastUpdated").gte(dateFrom).lte(dateTo);
    } else if (dateFrom != null) {
      criteria = Criteria.where("lastUpdated").gte(dateFrom);
    } else if (dateTo != null) {
      criteria = Criteria.where("lastUpdated").lte(dateTo);
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
    return upsertHubClientInfo(partyId, countryCode, clientInfoDTO);
  }

  public HubClientInfoDTO updateHubClientInfo(HubClientInfoDTO clientInfoDTO) {
    return upsertHubClientInfo(clientInfoDTO.getPartyId(), clientInfoDTO.getCountryCode(), clientInfoDTO);
  }

  private HubClientInfoDTO upsertHubClientInfo(String partyId, String countryCode, HubClientInfoDTO clientInfoDTO) {
    MongoClientInfo mongoClientInfo = findExistingClientInfo(partyId, countryCode, clientInfoDTO.getRole());
    ConnectionStatus previousStatus = mongoClientInfo != null ? mongoClientInfo.getStatus() : null;
    if (mongoClientInfo == null) {
      mongoClientInfo = new MongoClientInfo();
      mongoClientInfo.setPartyId(partyId);
      mongoClientInfo.setCountryCode(countryCode);
      mongoClientInfo.setRole(clientInfoDTO.getRole());
    }
    mongoClientInfo.setStatus(clientInfoDTO.getStatus());
    mongoClientInfo.setLastUpdated(LocalDateTime.now(ZoneOffset.UTC));
    HubClientInfoDTO saved = ClientInfoMapper.toHubClientInfoDTO(hubClientInfoRepository.save(mongoClientInfo));
    publishConnectedIfTransition(previousStatus, saved);
    return saved;
  }

  /**
   * Most recently updated document for this party/role, or null when there is none.
   *
   * <p>
   * The collection carries no unique index on party/country/role, so a write race can leave more
   * than one document for the same key. Reading those through an Optional-returning finder throws,
   * and because callers log-and-continue that made every later update for the affected party fail
   * silently - freezing its status. Taking the newest of the duplicates keeps the party updatable.
   */
  private MongoClientInfo findExistingClientInfo(String partyId, String countryCode, Role role) {
    List<MongoClientInfo> matches = hubClientInfoRepository
        .findByPartyIdAndCountryCodeAndRoleOrderByLastUpdatedDesc(partyId, countryCode, role);

    if (matches.isEmpty()) {
      return null;
    }
    if (matches.size() > 1) {
      log.warn("Found {} duplicate HubClientInfo documents for {}/{} ({}); updating the most recent one. "
          + "The redundant documents should be cleaned up.", matches.size(), countryCode, partyId, role);
    }
    return matches.get(0);
  }

  private void publishConnectedIfTransition(ConnectionStatus previousStatus, HubClientInfoDTO saved) {
    if (saved.getStatus() != ConnectionStatus.CONNECTED) {
      return;
    }
    if (previousStatus == ConnectionStatus.CONNECTED) {
      return;
    }
    log.info("Party {}/{} ({}) became CONNECTED; publishing welcome event", saved.getCountryCode(),
        saved.getPartyId(), saved.getRole());
    eventPublisher.publishEvent(new PartyConnectedEvent(this, saved));
  }

  @Override
  public void syncAllHubClientInfoParties() {
    try {
      pullHubClientInfoFromHub();
    } catch (Exception ex) {
      log.warn("Initial HubClientInfo sync failed, NSP will start creating the list dynamically "
          + ex.getLocalizedMessage());
    }
  }

  /**
   * Pull the party list from the hub and apply every record, returning how many were applied.
   *
   * <p>
   * Unlike {@link #syncAllHubClientInfoParties()}, which is best-effort because it runs at startup,
   * this surfaces failures to the caller so an on-demand sync can report what actually went wrong
   * instead of always reporting success.
   */
  @Override
  public int pullHubClientInfoFromHub() {
    String hubCountryCode = applicationConfiguration.getPlatformCountryCode();
    String hubPartyId = applicationConfiguration.getPlatformPartyId();
    if (hubCountryCode == null || hubCountryCode.isBlank() || hubPartyId == null || hubPartyId.isBlank()) {
      throw new OCPICustomException(
          "Cannot sync HubClientInfo: platform.country-code / platform.party-id are not configured");
    }

    OcpiResponse<List<HubClientInfoDTO>> hubClientInfoParties;
    try {
      hubClientInfoParties = platformClient.sendOutflowRequest(
          applicationConfiguration.getPlatformTenantId(),
          hubCountryCode,
          hubPartyId,
          InterfaceRole.SENDER,
          ModuleID.HUB_CLIENT_INFO,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<OcpiResponse<List<HubClientInfoDTO>>>() {
          },
          List.of(),
          Map.of());
    } catch (Exception ex) {
      throw new OCPICustomException(
          "Could not reach the hub to sync HubClientInfo: " + ex.getLocalizedMessage());
    }

    if (hubClientInfoParties == null) {
      throw new OCPICustomException("The hub returned an empty HubClientInfo response");
    }
    // Only the OCPI 1xxx range is a success; 2xxx (client) and 3xxx (server) codes are errors,
    // so 2000 must not slip through and be reported as an empty-but-successful sync.
    int statusCode = hubClientInfoParties.getStatus_code();
    if (statusCode < 1000 || statusCode >= 2000) {
      throw new OCPICustomException(
          "The hub rejected the HubClientInfo request: " + hubClientInfoParties.getStatus_message());
    }

    List<HubClientInfoDTO> parties = hubClientInfoParties.getData();
    if (parties == null || parties.isEmpty()) {
      log.info("Hub returned no HubClientInfo parties; nothing to apply");
      return 0;
    }

    for (HubClientInfoDTO hubClientInfo : parties) {
      this.updateHubClientInfo(hubClientInfo);
    }
    log.info("Applied {} HubClientInfo parties pulled from hub {}/{}", parties.size(), hubCountryCode, hubPartyId);
    return parties.size();
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
