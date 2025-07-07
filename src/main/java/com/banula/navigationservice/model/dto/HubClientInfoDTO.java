package com.banula.navigationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubClientInfoDTO {

  private String id;

  // Core client information (from ClientInfo)
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