package com.banula.navigationservice.service;

import com.banula.navigationservice.model.MongoClientInfo;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.repository.HubClientInfoRepository;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class HubClientInfoServiceImpl implements HubClientInfoService {

  private final HubClientInfoRepository hubClientInfoRepository;

  @Override
  public List<HubClientInfoDTO> getAllHubClientInfos() {
    try {
      List<MongoClientInfo> hubClientInfos = hubClientInfoRepository.findAll();
      return hubClientInfos.stream()
          .map(this::mapToDTO)
          .collect(Collectors.toList());
    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching all client infos: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public HubClientInfoDTO getHubClientInfoById(String id) {
    try {
      Optional<MongoClientInfo> hubClientInfo = hubClientInfoRepository.findById(id);
      if (hubClientInfo.isPresent()) {
        return mapToDTO(hubClientInfo.get());
      } else {
        throw new OCPICustomException("Client info not found with id: " + id);
      }
    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching client info by id: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public HubClientInfoDTO getHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode) {
    try {
      Optional<MongoClientInfo> hubClientInfo = hubClientInfoRepository.findByPartyIdAndCountryCode(partyId,
          countryCode);
      if (hubClientInfo.isPresent()) {
        return mapToDTO(hubClientInfo.get());
      } else {
        throw new OCPICustomException(
            "Client info not found with partyId: " + partyId + " and countryCode: " + countryCode);
      }
    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching client info by partyId and countryCode: "
          + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public List<HubClientInfoDTO> getHubClientInfosByPartyId(String partyId) {
    try {
      List<MongoClientInfo> hubClientInfos = hubClientInfoRepository.findByPartyId(partyId);
      return hubClientInfos.stream()
          .map(this::mapToDTO)
          .collect(Collectors.toList());
    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching client infos by party id: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public List<HubClientInfoDTO> getHubClientInfosByCountryCodeAndPartyId(String countryCode, String partyId) {
    try {
      List<MongoClientInfo> hubClientInfos = hubClientInfoRepository.findByCountryCodeAndPartyId(countryCode, partyId);
      return hubClientInfos.stream()
          .map(this::mapToDTO)
          .collect(Collectors.toList());
    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching client infos by country code and party id: "
          + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public List<HubClientInfoDTO> getHubClientInfosByStatus(String status) {
    try {
      List<MongoClientInfo> hubClientInfos = hubClientInfoRepository.findByStatus(status);
      return hubClientInfos.stream()
          .map(this::mapToDTO)
          .collect(Collectors.toList());
    } catch (Exception e) {
      String errorMessage = "Error occurred while fetching client infos by status: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public HubClientInfoDTO createHubClientInfo(HubClientInfoDTO hubClientInfoDTO) {
    try {
      MongoClientInfo hubClientInfo = mapToEntity(hubClientInfoDTO);
      hubClientInfo.setCreatedAt(LocalDateTime.now());
      hubClientInfo.setUpdatedAt(LocalDateTime.now());
      hubClientInfo.setLastUpdated(LocalDateTime.now());

      MongoClientInfo savedHubClientInfo = hubClientInfoRepository.save(hubClientInfo);
      log.info("Client info created successfully with id: {}", savedHubClientInfo.getId());
      return mapToDTO(savedHubClientInfo);
    } catch (Exception e) {
      String errorMessage = "Error occurred while creating client info: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public HubClientInfoDTO updateHubClientInfo(String id, HubClientInfoDTO hubClientInfoDTO) {
    try {
      Optional<HubClientInfo> existingHubClientInfo = hubClientInfoRepository.findById(id);
      if (existingHubClientInfo.isPresent()) {
        HubClientInfo hubClientInfo = existingHubClientInfo.get();
        updateEntityFromDTO(hubClientInfo, hubClientInfoDTO);
        hubClientInfo.setUpdatedAt(LocalDateTime.now());
        hubClientInfo.setLastUpdated(LocalDateTime.now());

        HubClientInfo updatedHubClientInfo = hubClientInfoRepository.save(hubClientInfo);
        log.info("Client info updated successfully with id: {}", updatedHubClientInfo.getId());
        return mapToDTO(updatedHubClientInfo);
      } else {
        throw new OCPICustomException("Client info not found with id: " + id);
      }
    } catch (Exception e) {
      String errorMessage = "Error occurred while updating client info: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public HubClientInfoDTO updateHubClientInfoStatus(String id, String status) {
    try {
      Optional<HubClientInfo> existingHubClientInfo = hubClientInfoRepository.findById(id);
      if (existingHubClientInfo.isPresent()) {
        HubClientInfo hubClientInfo = existingHubClientInfo.get();
        hubClientInfo.setStatus(status);
        hubClientInfo.setUpdatedAt(LocalDateTime.now());
        hubClientInfo.setLastUpdated(LocalDateTime.now());

        HubClientInfo updatedHubClientInfo = hubClientInfoRepository.save(hubClientInfo);
        log.info("Client info status updated successfully with id: {}", updatedHubClientInfo.getId());
        return mapToDTO(updatedHubClientInfo);
      } else {
        throw new OCPICustomException("Client info not found with id: " + id);
      }
    } catch (Exception e) {
      String errorMessage = "Error occurred while updating client info status: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public void deleteHubClientInfo(String id) {
    try {
      Optional<HubClientInfo> hubClientInfo = hubClientInfoRepository.findById(id);
      if (hubClientInfo.isPresent()) {
        hubClientInfoRepository.deleteById(id);
        log.info("Client info deleted successfully with id: {}", id);
      } else {
        throw new OCPICustomException("Client info not found with id: " + id);
      }
    } catch (Exception e) {
      String errorMessage = "Error occurred while deleting client info: " + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  @Override
  public void deleteHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode) {
    try {
      Optional<HubClientInfo> hubClientInfo = hubClientInfoRepository.findByPartyIdAndCountryCode(partyId, countryCode);
      if (hubClientInfo.isPresent()) {
        hubClientInfoRepository.delete(hubClientInfo.get());
        log.info("Client info deleted successfully with partyId: {} and countryCode: {}", partyId, countryCode);
      } else {
        throw new OCPICustomException(
            "Client info not found with partyId: " + partyId + " and countryCode: " + countryCode);
      }
    } catch (Exception e) {
      String errorMessage = "Error occurred while deleting client info by partyId and countryCode: "
          + e.getLocalizedMessage();
      log.error(errorMessage);
      throw new OCPICustomException(errorMessage);
    }
  }

  private HubClientInfoDTO mapToDTO(HubClientInfo hubClientInfo) {
    return HubClientInfoDTO.builder()
        .id(hubClientInfo.getId())
        .partyId(hubClientInfo.getPartyId())
        .countryCode(hubClientInfo.getCountryCode())
        .role(hubClientInfo.getRole())
        .status(hubClientInfo.getStatus())
        .lastUpdated(hubClientInfo.getLastUpdated())
        .createdAt(hubClientInfo.getCreatedAt())
        .updatedAt(hubClientInfo.getUpdatedAt())
        .createdBy(hubClientInfo.getCreatedBy())
        .updatedBy(hubClientInfo.getUpdatedBy())
        .build();
  }

  private HubClientInfo mapToEntity(HubClientInfoDTO hubClientInfoDTO) {
    return HubClientInfo.builder()
        .id(hubClientInfoDTO.getId())
        .partyId(hubClientInfoDTO.getPartyId())
        .countryCode(hubClientInfoDTO.getCountryCode())
        .role(hubClientInfoDTO.getRole())
        .status(hubClientInfoDTO.getStatus())
        .lastUpdated(hubClientInfoDTO.getLastUpdated())
        .createdAt(hubClientInfoDTO.getCreatedAt())
        .updatedAt(hubClientInfoDTO.getUpdatedAt())
        .createdBy(hubClientInfoDTO.getCreatedBy())
        .updatedBy(hubClientInfoDTO.getUpdatedBy())
        .build();
  }

  private void updateEntityFromDTO(HubClientInfo hubClientInfo, HubClientInfoDTO hubClientInfoDTO) {
    hubClientInfo.setPartyId(hubClientInfoDTO.getPartyId());
    hubClientInfo.setCountryCode(hubClientInfoDTO.getCountryCode());
    hubClientInfo.setRole(hubClientInfoDTO.getRole());
    hubClientInfo.setStatus(hubClientInfoDTO.getStatus());
    hubClientInfo.setLastUpdated(hubClientInfoDTO.getLastUpdated());
    hubClientInfo.setCreatedBy(hubClientInfoDTO.getCreatedBy());
    hubClientInfo.setUpdatedBy(hubClientInfoDTO.getUpdatedBy());
  }
}