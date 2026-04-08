package com.banula.navigationservice.repository;

import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.openlib.mongodb.repository.OcpiCommonCompoundIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SmartLocationRepository
    extends MongoRepository<MongoSmartLocation, String>, OcpiCommonCompoundIndex<MongoSmartLocation> {
  List<MongoSmartLocation> findByCountryCodeAndPartyId(String countryCode, String party_id);

  Optional<MongoSmartLocation> findByMarketLocationId(String maloId);

  /**
   * Find published smart locations with optional date range filtering and
   * pagination.
   * Behaves like LocationUtility.findLocations but filters for published
   * locations only.
   * 
   * @param dateFrom Only return locations with lastUpdated >= dateFrom (if not
   *                 null)
   * @param dateTo   Only return locations with lastUpdated <= dateTo (if not
   *                 null)
   * @param pageable Pagination parameters (offset and limit)
   * @return Page of published MongoSmartLocation entities
   */
  @Query("{ 'publish': true" +
      ", $and: [" +
      "  { $or: [ { 'lastUpdated': { $gte: ?0 } }, { $expr: { $eq: [?0, null] } } ] }" +
      ", { $or: [ { 'lastUpdated': { $lte: ?1 } }, { $expr: { $eq: [?1, null] } } ] }" +
      "] }")
  Page<MongoSmartLocation> findPublishedSmartLocations(LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable);

}
