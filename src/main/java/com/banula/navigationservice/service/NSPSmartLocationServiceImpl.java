package com.banula.navigationservice.service;

import com.banula.navigationservice.mapper.SmartLocationMapper;
import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.navigationservice.config.MongoCollectionMapper;
import com.banula.navigationservice.repository.SmartLocationRepository;
import com.banula.openlib.ocpi.custom.smartlocations.DefaultSupplier;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.custom.smartlocations.validations.SmartLocationCreateGroup;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class NSPSmartLocationServiceImpl implements NSPSmartLocationService {

    private final SmartLocationRepository smartLocationRepository;
    private final MongoCollectionMapper mongoCollectionMapper;

    @Override
    public List<SmartLocationDTO> getLocationsByParty(String countryCode, String partyId) {
        List<MongoSmartLocation> locations = smartLocationRepository.findByCountryCodeAndPartyId(countryCode, partyId);
        return SmartLocationMapper.toListSmartLocationDTO(locations);
    }

    @Override
    public SmartLocationDTO getLocation(String countryCode, String partyId, String locationId) {
        MongoSmartLocation smartLocation = smartLocationRepository.findByCompositeKey(countryCode, partyId, locationId)
                .orElseThrow(RuntimeException::new);
        return SmartLocationMapper.toSmartLocationDTO(smartLocation);
    }

    @Override
    public void saveSmartLocation(@Validated(SmartLocationCreateGroup.class) String id, String countryCode,
            String partyId, SmartLocationDTO smartLocationDTO) {
        // Check if location already exists
        MongoSmartLocation existingLocation = null;
        if (id != null) {
            existingLocation = smartLocationRepository.findByCompositeKey(countryCode, partyId, id).orElse(null);
        }

        if (existingLocation != null) {
            // Update only the fields that are provided in the DTO
            if (smartLocationDTO.getMarketLocationId() != null) {
                existingLocation.setMarketLocationId(smartLocationDTO.getMarketLocationId());
            }
            if (smartLocationDTO.getMeteringLocationId() != null) {
                existingLocation.setMeteringLocationId(smartLocationDTO.getMeteringLocationId());
            }
            if (smartLocationDTO.getDsoMarketPartnerId() != null) {
                existingLocation.setDsoMarketPartnerId(smartLocationDTO.getDsoMarketPartnerId());
            }
            if (smartLocationDTO.getTsoMarketPartnerId() != null) {
                existingLocation.setTsoMarketPartnerId(smartLocationDTO.getTsoMarketPartnerId());
            }
            if (smartLocationDTO.getMpoMarketPartnerId() != null) {
                existingLocation.setMpoMarketPartnerId(smartLocationDTO.getMpoMarketPartnerId());
            }
            if (smartLocationDTO.getMeteringDataSource() != null) {
                existingLocation.setMeteringDataSource(smartLocationDTO.getMeteringDataSource());
            }
            if (smartLocationDTO.getSmartMeterId() != null) {
                existingLocation.setSmartMeterId(smartLocationDTO.getSmartMeterId());
            }
            if (smartLocationDTO.getMessageQueueUrl() != null) {
                existingLocation.setMessageQueueUrl(smartLocationDTO.getMessageQueueUrl());
            }
            if (smartLocationDTO.getPublish() != null) {
                existingLocation.setPublish(smartLocationDTO.getPublish());
            }
            // Handle default supplier
            if (smartLocationDTO.getDefaultSupplier() != null) {
                // Create new DefaultSupplier instance if it doesn't exist
                if (existingLocation.getDefaultSupplier() == null) {
                    existingLocation.setDefaultSupplier(
                            DefaultSupplier.builder()
                                    .supplierMarketPartnerId(
                                            smartLocationDTO.getDefaultSupplier().getSupplierMarketPartnerId())
                                    .bkvId(smartLocationDTO.getDefaultSupplier().getBkvId())
                                    .balancingGroupEicId(smartLocationDTO.getDefaultSupplier().getBalancingGroupEicId())
                                    .build());
                } else {
                    // Update existing DefaultSupplier fields
                    if (smartLocationDTO.getDefaultSupplier().getSupplierMarketPartnerId() != null) {
                        existingLocation.getDefaultSupplier().setSupplierMarketPartnerId(
                                smartLocationDTO.getDefaultSupplier().getSupplierMarketPartnerId());
                    }
                    if (smartLocationDTO.getDefaultSupplier().getBkvId() != null) {
                        existingLocation.getDefaultSupplier().setBkvId(
                                smartLocationDTO.getDefaultSupplier().getBkvId());
                    }
                    if (smartLocationDTO.getDefaultSupplier().getBalancingGroupEicId() != null) {
                        existingLocation.getDefaultSupplier().setBalancingGroupEicId(
                                smartLocationDTO.getDefaultSupplier().getBalancingGroupEicId());
                    }
                }
            }

            // Always ensure countryCode and partyId are set
            existingLocation.setCountryCode(countryCode);
            existingLocation.setPartyId(partyId);

            // Save the updated entity
            smartLocationRepository.save(existingLocation);
            log.info("Updated existing location with ID: {}", existingLocation.getId());
        }
    }

    @Override
    public SmartLocationDTO getLocationByMaloId(String maloId) {
        MongoSmartLocation smartLocation = smartLocationRepository.findByMarketLocationId(maloId)
                .orElseThrow(RuntimeException::new);
        return SmartLocationMapper.toSmartLocationDTO(smartLocation);
    }

}
