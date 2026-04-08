package com.banula.navigationservice.service;

import com.banula.openlib.mongodb.util.GenericMongoMapper;
import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.navigationservice.repository.SmartLocationRepository;
import com.banula.openlib.ocpi.custom.smartlocations.DefaultSupplier;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocation;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.custom.smartlocations.validations.SmartLocationCreateGroup;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.model.dto.LocationDTO;
import com.banula.openlib.ocpi.util.Constants;
import com.banula.openlib.ocpi.util.ModelPatcherUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class NSPSmartLocationServiceImpl implements NSPSmartLocationService {

    private final SmartLocationRepository smartLocationRepository;
    private final GenericMongoMapper genericMongoMapper;

    @Override
    public List<SmartLocationDTO> getLocationsByParty(String countryCode, String partyId) {
        List<MongoSmartLocation> locations = smartLocationRepository.findByCountryCodeAndPartyId(countryCode, partyId);
        return genericMongoMapper.mongoListToDTO(locations, SmartLocation.class, SmartLocationDTO.class);
    }

    @Override
    public SmartLocationDTO getLocation(String countryCode, String partyId, String locationId) {
        // Using the generic OcpiCommonCompoundIndex method
        MongoSmartLocation smartLocation = smartLocationRepository
                .findByCompoundIndex(countryCode, partyId, locationId)
                .orElse(null);
        return genericMongoMapper.mongoToDTO(smartLocation, SmartLocation.class, SmartLocationDTO.class);
    }

    @Override
    public SmartLocationDTO patchSmartLocation(String countryCode, String partyId, String id,
            SmartLocationDTO smartLocationDTO) {

        try {
            validateAndPopulateLocationIdentifiers(smartLocationDTO, countryCode, partyId, id);

            // update Last Updated field
            smartLocationDTO.setLastUpdated(LocalDateTime.now());

            SmartLocation incompleteEntity = genericMongoMapper.fromDTO(smartLocationDTO, SmartLocation.class);

            MongoSmartLocation existingMongoSmartLocation = smartLocationRepository
                    .findByCompoundIndex(countryCode, partyId, id)
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));

            // MongoSmartLocation extends SmartLocation, so we can cast directly
            SmartLocation existingEntity = existingMongoSmartLocation;

            // Explicitly handle the publish field if present in the DTO
            if (smartLocationDTO.getPublish() != null) {
                incompleteEntity.setPublish(smartLocationDTO.getPublish());
            }

            // Patch the existing location with the new data
            ModelPatcherUtil.smartLocationPatcher(existingEntity,
                    incompleteEntity);
            // Convert to MongoSmartLocation with smart upsert (will find and preserve
            // existing _id)
            MongoSmartLocation mongoSmartLocation = genericMongoMapper.toMongo(existingEntity,
                    MongoSmartLocation.class);
            smartLocationRepository.save(mongoSmartLocation);
            SmartLocationDTO smartLocationDTOResponse = genericMongoMapper.toDTO(existingEntity,
                    SmartLocationDTO.class);
            log.info("Patched location with ID: {}", smartLocationDTOResponse.getId());
            return smartLocationDTOResponse;

        } catch (OCPICustomException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while patching smart location", e);
        }
    }

    private void validateAndPopulateLocationIdentifiers(SmartLocationDTO locationDTO, String countryCode,
            String partyId, String locationId) {
        // Validate path variables are not null
        if (countryCode == null || countryCode.isEmpty()) {
            throw new OCPICustomException("Country code in path cannot be null or empty",
                    Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }
        if (partyId == null || partyId.isEmpty()) {
            throw new OCPICustomException("Party ID in path cannot be null or empty",
                    Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }
        if (locationId == null || locationId.isEmpty()) {
            throw new OCPICustomException("Location ID in path cannot be null or empty",
                    Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        // Set identifying fields from path variables to support PATCH without body IDs
        if (locationDTO.getId() == null) {
            locationDTO.setId(locationId);
        }
        if (locationDTO.getCountryCode() == null) {
            locationDTO.setCountryCode(countryCode);
        }
        if (locationDTO.getPartyId() == null) {
            locationDTO.setPartyId(partyId);
        }
    }

    @Override
    public SmartLocationDTO getLocationByMaloId(String maloId) {
        MongoSmartLocation smartLocation = smartLocationRepository.findByMarketLocationId(maloId)
                .orElse(null);
        return genericMongoMapper.mongoToDTO(smartLocation, SmartLocation.class, SmartLocationDTO.class);
    }

    @Override
    public List<SmartLocationDTO> getAllLocations() {
        List<MongoSmartLocation> locations = smartLocationRepository.findAll();
        return genericMongoMapper.mongoListToDTO(locations, SmartLocation.class, SmartLocationDTO.class);
    }

    @Override
    public Set<String> getPartySet() {
        List<MongoSmartLocation> locations = smartLocationRepository.findAll();
        return locations.stream()
                .filter(location -> location.getCountryCode() != null && location.getPartyId() != null)
                .map(location -> location.getCountryCode() + "/" + location.getPartyId())
                .collect(Collectors.toSet());
    }

}
