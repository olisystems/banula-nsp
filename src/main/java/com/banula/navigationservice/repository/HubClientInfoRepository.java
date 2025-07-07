package com.banula.navigationservice.repository;

import com.banula.navigationservice.model.MongoClientInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HubClientInfoRepository extends MongoRepository<MongoClientInfo, String> {

  List<MongoClientInfo> findByPartyId(String partyId);

  List<MongoClientInfo> findByCountryCodeAndPartyId(String countryCode, String partyId);

  List<MongoClientInfo> findByStatus(String status);

  Optional<MongoClientInfo> findByPartyIdAndCountryCode(String partyId, String countryCode);
}