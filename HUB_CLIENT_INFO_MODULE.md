# Client Info Module

## Overview

The Client Info module provides a simplified interface for managing OCPI client information in the Banula Navigation Service Provider (NSP). This module focuses on core OCPI client data without hub-specific complexity, making it ideal for Open Charging Network (OCN) node communication.

## Architecture

The module follows the standard Spring Boot layered architecture:

```
Controller Layer (HubClientInfoController)
    ↓
Service Layer (HubClientInfoService → HubClientInfoServiceImpl)
    ↓
Repository Layer (HubClientInfoRepository)
    ↓
Data Layer (MongoDB - Nsp_ClientInfo collection)
```

## Core Components

### Model (`HubClientInfo`)

The model contains only essential OCPI client information:

```java
@Document(collection = "Nsp_ClientInfo")
public class HubClientInfo {
    @Id
    private String id;
    
    // Core OCPI client information
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
```

### DTO (`HubClientInfoDTO`)

Data Transfer Object for API communication:

```java
public class HubClientInfoDTO {
    private String id;
    private String partyId;
    private String countryCode;
    private String role;
    private String status;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
```

### Repository (`HubClientInfoRepository`)

MongoDB repository with essential query methods:

```java
public interface HubClientInfoRepository extends MongoRepository<HubClientInfo, String> {
    List<HubClientInfo> findByPartyId(String partyId);
    List<HubClientInfo> findByCountryCodeAndPartyId(String countryCode, String partyId);
    List<HubClientInfo> findByStatus(String status);
    Optional<HubClientInfo> findByPartyIdAndCountryCode(String partyId, String countryCode);
}
```

### Service (`HubClientInfoService`)

Service interface defining core operations:

```java
public interface HubClientInfoService {
    List<HubClientInfoDTO> getAllHubClientInfos();
    HubClientInfoDTO getHubClientInfoById(String id);
    HubClientInfoDTO getHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode);
    List<HubClientInfoDTO> getHubClientInfosByPartyId(String partyId);
    List<HubClientInfoDTO> getHubClientInfosByCountryCodeAndPartyId(String countryCode, String partyId);
    List<HubClientInfoDTO> getHubClientInfosByStatus(String status);
    HubClientInfoDTO createHubClientInfo(HubClientInfoDTO hubClientInfoDTO);
    HubClientInfoDTO updateHubClientInfo(String id, HubClientInfoDTO hubClientInfoDTO);
    HubClientInfoDTO updateHubClientInfoStatus(String id, String status);
    void deleteHubClientInfo(String id);
    void deleteHubClientInfoByPartyIdAndCountryCode(String partyId, String countryCode);
}
```

## API Endpoints

### Base URL
```
${party.api-non-ocpi-prefix}/clientinfo
```

### GET Endpoints

| Endpoint | Description | Parameters |
|----------|-------------|------------|
| `GET /` | Get all client infos | None |
| `GET /{id}` | Get client info by ID | `id` (path) |
| `GET /party/{partyId}/country/{countryCode}` | Get client info by party ID and country code | `partyId`, `countryCode` (path) |
| `GET /party/{partyId}` | Get client infos by party ID | `partyId` (path) |
| `GET /country/{countryCode}/party/{partyId}` | Get client infos by country code and party ID | `countryCode`, `partyId` (path) |
| `GET /status/{status}` | Get client infos by status | `status` (path) |

### POST Endpoints

| Endpoint | Description | Body |
|----------|-------------|------|
| `POST /` | Create new client info | `HubClientInfoDTO` |

### PUT Endpoints

| Endpoint | Description | Parameters | Body |
|----------|-------------|------------|------|
| `PUT /{id}` | Update client info by ID | `id` (path) | `HubClientInfoDTO` |

### PATCH Endpoints

| Endpoint | Description | Parameters |
|----------|-------------|------------|
| `PATCH /{id}/status` | Update client info status | `id` (path), `status` (query) |

### DELETE Endpoints

| Endpoint | Description | Parameters |
|----------|-------------|------------|
| `DELETE /{id}` | Delete client info by ID | `id` (path) |
| `DELETE /party/{partyId}/country/{countryCode}` | Delete client info by party ID and country code | `partyId`, `countryCode` (path) |

## Request/Response Examples

### Create Client Info

**Request:**
```json
POST /api/v1/clientinfo
{
  "partyId": "TEST",
  "countryCode": "NL",
  "role": "CPO",
  "status": "ACTIVE",
  "createdBy": "admin"
}
```

**Response:**
```json
{
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "partyId": "TEST",
    "countryCode": "NL",
    "role": "CPO",
    "status": "ACTIVE",
    "lastUpdated": "2024-01-15T10:30:00",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "createdBy": "admin",
    "updatedBy": "admin"
  }
}
```

### Get Client Info by Party ID and Country Code

**Request:**
```json
GET /api/v1/clientinfo/party/TEST/country/NL
```

**Response:**
```json
{
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "partyId": "TEST",
    "countryCode": "NL",
    "role": "CPO",
    "status": "ACTIVE",
    "lastUpdated": "2024-01-15T10:30:00",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "createdBy": "admin",
    "updatedBy": "admin"
  }
}
```

### Update Client Info Status

**Request:**
```json
PATCH /api/v1/clientinfo/507f1f77bcf86cd799439011/status?status=INACTIVE
```

**Response:**
```json
{
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "partyId": "TEST",
    "countryCode": "NL",
    "role": "CPO",
    "status": "INACTIVE",
    "lastUpdated": "2024-01-15T11:00:00",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:00:00",
    "createdBy": "admin",
    "updatedBy": "admin"
  }
}
```

## Key Features

### 1. **OCPI Compliance**
- Follows OCPI standards for client information
- Uses standard OCPI fields: `partyId`, `countryCode`, `role`, `status`, `lastUpdated`

### 2. **Simplified Data Model**
- Removed hub-specific complexity
- Focused on essential client information
- Clean, maintainable structure

### 3. **Flexible Querying**
- Query by party ID and country code combination
- Filter by status
- Support for OCPI standard identifiers

### 4. **Audit Trail**
- Automatic timestamp management
- User tracking for create/update operations
- Full audit history

### 5. **Error Handling**
- Comprehensive exception handling
- OCPI-compliant error responses
- Detailed logging for debugging

## Benefits

### 1. **OCN Compatibility**
- Designed for Open Charging Network communication
- Simplified for direct node-to-node interaction
- No hub-specific overhead

### 2. **Maintainability**
- Clean, focused codebase
- Reduced complexity
- Easier to understand and modify

### 3. **Performance**
- Minimal data model
- Efficient queries
- Reduced storage requirements

### 4. **Standards Compliance**
- OCPI 2.2.1 compliant
- Standard field names and structures
- Interoperable with other OCPI implementations

## Testing

The module includes comprehensive unit tests covering:

- CRUD operations
- Query methods
- Error scenarios
- Edge cases
- Data validation

Run tests with:
```bash
./mvnw test -Dtest=HubClientInfoServiceTest
```

## Usage Examples

### For OCN Node Communication

```java
// Create a new client info for OCN communication
HubClientInfoDTO clientInfo = HubClientInfoDTO.builder()
    .partyId("OCN_NODE_001")
    .countryCode("NL")
    .role("CPO")
    .status("ACTIVE")
    .createdBy("system")
    .build();

HubClientInfoDTO created = hubClientInfoService.createHubClientInfo(clientInfo);
```

### Query by OCPI Identifiers

```java
// Find client by OCPI party ID and country code
HubClientInfoDTO client = hubClientInfoService
    .getHubClientInfoByPartyIdAndCountryCode("OCN_NODE_001", "NL");

// Get all clients for a specific party
List<HubClientInfoDTO> clients = hubClientInfoService
    .getHubClientInfosByPartyId("OCN_NODE_001");
```

## Future Enhancements

1. **OCPI 3.0 Support**: Update to latest OCPI version when available
2. **Enhanced Validation**: Add more comprehensive input validation
3. **Caching**: Implement Redis caching for frequently accessed data
4. **Metrics**: Add performance monitoring and metrics
5. **Bulk Operations**: Support for bulk create/update operations

## Conclusion

The simplified Client Info module provides a clean, OCPI-compliant interface for managing client information in the Banula NSP. By removing hub-specific complexity, it's optimized for Open Charging Network communication while maintaining full OCPI standards compliance. 