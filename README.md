# Banula Navigation Service (NSP)


This repository contains a Navigation Service Provider (NSP) implementation that follows the Open Charge Point Interface (OCPI) protocol, along with custom extensions for enhanced functionality. The service includes a React application for managing smart location information.

## Supported Modules

### OCPI Standard Modules

- **Credentials** (v2.2): For registering and authenticating with other OCPI parties
- **Versions** (v2.2): For discovering available OCPI versions and modules
- **Locations** (v2.2.1): For managing charging station location data

### Custom Modules

- **Smart Locations**: Enhanced location data management with additional features
- **OCN Credentials**: Management of Open Charging Network credentials

## React Application

The repository includes a React-based web application that provides a user interface for:

- Viewing and managing smart location information
- Monitoring charging station status
- Configuring location-specific parameters

The UI can be accessed at `/navigator/ui` after starting the service.

## Running the Service

### Prerequisites

- Java 17+
- Maven 3.6+
- MongoDB 4.4+
- Access to an OCN node (if using OCN functionality)

### Configuration

Create an `application.properties` or `application.yml` file in the `src/main/resources` directory with the following properties:

```properties
# Server Configuration
server.port=8080

# Party Configuration
party.url=http://localhost:8080
party.tokenB=your-token-b-here
party.role=NSP
party.api-prefix=/ocpi/emsp
party.api-non-ocpi-prefix=/non-ocpi/emsp
party.command-timeout=60
party.private-key=your-private-key-here
party.party-id=YOUR-PARTY-ID
party.country-code=US
party.zone-id=Europe/Berlin
party.collection-prefix=nsp_

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/navigation-service
spring.data.mongodb.auto-index-creation=true

# OCN Node Configuration
ocn-node.updating-party=true
ocn-node.url=https://your-ocn-node-url
ocn-node.admin-key=your-ocn-admin-key
ocn-node.signing-supported=true

# Logging
logging.level.com.sharenergy=DEBUG
logging.level.org.springframework.data.mongodb=WARN

# Swagger/OpenAPI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

### Setting up the application properties

1. **Basic Configuration**:

   - `party.url`: The base URL where this service is hosted
   - `party.tokenB`: A secure token for authenticating with other parties
   - `party.role`: Set to `NSP` for this service
   - `party.party-id` and `party.country-code`: Your unique identifiers in the e-mobility ecosystem

2. **MongoDB Configuration**:

   - Configure the MongoDB connection URI
   - The `party.collection-prefix` will be added to all MongoDB collection names

3. **OCN Node Configuration** (if using OCN):
   - `ocn-node.url`: URL of your OCN node
   - `ocn-node.admin-key`: Admin key for OCN node access
   - `party.private-key`: Your private key for signing OCN messages
   - `ocn-node.signing-supported`: Set to true if using signed messages

### Building and Running

#### Using Maven

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/banula-navigation-service-0.0.1-SNAPSHOT.jar
```

#### Using Docker

```bash
# Build Docker image
docker build -t banula-navigation-service .

# Run Docker container
docker run -p 8080:8080 -v /path/to/config:/app/config banula-navigation-service
```

### Verifying the Setup

1. Access the Swagger UI at `http://localhost:8080/swagger-ui.html` to explore the API
2. Test the version endpoint at `http://localhost:8080/ocpi/emsp/2.2/versions`
3. Access the React UI at `http://localhost:8080/navigator/ui`

## Integration with Other OCPI Parties

To connect with other OCPI parties:

1. Use the credentials module to register with the other party
2. Exchange tokens through the OCPI registration process
3. Begin sharing location data through the locations module

For detailed integration steps, refer to the [OCPI Documentation](https://github.com/ocpi/ocpi/blob/master/credentials.asciidoc).

## Smart Locations Management

The Smart Locations module extends standard OCPI locations with additional features:

- Enhanced POI information
- Dynamic status updates
- Custom attributes for specific use cases

Use the React UI or the API endpoints to manage smart location data.

## Troubleshooting

Common issues and their solutions:

- **Connection refused errors**: Check MongoDB connection settings
- **Authentication failures**: Verify your tokens and credentials
- **OCN handshake errors**: Ensure OCN node URL and credentials are correct
- **Missing collections**: Verify MongoDB permissions and database existence

For more detailed logs, adjust the logging levels in the properties file.
