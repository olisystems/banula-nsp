package com.banula.navigationservice.service;

import com.banula.navigationservice.mapper.SmartLocationMapper;
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

@Slf4j
@Service
@AllArgsConstructor
public class NSPSmartLocationServiceImpl implements NSPSmartLocationService {

    private final SmartLocationRepository smartLocationRepository;

    @Override
    public List<SmartLocationDTO> getLocationsByParty(String countryCode, String partyId) {
        List<MongoSmartLocation> locations = smartLocationRepository.findByCountryCodeAndPartyId(countryCode, partyId);
        return SmartLocationMapper.toListSmartLocationDTOFromMongoList(locations);
    }

    @Override
    public SmartLocationDTO getLocation(String countryCode, String partyId, String locationId) {
        MongoSmartLocation smartLocation = smartLocationRepository.findByCompositeKey(countryCode, partyId, locationId)
                .orElse(null);
        return SmartLocationMapper.toSmartLocationDTO(smartLocation);
    }

    @Override
    public SmartLocationDTO patchSmartLocation(String countryCode, String partyId, String id,
            SmartLocationDTO smartLocationDTO) {

        try {
            validateAndPopulateLocationIdentifiers(smartLocationDTO, countryCode, partyId, id);

            // update Last Updated field
            smartLocationDTO.setLastUpdated(LocalDateTime.now());

            SmartLocation incompleteEntity = SmartLocationMapper.toSmartLocationEntity(smartLocationDTO);

            MongoSmartLocation existingMongoSmartLocation = smartLocationRepository
                    .findByIdAndCountryCodeAndPartyId(id, countryCode, partyId)
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));

            SmartLocation existingEntity = SmartLocationMapper.toSmartLocationEntity(
                    SmartLocationMapper.toSmartLocationDTO(existingMongoSmartLocation));
            // Patch the existing location with the new data
            ModelPatcherUtil.smartLocationPatcher(existingEntity,
                    incompleteEntity);
            MongoSmartLocation mongoSmartLocation = SmartLocationMapper.toMongoSmartLocation(existingEntity);
            smartLocationRepository.save(mongoSmartLocation);
            SmartLocationDTO smartLocationDTOResponse = SmartLocationMapper.toSmartLocationDTO(existingEntity);
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
        return SmartLocationMapper.toSmartLocationDTO(smartLocation);
    }

    @Override
    public List<SmartLocationDTO> getAllLocations() {
        List<MongoSmartLocation> locations = smartLocationRepository.findAll();
        return SmartLocationMapper.toListSmartLocationDTOFromMongoList(locations);
    }

}
