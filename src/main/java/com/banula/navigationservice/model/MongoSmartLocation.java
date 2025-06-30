package com.banula.navigationservice.model;

import com.banula.openlib.ocpi.custom.smartlocations.SmartLocation;
import com.banula.openlib.ocpi.model.Location;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@SuperBuilder
@NoArgsConstructor(force = true)
@Document(collection = "Nsp_Location")
public class MongoSmartLocation extends SmartLocation {

    @Id
    private String mongoId;


    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public void updateLocation(Location location) {
        this.setCountryCode(location.getCountryCode());
        this.setPartyId(location.getPartyId());
        this.setId(location.getId());
        this.setPublish(location.getPublish());
        this.setPublishAllowedTo(location.getPublishAllowedTo());
        this.setName(location.getName());
        this.setAddress(location.getAddress());
        this.setCity(location.getCity());
        this.setPostalCode(location.getPostalCode());
        this.setState(location.getState());
        this.setCountry(location.getCountry());
        this.setCoordinates(location.getCoordinates());
        this.setRelatedLocations(location.getRelatedLocations());
        this.setParkingType(location.getParkingType());
        this.setEvses(location.getEvses());
        this.setDirections(location.getDirections());
        this.setOperator(location.getOperator());
        this.setSubOperator(location.getSubOperator());
        this.setOwner(location.getOwner());
        this.setFacilities(location.getFacilities());
        this.setTimeZone(location.getTimeZone());
        this.setOpeningTimes(location.getOpeningTimes());
        this.setChargingWhenClosed(location.getChargingWhenClosed());
        this.setImages(location.getImages());
        this.setEnergyMix(location.getEnergyMix());
        this.setLastUpdated(location.getLastUpdated());
    }

    public Location mapToLocationOnly() {
        Location target = new Location();

        target.setCountryCode(getCountryCode());
        target.setPartyId(getPartyId());
        target.setId(getId());
        target.setPublish(getPublish());
        target.setPublishAllowedTo(getPublishAllowedTo());
        target.setName(getName());
        target.setAddress(getAddress());
        target.setCity(getCity());
        target.setPostalCode(getPostalCode());
        target.setState(getState());
        target.setCountry(getCountry());
        target.setCoordinates(getCoordinates());
        target.setRelatedLocations(getRelatedLocations());
        target.setParkingType(getParkingType());
        target.setEvses(getEvses());
        target.setDirections(getDirections());
        target.setOperator(getOperator());
        target.setSubOperator(getSubOperator());
        target.setOwner(getOwner());
        target.setFacilities(getFacilities());
        target.setTimeZone(getTimeZone());
        target.setOpeningTimes(getOpeningTimes());
        target.setChargingWhenClosed(getChargingWhenClosed());
        target.setImages(getImages());
        target.setEnergyMix(getEnergyMix());
        target.setLastUpdated(getLastUpdated());

        if(target.getId() == null) {
            target.setId(this.mongoId);
        }

        return target;
    }


}
