package com.banula.navigationservice.repository;

import com.banula.navigationservice.model.MongoClientInfo;
import com.banula.openlib.ocpi.model.enums.Role;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HubClientInfoRepository extends MongoRepository<MongoClientInfo, String> {

  List<MongoClientInfo> findByPartyIdAndCountryCode(String partyId, String countryCode);
  Optional<MongoClientInfo> findByPartyIdAndCountryCodeAndRole(String partId, String countryCode, Role role);

}