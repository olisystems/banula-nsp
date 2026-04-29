package com.banula.navigationservice.service;

import com.banula.openlib.mongodb.util.GenericMongoMapper;
import com.banula.navigationservice.dto.BulkImportResultDTO;
import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.navigationservice.repository.SmartLocationRepository;
import com.banula.openlib.ocpi.custom.smartlocations.DefaultSupplier;
import com.banula.openlib.ocpi.custom.smartlocations.MeteringDataSource;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocationState;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocation;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.util.Constants;
import com.banula.openlib.ocpi.util.ModelPatcherUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
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
            smartLocationDTO.setLastUpdated(LocalDateTime.now(ZoneOffset.UTC));

            SmartLocation incompleteEntity = genericMongoMapper.fromDTO(smartLocationDTO, SmartLocation.class);

            MongoSmartLocation existingMongoSmartLocation = smartLocationRepository
                    .findByCompoundIndex(countryCode, partyId, id)
                    .orElse(null);

            if (existingMongoSmartLocation == null) {
                return null;
            }

            // MongoSmartLocation extends SmartLocation, so we can cast directly
            SmartLocation existingEntity = existingMongoSmartLocation;

            // Handle smartLocationState from DTO
            if (smartLocationDTO.getSmartLocationState() != null) {
                existingEntity.setSmartLocationState(smartLocationDTO.getSmartLocationState());
            }

            // Set publish = true when state is set to VERIFIED
            if (existingEntity.getSmartLocationState() == SmartLocationState.VERIFIED) {
                existingEntity.setPublish(true);
            } else {
                existingEntity.setPublish(false);
            }

            // Patch the existing location with the new data
            ModelPatcherUtil.smartLocationPatcher(existingEntity,
                    incompleteEntity);

            // Automatically set state to ENRICHED if it's currently PLAIN_OCPI and all
            // required smart fields are present
            if ((existingEntity.getSmartLocationState() == null
                    || existingEntity.getSmartLocationState() == SmartLocationState.PLAIN_OCPI) &&
                    isEnriched(existingEntity)) {
                existingEntity.setSmartLocationState(SmartLocationState.ENRICHED);
            }

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

        // Validate that provided identifiers in the body match the path variables
        if (locationDTO.getId() != null && !locationDTO.getId().equals(locationId)) {
            throw new OCPICustomException(
                    "Location ID in body '" + locationDTO.getId() + "' does not match path variable '" + locationId + "'",
                    Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }
        if (locationDTO.getCountryCode() != null && !locationDTO.getCountryCode().equals(countryCode)) {
            throw new OCPICustomException(
                    "Country code in body '" + locationDTO.getCountryCode() + "' does not match path variable '" + countryCode + "'",
                    Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }
        if (locationDTO.getPartyId() != null && !locationDTO.getPartyId().equals(partyId)) {
            throw new OCPICustomException(
                    "Party ID in body '" + locationDTO.getPartyId() + "' does not match path variable '" + partyId + "'",
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

    private boolean isEnriched(SmartLocation location) {
        return location.getMarketLocationId() != null && !location.getMarketLocationId().isEmpty() &&
                location.getMeteringLocationId() != null && !location.getMeteringLocationId().isEmpty() &&
                location.getDsoMarketPartnerId() != null && !location.getDsoMarketPartnerId().isEmpty() &&
                location.getTsoMarketPartnerId() != null && !location.getTsoMarketPartnerId().isEmpty() &&
                location.getMpoMarketPartnerId() != null && !location.getMpoMarketPartnerId().isEmpty() &&
                location.getMeteringDataSource() != null &&
                location.getDefaultSupplier() != null;
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

    private static final int MAX_IMPORT_ROWS = 1000;

    private static final char BOM = (char) 0xFEFF;

    private static final String[] CSV_HEADERS = {
            "country_code", "party_id", "location_id",
            "market_location_id", "metering_location_id",
            "dso_market_partner_id", "tso_market_partner_id", "mpo_market_partner_id",
            "metering_data_source", "malo",
            "smart_meter_id", "message_queue_url",
            "default_supplier_market_partner_id", "default_supplier_bkv_id", "default_supplier_balancing_group_eic_id"
    };

    @Override
    public BulkImportResultDTO bulkImport(MultipartFile file) {
        BulkImportResultDTO result = new BulkImportResultDTO();

        if (file == null || file.isEmpty()) {
            throw new OCPICustomException("CSV file is empty",
                    Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (PushbackReader reader = new PushbackReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            int firstChar = reader.read();
            if (firstChar != -1 && firstChar != BOM) {
                reader.unread(firstChar);
            }

            try (CSVParser parser = CSVParser.parse(reader, format)) {
                List<String> headerNames = parser.getHeaderNames();
                List<String> missingHeaders = new ArrayList<>();
                for (String required : CSV_HEADERS) {
                    if (!headerNames.contains(required)) {
                        missingHeaders.add(required);
                    }
                }
                if (!missingHeaders.isEmpty()) {
                    throw new OCPICustomException(
                            "CSV is missing required columns: " + String.join(", ", missingHeaders),
                            Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
                }

                for (CSVRecord record : parser) {
                    if (result.getTotalRows() >= MAX_IMPORT_ROWS) {
                        result.addError(record.getRecordNumber(), null,
                                "Row limit of " + MAX_IMPORT_ROWS + " exceeded; remaining rows skipped");
                        break;
                    }
                    result.setTotalRows(result.getTotalRows() + 1);
                    processRow(record, result);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        return result;
    }

    private void processRow(CSVRecord record, BulkImportResultDTO result) {
        String countryCode = getValue(record, "country_code");
        String partyId = getValue(record, "party_id");
        String locationId = getValue(record, "location_id");
        String locationKey = countryCode + "*" + partyId + "*" + locationId;

        try {
            if (isBlank(countryCode) || isBlank(partyId) || isBlank(locationId)) {
                result.addError(record.getRecordNumber(), locationKey,
                        "country_code, party_id and location_id are required");
                return;
            }

            SmartLocationDTO dto = buildDtoFromRow(record);

            SmartLocationDTO updated = patchSmartLocation(countryCode, partyId, locationId, dto);
            if (updated == null) {
                result.addError(record.getRecordNumber(), locationKey,
                        "Location not found");
                return;
            }
            result.incrementSuccess();
        } catch (OCPICustomException e) {
            result.addError(record.getRecordNumber(), locationKey, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to import row {} ({}): {}", record.getRecordNumber(), locationKey, e.getMessage());
            result.addError(record.getRecordNumber(), locationKey, e.getMessage());
        }
    }

    private SmartLocationDTO buildDtoFromRow(CSVRecord record) {
        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setMarketLocationId(getValue(record, "market_location_id"));
        dto.setMeteringLocationId(getValue(record, "metering_location_id"));
        dto.setDsoMarketPartnerId(getValue(record, "dso_market_partner_id"));
        dto.setTsoMarketPartnerId(getValue(record, "tso_market_partner_id"));
        dto.setMpoMarketPartnerId(getValue(record, "mpo_market_partner_id"));
        dto.setMalo(getValue(record, "malo"));
        dto.setSmartMeterId(getValue(record, "smart_meter_id"));
        dto.setMessageQueueUrl(getValue(record, "message_queue_url"));

        String meteringDataSource = getValue(record, "metering_data_source");
        if (!isBlank(meteringDataSource)) {
            try {
                dto.setMeteringDataSource(MeteringDataSource.valueOf(meteringDataSource.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                String allowed = Arrays.stream(MeteringDataSource.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));
                throw new OCPICustomException(
                        "Invalid metering_data_source '" + meteringDataSource + "'. Allowed: " + allowed,
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }
        }

        String supplierId = getValue(record, "default_supplier_market_partner_id");
        String bkvId = getValue(record, "default_supplier_bkv_id");
        String eicId = getValue(record, "default_supplier_balancing_group_eic_id");
        if (!isBlank(supplierId) || !isBlank(bkvId) || !isBlank(eicId)) {
            dto.setDefaultSupplier(DefaultSupplier.builder()
                    .supplierMarketPartnerId(supplierId)
                    .bkvId(bkvId)
                    .balancingGroupEicId(eicId)
                    .build());
        }

        return dto;
    }

    private String getValue(CSVRecord record, String column) {
        if (!record.isMapped(column) || !record.isSet(column)) {
            return null;
        }
        String value = record.get(column);
        return value == null || value.isEmpty() ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public String generateImportTemplate(String countryCode, String partyId) {
        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(CSV_HEADERS)
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            if (isBlank(countryCode) || isBlank(partyId)) {
                log.warn("generateImportTemplate called without countryCode/partyId filters; returning header-only template to avoid unbounded CSV generation");
                return writer.toString();
            }

            List<MongoSmartLocation> locations =
                    smartLocationRepository.findByCountryCodeAndPartyId(countryCode, partyId);
            for (MongoSmartLocation location : locations) {
                DefaultSupplier supplier = location.getDefaultSupplier();
                MeteringDataSource source = location.getMeteringDataSource();
                printer.printRecord(
                        nullToEmpty(location.getCountryCode()),
                        nullToEmpty(location.getPartyId()),
                        nullToEmpty(location.getId()),
                        nullToEmpty(location.getMarketLocationId()),
                        nullToEmpty(location.getMeteringLocationId()),
                        nullToEmpty(location.getDsoMarketPartnerId()),
                        nullToEmpty(location.getTsoMarketPartnerId()),
                        nullToEmpty(location.getMpoMarketPartnerId()),
                        source == null ? "" : source.name(),
                        nullToEmpty(location.getMalo()),
                        nullToEmpty(location.getSmartMeterId()),
                        nullToEmpty(location.getMessageQueueUrl()),
                        supplier == null ? "" : nullToEmpty(supplier.getSupplierMarketPartnerId()),
                        supplier == null ? "" : nullToEmpty(supplier.getBkvId()),
                        supplier == null ? "" : nullToEmpty(supplier.getBalancingGroupEicId()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV template: " + e.getMessage(), e);
        }

        return writer.toString();
    }
}
