package com.banula.navigationservice.repository;

import com.banula.navigationservice.model.MongoSmartLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SmartLocationRepository  extends MongoRepository<MongoSmartLocation, String> {
    List<MongoSmartLocation> findByCountryCodeAndPartyId(String countryCode, String party_id);
    Optional<MongoSmartLocation> findByMarketLocationId(String maloId);

    @Query("""
         {
           'countryCode': ?0,
           'partyId': ?1,
           '$or': [
             { 'id': ?2 },
             { '_id': ?2 }
           ]
         }
             """)
    Optional<MongoSmartLocation> findByCompositeKey(String countryCode, String partyId, String locationId);

}
