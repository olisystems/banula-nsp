package com.banula.navigationservice.repository;

import com.banula.navigationservice.model.MongoClientInfo;
import com.banula.openlib.ocpi.model.enums.Role;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HubClientInfoRepository extends MongoRepository<MongoClientInfo, String> {

  List<MongoClientInfo> findByPartyIdAndCountryCode(String partyId, String countryCode);
  Optional<MongoClientInfo> findByPartyIdAndCountryCodeAndRole(String partId, String countryCode, Role role);

  /**
   * Duplicate-tolerant variant of {@link #findByPartyIdAndCountryCodeAndRole}. The collection has no
   * unique index on this key, so a write race can leave more than one document for the same
   * party/role - which makes the Optional-returning finder throw and silently freeze that party.
   * Newest first, so callers can take the most recent one.
   */
  List<MongoClientInfo> findByPartyIdAndCountryCodeAndRoleOrderByLastUpdatedDesc(String partyId, String countryCode,
      Role role);

}