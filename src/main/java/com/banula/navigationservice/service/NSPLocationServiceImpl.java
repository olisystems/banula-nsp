package com.banula.navigationservice.service;

import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.navigationservice.repository.SmartLocationRepository;
import com.banula.navigationservice.util.LocationUtility;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.mapper.LocationMapper;
import com.banula.openlib.ocpi.model.Location;
import com.banula.openlib.ocpi.model.dto.LocationDTO;
import com.banula.openlib.ocpi.model.vo.Connector;
import com.banula.openlib.ocpi.model.vo.EVSE;
import com.banula.openlib.ocpi.util.Constants;
import com.banula.openlib.ocpi.util.ModelPatcherUtil;
import com.banula.navigationservice.config.MongoCollectionMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class NSPLocationServiceImpl implements NSPLocationService {

    private final SmartLocationRepository smartLocationRepository;
    private final LocationUtility locationUtility;
    private final MongoCollectionMapper mongoCollectionMapper;

    @Override
    public Object getLocationConnector(String countryCode, String party_id, String locationId, String evseUid,
            String connectorId) {
        try {
            return locationUtility.findLocations(countryCode, party_id, locationId, evseUid, connectorId,
                    mongoCollectionMapper.getSmartLocationCollectionName());
        } catch (Exception e) {
            String errorMessage = "Error happened while fetching location, error message: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void putEvse(EVSE evseVO, String countryCode, String party_id, String locationId, String evseUid) {

        // get the locationDTO
        LocationDTO locationDTO = (LocationDTO) getLocationConnector(countryCode, party_id, locationId, null, null);
        if (locationDTO == null) {
            throw new OCPICustomException("Location not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        // verify if the evseUid is already registered and replace object
        EVSE currentEVSE = null;
        if (locationDTO.getEvses() == null) {
            locationDTO.setEvses(new ArrayList<>());
        } else {
            currentEVSE = (EVSE) getLocationConnector(countryCode, party_id, locationId, evseUid, null);
        }
        if (currentEVSE != null) {
            locationDTO.getEvses().removeIf(evse -> evse.getUid().equals(evseUid));
        }

        // add the new evse to the ArrayList of evses in the Location and register
        // location
        locationDTO.getEvses().add(evseVO);
        putLocation(locationDTO, countryCode, party_id, locationId);
    }

    public void patchEvse(EVSE incompleteEvse, String countryCode, String party_id, String locationId, String evseUid) {
        try {

            Optional<MongoSmartLocation> optionalMongoSmartLocation = smartLocationRepository.findByCompositeKey(
                    countryCode, party_id, locationId);

            if(optionalMongoSmartLocation.isEmpty()) {
                throw new OCPICustomException("Location not found",
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            if (getLocationConnector(countryCode, party_id, locationId, evseUid, null) == null) {
                throw new OCPICustomException("EVSE not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }


            Location existingLocation = optionalMongoSmartLocation.get();
            EVSE existingEvseToUpdate = LocationUtility.getEvseInLocation(existingLocation, evseUid);
            ModelPatcherUtil.evsePatcher(existingEvseToUpdate, incompleteEvse);

            // substitute old EVSE with new EVSE and save location
            existingLocation.getEvses().removeIf(evse -> evse.getUid().equals(evseUid));
            existingLocation.getEvses().add(existingEvseToUpdate);

            MongoSmartLocation mongoSmartLocation = optionalMongoSmartLocation.get();
            mongoSmartLocation.updateLocation(existingLocation);

            smartLocationRepository.save(mongoSmartLocation);
        } catch (Exception e) {
            String errorMessage = "Error happened while patching location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void putConnector(Connector connectorVO, String countryCode, String party_id, String locationId,
            String evseUid, String connectorId) {
        // get the locationDTO
        LocationDTO locationDTO = (LocationDTO) getLocationConnector(countryCode, party_id, locationId, null, null);
        if (locationDTO == null) {
            throw new OCPICustomException("Location not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        // get the EVSE and current index
        EVSE evse = (EVSE) getLocationConnector(countryCode, party_id, locationId, evseUid, null);
        if (evse == null) {
            throw new OCPICustomException("EVSE not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }
        int evseCurrentIndex = LocationUtility.indexOf(locationDTO.getEvses(), evse);

        // verify if the connector is already registered and replace object, otherwise
        // add it
        if (evse.getConnectors() == null) {
            evse.setConnectors(new ArrayList<>());
        }
        int currentConnectorIndex = LocationUtility.indexOf(evse.getConnectors(), connectorVO);
        if (currentConnectorIndex != -1) {
            evse.getConnectors().set(currentConnectorIndex, connectorVO);
        } else {
            evse.getConnectors().add(connectorVO);
        }

        // replace the EVSE in the locationDTO
        locationDTO.getEvses().set(evseCurrentIndex, evse);

        // push the updated locationDTO to the database
        putLocation(locationDTO, countryCode, party_id, locationId);
    }

    @Override
    public void patchConnector(Connector incompleteConnector, String countryCode, String party_id, String locationId,
            String evseUid, String connectorId) {
        try {
            // verify if location EVSE and Connector exists
            Optional<MongoSmartLocation> optionalMongoSmartLocation = smartLocationRepository.findByCompositeKey(
                    countryCode, party_id, locationId);

            if(optionalMongoSmartLocation.isEmpty()) {
                throw new OCPICustomException("Location not found",
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }
            if (getLocationConnector(countryCode, party_id, locationId, evseUid, null) == null) {
                throw new OCPICustomException("EVSE not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }
            if (getLocationConnector(countryCode, party_id, locationId, evseUid, connectorId) == null) {
                throw new OCPICustomException("Connector not found",
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            MongoSmartLocation mongoSmartLocation = optionalMongoSmartLocation.get();
            EVSE existingEvse = LocationUtility.getEvseInLocation(mongoSmartLocation, evseUid);
            Connector existingConnectorToUpdate = LocationUtility.getConnectorInEvse(existingEvse, connectorId);

            ModelPatcherUtil.connectorPatcher(existingConnectorToUpdate, incompleteConnector);

            // substitute old Connector with new Connector and save location
            existingEvse.getConnectors().removeIf(connector -> connector.getId().equals(connectorId));
            existingEvse.getConnectors().add(existingConnectorToUpdate);

            mongoSmartLocation.getEvses().removeIf(evse -> evse.getUid().equals(evseUid));
            mongoSmartLocation.getEvses().add(existingEvse);
            mongoSmartLocation.updateLocation(mongoSmartLocation);

            smartLocationRepository.save(mongoSmartLocation);
        } catch (Exception e) {
            String errorMessage = "Error happened while patching location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void putLocation(LocationDTO locationDTO, String countryCode, String partyId, String ocpiId) {
        try {
            MongoSmartLocation mongoSmartLocation = smartLocationRepository.findByCompositeKey(countryCode, partyId, ocpiId)
                    .orElse(null);
            Location location = LocationMapper.toLocationEntity(locationDTO);
            if(mongoSmartLocation == null) {
                mongoSmartLocation = new MongoSmartLocation();
            }
            mongoSmartLocation.updateLocation(location);
            smartLocationRepository.save(mongoSmartLocation);
            log.info("Location saved in database! | uid: {} | collection: {}", locationDTO.getId(),
                    mongoCollectionMapper.getSmartLocationCollectionName());
        } catch (Exception e) {
            String errorMessage = "Error happened while pushing location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void patchLocation(LocationDTO locationDTO, String countryCode, String partyId, String id) {
        try {
            Location incompleteLocation = LocationMapper.toLocationEntity(locationDTO);
            MongoSmartLocation mongoExistingLocation = smartLocationRepository.findByCompositeKey(countryCode, partyId, id)
                    .orElseThrow(RuntimeException::new);
            ModelPatcherUtil.locationPatcher(mongoExistingLocation, incompleteLocation);
            mongoExistingLocation.updateLocation(mongoExistingLocation);
            smartLocationRepository.save(mongoExistingLocation);
        } catch (Exception e) {
            String errorMessage = "Error happened while patching location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

}
