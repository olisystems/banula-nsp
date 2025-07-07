package com.banula.navigationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Nsp_ClientInfo")
public class MongoClientInfo {

  @Id
  private String id;

  // Core OCPI client information (following ClientInfo structure)
  private String partyId;
  private String countryCode;
  private String role;
  private String status;
  private LocalDateTime lastUpdated;

  // Audit fields
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;
  private String updatedBy;
}