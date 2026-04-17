package com.banula.navigationservice.model;

public interface HasCompositeMongoId {
    String getCountryCode();

    String getPartyId();

    String getId(); // this is the "internal" id like CDR id, Location id, etc.

    String getMongoId();

    void setMongoId(String id);
}
