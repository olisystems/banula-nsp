package com.banula.navigationservice.mapper;

import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.model.dto.GeoLocationDTO;
import com.banula.openlib.ocpi.model.vo.GeoLocation;
import com.banula.openlib.ocpi.custom.smartlocations.DefaultSupplier;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SmartLocationMapper {

    public static MongoSmartLocation toSmartLocationEntity(SmartLocationDTO smartLocationDTO) {

        if (smartLocationDTO == null)
            return null;

        GeoLocation geoLocation = null;
        if (smartLocationDTO.getCoordinates() != null) {
            GeoLocationDTO geoLocationDTO = smartLocationDTO.getCoordinates();
            if (geoLocationDTO.getLatitude() != null && geoLocationDTO.getLongitude() != null) {
                geoLocation = new GeoLocation(
                        Double.parseDouble(smartLocationDTO.getCoordinates().getLatitude()),
                        Double.parseDouble(smartLocationDTO.getCoordinates().getLongitude()));
            }
        }
        // Map DefaultSupplier if present
        DefaultSupplier defaultSupplier = null;
        if (smartLocationDTO.getDefaultSupplier() != null) {
            defaultSupplier = DefaultSupplier.builder()
                    .supplierMarketPartnerId(smartLocationDTO.getDefaultSupplier().getSupplierMarketPartnerId())
                    .bkvId(smartLocationDTO.getDefaultSupplier().getBkvId())
                    .balancingGroupEicId(smartLocationDTO.getDefaultSupplier().getBalancingGroupEicId())
                    .build();
        }

        return MongoSmartLocation.builder()
                .id(smartLocationDTO.getId())
                .city(smartLocationDTO.getCity())
                .address(smartLocationDTO.getAddress())
                .evses(smartLocationDTO.getEvses())
                .coordinates(geoLocation)
                .energyMix(smartLocationDTO.getEnergyMix())
                .name(smartLocationDTO.getName())
                .countryCode(smartLocationDTO.getCountryCode())
                .country(smartLocationDTO.getCountry())
                .lastUpdated(smartLocationDTO.getLastUpdated())
                .owner(smartLocationDTO.getOwner())
                .relatedLocations(smartLocationDTO.getRelatedLocations())
                .directions(smartLocationDTO.getDirections())
                .images(smartLocationDTO.getImages())
                .facilities(smartLocationDTO.getFacilities())
                .openingTimes(smartLocationDTO.getOpeningTimes())
                .operator(smartLocationDTO.getOperator())
                .state(smartLocationDTO.getState())
                .parkingType(smartLocationDTO.getParkingType())
                .publish(smartLocationDTO.getPublish())
                .postalCode(smartLocationDTO.getPostalCode())
                .partyId(smartLocationDTO.getPartyId())
                .publishAllowedTo(smartLocationDTO.getPublishAllowedTo())
                .subOperator(smartLocationDTO.getSubOperator())
                .timeZone(smartLocationDTO.getTimeZone())
                .chargingWhenClosed(smartLocationDTO.getChargingWhenClosed())
                // Smart Location Fields
                .marketLocationId(smartLocationDTO.getMarketLocationId())
                .meteringLocationId(smartLocationDTO.getMeteringLocationId())
                .dsoMarketPartnerId(smartLocationDTO.getDsoMarketPartnerId())
                .tsoMarketPartnerId(smartLocationDTO.getTsoMarketPartnerId())
                .mpoMarketPartnerId(smartLocationDTO.getMpoMarketPartnerId())
                .meteringDataSource(smartLocationDTO.getMeteringDataSource())
                .smartMeterId(smartLocationDTO.getSmartMeterId())
                .messageQueueUrl(smartLocationDTO.getMessageQueueUrl())
                .defaultSupplier(defaultSupplier)
                .build();
    }

    public static SmartLocationDTO toSmartLocationDTO(MongoSmartLocation location) {
        if (location == null)
            return null;

        GeoLocationDTO geoLocationDTO = null;
        if (location.getCoordinates() != null && location.getCoordinates().getCoordinates() != null) {
            List<Double> coordinates = location.getCoordinates().getCoordinates();
            geoLocationDTO = new GeoLocationDTO(coordinates.get(0), coordinates.get(1));
        }

        // Map DefaultSupplier if present
        DefaultSupplier defaultSupplierDTO = null;
        if (location.getDefaultSupplier() != null) {
            defaultSupplierDTO = DefaultSupplier.builder()
                    .supplierMarketPartnerId(location.getDefaultSupplier().getSupplierMarketPartnerId())
                    .bkvId(location.getDefaultSupplier().getBkvId())
                    .balancingGroupEicId(location.getDefaultSupplier().getBalancingGroupEicId())
                    .build();
        }

        return SmartLocationDTO.builder()
                .id(location.getId())
                .city(location.getCity())
                .address(location.getAddress())
                .evses(location.getEvses())
                .coordinates(geoLocationDTO)
                .energyMix(location.getEnergyMix())
                .name(location.getName())
                .countryCode(location.getCountryCode())
                .country(location.getCountry())
                .lastUpdated(location.getLastUpdated())
                .owner(location.getOwner())
                .relatedLocations(location.getRelatedLocations())
                .directions(location.getDirections())
                .images(location.getImages())
                .facilities(location.getFacilities())
                .openingTimes(location.getOpeningTimes())
                .operator(location.getOperator())
                .state(location.getState())
                .parkingType(location.getParkingType())
                .publish(location.getPublish())
                .postalCode(location.getPostalCode())
                .partyId(location.getPartyId())
                .publishAllowedTo(location.getPublishAllowedTo())
                .subOperator(location.getSubOperator())
                .timeZone(location.getTimeZone())
                .chargingWhenClosed(location.getChargingWhenClosed())
                // Smart Location Fields
                .marketLocationId(location.getMarketLocationId())
                .meteringLocationId(location.getMeteringLocationId())
                .dsoMarketPartnerId(location.getDsoMarketPartnerId())
                .tsoMarketPartnerId(location.getTsoMarketPartnerId())
                .mpoMarketPartnerId(location.getMpoMarketPartnerId())
                .meteringDataSource(location.getMeteringDataSource())
                .smartMeterId(location.getSmartMeterId())
                .messageQueueUrl(location.getMessageQueueUrl())
                .defaultSupplier(defaultSupplierDTO)
                .build();
    }

    public static List<SmartLocationDTO> toListSmartLocationDTO(List<MongoSmartLocation> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .filter(Objects::nonNull)
                .map(SmartLocationMapper::toSmartLocationDTO)
                .collect(Collectors.toList());
    }

    public static List<MongoSmartLocation> toListSmartLocation(List<SmartLocationDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .filter(Objects::nonNull)
                .map(SmartLocationMapper::toSmartLocationEntity)
                .collect(Collectors.toList());
    }
}
