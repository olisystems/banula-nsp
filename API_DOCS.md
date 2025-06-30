# Navigation Service Provider (NSP) API Documentation

## Overview

The Navigation Service Provider (NSP) service is an OCPI-compliant implementation that provides charging station location information and related services. This API serves as an interface between Charge Point Operators (CPOs) and Navigation Service Providers (NSPs).

## API Versions

The service supports OCPI version 2.2.1 with various modules.

## Authentication

Most endpoints require authentication using one of the following methods:

- Token-based authentication via `Authorization` header
- OCN signatures for secure communication

## OCPI Modules

### Credentials Module

Base URL: `{party.api-prefix}/2.2.1/credentials`

#### GET /credentials

Retrieves the credentials object to access the server's platform.

**Headers:**

- `Authorization`: Required token for authentication

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    "token": "string",
    "url": "string",
    "roles": [
      {
        "role": "NSP",
        "party_id": "OLN",
        "country_code": "DE",
        "business_details": {
          "name": "Example Operator",
          "website": "http://example.com"
        }
      }
    ]
  }
}
```

#### POST /credentials

Provides the server with a credentials object to access the client's system (register).

**Headers:**

- `Authorization`: Required token for authentication

**Request Body:**

```json
{
  "token": "string",
  "url": "string",
  "roles": [
    {
      "role": "CPO",
      "party_id": "string",
      "country_code": "string",
      "business_details": {
        "name": "string",
        "website": "string"
      }
    }
  ]
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    "token": "string",
    "url": "string",
    "roles": [
      {
        "role": "NSP",
        "party_id": "string",
        "country_code": "string",
        "business_details": {
          "name": "string",
          "website": "string"
        }
      }
    ]
  }
}
```

#### PUT /credentials

Updates credentials for accessing the client's system.

**Request Body:**

```json
{
  "token": "string",
  "url": "string",
  "roles": [
    {
      "role": "string",
      "party_id": "string",
      "country_code": "string",
      "business_details": {
        "name": "string",
        "website": "string"
      }
    }
  ]
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": null
}
```

#### DELETE /credentials

Invalidates the server's credentials to the client's system (unregister).

**Request Body:**

```json
{
  "token": "string",
  "url": "string",
  "roles": [
    {
      "role": "string",
      "party_id": "string",
      "country_code": "string",
      "business_details": {
        "name": "string",
        "website": "string"
      }
    }
  ]
}
```

**Response:**

```json`
{
  "status_code": 1000,
  "data": null
}
```

### Versions Module

Base URL: `{party.api-prefix}/2.2.1/versions`

#### GET /versions

Retrieves supported OCPI versions.

**Response:**

```json
{
  "status_code": 1000,
  "data": [
    {
      "version": "2.2.1",
      "url": "string"
    }
  ]
}
```

#### GET /versions/details/{version}

Retrieves detailed information about supported modules for a specific OCPI version.

**Headers:**

- `Authorization`: Required token for authentication

**Path Parameter:**

- `version`: OCPI version (e.g., 2.2.1)

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    "version": "2.2.1",
    "endpoints": [
      {
        "identifier": "credentials",
        "role": "RECEIVER",
        "url": "string"
      },
      {
        "identifier": "credentials",
        "role": "SENDER",
        "url": "string"
      },
      {
        "identifier": "locations",
        "role": "SENDER",
        "url": "string"
      },
      {
        "identifier": "locations",
        "role": "RECEIVER",
        "url": "string"
      }
    ]
  }
}
```

### Locations Module

Base URL: `{party.api-prefix}/2.2.1/locations`

#### GET /{countryCode}/{partyId}/{locationId}[/{evseUid}[/{connectorId}]]

Retrieves location, EVSE, or connector information.

**Headers:**

- Authorization headers required

**Path Parameters:**

- `countryCode`: Country code of the CPO
- `partyId`: Party ID of the CPO
- `locationId`: Location ID to retrieve
- `evseUid`: (Optional) EVSE UID to retrieve
- `connectorId`: (Optional) Connector ID to retrieve

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    // Location, EVSE, or Connector object depending on request parameters
  }
}
```

#### PUT /{countryCode}/{partyId}/{locationId}

Updates or creates a location.

**Headers:**

- OCN signature and authorization headers required

**Path Parameters:**

- `countryCode`: Country code of the CPO
- `partyId`: Party ID of the CPO
- `locationId`: Location ID to update

**Request Body:**

```json
{
  // Location object
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": null
}
```

#### PUT /{countryCode}/{partyId}/{locationId}/{evseUid}

Updates or creates an EVSE within a location.

**Headers:**

- Token B authorization required

**Path Parameters:**

- `countryCode`: Country code of the CPO
- `partyId`: Party ID of the CPO
- `locationId`: Location ID
- `evseUid`: EVSE UID to update

**Request Body:**

```json
{
  // EVSE object
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": null
}
```

#### PUT /{countryCode}/{partyId}/{locationId}/{evseUid}/{connectorId}

Updates or creates a connector within an EVSE.

**Headers:**

- OCN signature and authorization headers required

**Path Parameters:**

- `countryCode`: Country code of the CPO
- `partyId`: Party ID of the CPO
- `locationId`: Location ID
- `evseUid`: EVSE UID
- `connectorId`: Connector ID to update

**Request Body:**

```json
{
  // Connector object
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": null
}
```

#### PATCH /{countryCode}/{partyId}/{locationId}

Partially updates a location.

**Headers:**

- OCN signature and authorization headers required

**Path Parameters:**

- `countryCode`: Country code of the CPO
- `partyId`: Party ID of the CPO
- `locationId`: Location ID to update

**Request Body:**

```json
{
  // Partial Location object
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": null
}
```

#### PATCH /{countryCode}/{partyId}/{locationId}/{evseUid}

Partially updates an EVSE.

**Headers:**

- OCN signature and authorization headers required

**Path Parameters:**

- `countryCode`: Country code of the CPO
- `partyId`: Party ID of the CPO
- `locationId`: Location ID
- `evseUid`: EVSE UID to update

**Request Body:**

```json
{
  // Partial EVSE object
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": null
}
```

#### PATCH /{countryCode}/{partyId}/{locationId}/{evseUid}/{connectorId}

Partially updates a connector.

**Headers:**

- OCN signature and authorization headers required

**Path Parameters:**

- `countryCode`: Country code of the CPO
- `partyId`: Party ID of the CPO
- `locationId`: Location ID
- `evseUid`: EVSE UID
- `connectorId`: Connector ID to update

**Request Body:**

```json
{
  // Partial Connector object
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": null
}
```

## Custom Modules

### Smart Locations Module

Base URL: `{party.api-non-ocpi-prefix}/locations`

#### GET /{countryCode}/{partyId}

Retrieves all smart locations for a specific party.

**Headers:**

- Token B authorization required

**Path Parameters:**

- `countryCode`: Country code of the party
- `partyId`: Party ID

**Response:**

```json
{
  "status_code": 1000,
  "data": [
    {
      // SmartLocation objects
    }
  ]
}
```

#### GET /{maloId}

Retrieves a smart location by its MALO ID.

**Headers:**

- Token B authorization required

**Path Parameters:**

- `maloId`: MALO ID of the location

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    // SmartLocation object
  }
}
```

#### GET /{countryCode}/{partyId}/{locationId}

Retrieves a specific smart location.

**Headers:**

- Token B authorization required

**Path Parameters:**

- `countryCode`: Country code of the party
- `partyId`: Party ID
- `locationId`: Location ID

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    // SmartLocation object
  }
}
```

#### POST /{countryCode}/{partyId}

Creates a new smart location.

**Headers:**

- Token B authorization required

**Path Parameters:**

- `countryCode`: Country code of the party
- `partyId`: Party ID

**Request Body:**

```json
{
  // SmartLocation object
}
```

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    // Created SmartLocation object
  }
}
```

### OCN Credentials Module

Base URL: `{party.api-non-ocpi-prefix}/ocn-credentials`

#### GET

Retrieves OCN credentials.

**Headers:**

- Token B authorization required

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    // OcnCredential object
  }
}
```

#### DELETE

Deletes OCN credentials.

**Headers:**

- Token B authorization required

**Response:**

```json
{
  "status_code": 1000,
  "data": {
    // Deleted OcnCredential object
  },
  "status_message": "Ocn Credential deleted successfully!"
}
```

## Error Codes

- `1000`: Success
- `2001`: Missing required arguments
- `2004`: Invalid token
- Other standard OCPI error codes apply

## Notes

- All endpoints support CORS
- The API adheres to the OCPI standard while providing custom extensions for advanced functionality
- Authentication tokens must be properly managed and secured
- For proper integration, partners should follow the registration flow as defined in the OCPI standard
