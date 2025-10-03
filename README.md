# Connection Pool Demo - Spring Boot with YugabyteDB

A Spring Boot application demonstrating database connection pooling with YugabyteDB using the YugabyteDB Smart Driver. The application includes an endpoint that queries the database, calls an external API, and updates records based on the result.

## Features

- **Spring Boot 3.1.5** - Modern Java framework
- **YugabyteDB Smart Driver** - Intelligent connection pooling and load balancing
- **HikariCP** - High-performance JDBC connection pool
- **RESTful API** - Complete CRUD operations
- **External API Integration** - Calls dummy API and updates database
- **JPA/Hibernate** - Object-relational mapping
- **Lombok** - Reduces boilerplate code

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- YugabyteDB instance running (local or remote)

## YugabyteDB Setup

### Option 1: Local YugabyteDB Installation

1. Download and install YugabyteDB from [https://download.yugabyte.com/](https://download.yugabyte.com/)

2. Start a local cluster:
```bash
./bin/yugabyted start
```

3. Verify the cluster is running:
```bash
./bin/yugabyted status
```

### Option 2: Docker

Run YugabyteDB in Docker:
```bash
docker run -d --name yugabyte \
  -p 7000:7000 -p 9000:9000 -p 5433:5433 -p 9042:9042 \
  yugabytedb/yugabyte:latest \
  bin/yugabyted start \
  --daemon=false
```

### Option 3: YugabyteDB Cloud

Sign up for a free tier at [https://cloud.yugabyte.com/](https://cloud.yugabyte.com/)

## Configuration

Update `src/main/resources/application.properties` with your YugabyteDB connection details:

```properties
# YugabyteDB Configuration
spring.datasource.url=jdbc:yugabytedb://localhost:5433/yugabyte?load-balance=true
spring.datasource.username=yugabyte
spring.datasource.password=yugabyte
```

For YugabyteDB Cloud, use the connection string provided in your cluster dashboard.

## Building the Application

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:
```bash
java -jar target/connection-pool-demo-1.0.0.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Monitoring Endpoints (NEW! 🔥)

#### Real-Time Dashboard 📊

**Visual monitoring with live charts:**
```
http://localhost:8080/dashboard/hikari
```

Features:
- ✅ Live updating charts (500ms refresh)
- ✅ Connection pool visualization
- ✅ Automatic alerts
- ✅ 60-second history graph
- ✅ No manual refresh needed

See [REALTIME_DASHBOARD_GUIDE.md](REALTIME_DASHBOARD_GUIDE.md) for details.

#### API Endpoints

Monitor HikariCP connection pool programmatically:

```bash
# Quick status check
GET http://localhost:8080/api/monitoring/hikari/status

# Detailed metrics
GET http://localhost:8080/api/monitoring/hikari/details

# Basic metrics
GET http://localhost:8080/api/monitoring/hikari

# Real-time stream (SSE)
GET http://localhost:8080/api/monitoring/hikari/stream
```

**Example Response:**
```json
{
  "active": 3,
  "idle": 7,
  "total": 10,
  "waiting": 0,
  "max": 10,
  "healthy": true
}
```

See [MONITORING_GUIDE.md](MONITORING_GUIDE.md) for complete documentation.

### Health Check
```bash
GET http://localhost:8080/api/products/health
```

### Create Product
```bash
POST http://localhost:8080/api/products
Content-Type: application/json

{
  "name": "Laptop",
  "description": "High-end laptop",
  "price": 1200.00,
  "stockQuantity": 50
}
```

### Get All Products
```bash
GET http://localhost:8080/api/products
```

### Get Product by ID (V1 - Blocking)
```bash
GET http://localhost:8080/api/products/1
```
⚠️ Holds DB connection during entire request (~2 seconds)

### Get Product by ID (V2 - Non-Blocking) 🚀 NEW!
```bash
GET http://localhost:8080/api/products/v2/1
```
✅ Releases DB connection immediately after query (~50ms hold time)
✅ Mock API delay happens WITHOUT holding DB connection
✅ Prevents connection pool exhaustion under load

**Key Benefit:** With V2, you can handle 10x more concurrent requests without exhausting the connection pool!

See [ASYNC_API_GUIDE.md](ASYNC_API_GUIDE.md) for detailed explanation.

### **Main Endpoint: Process Product** ⭐
This endpoint demonstrates the core functionality:
1. Queries the database for the product
2. Calls an external dummy API (JSONPlaceholder)
3. Updates the product based on the API response

```bash
POST http://localhost:8080/api/products/1/process
```

**Business Logic:**
- If the API response title is > 50 characters: Increases stock by 10
- Otherwise: Decreases stock by 5 (minimum 0)
- Stores the API response title in the product record

### Delete Product
```bash
DELETE http://localhost:8080/api/products/1
```

## Testing with cURL

### Create a product:
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Mouse",
    "description": "RGB gaming mouse with 12 buttons",
    "price": 79.99,
    "stockQuantity": 100
  }'
```

### Process a product (main feature):
```bash
curl -X POST http://localhost:8080/api/products/1/process
```

### Get all products:
```bash
curl http://localhost:8080/api/products
```

## Connection Pool Configuration

The application uses HikariCP for connection pooling with the following settings:

- **Maximum Pool Size:** 10 connections
- **Minimum Idle:** 5 connections
- **Connection Timeout:** 30 seconds
- **Idle Timeout:** 10 minutes
- **Max Lifetime:** 30 minutes

These settings can be adjusted in `application.properties`.

## YugabyteDB Smart Driver Features

The YugabyteDB JDBC Smart Driver provides:

1. **Topology-Aware Load Balancing** - Automatically distributes connections across nodes
2. **Connection Load Balancing** - Uses `load-balance=true` parameter
3. **Cluster Awareness** - Monitors cluster topology changes
4. **Fault Tolerance** - Automatically handles node failures

## Project Structure

```
src/
├── main/
│   ├── java/com/example/connectionpool/
│   │   ├── ConnectionPoolDemoApplication.java  # Main application
│   │   ├── controller/
│   │   │   └── ProductController.java          # REST endpoints
│   │   ├── service/
│   │   │   └── ProductService.java             # Business logic
│   │   ├── repository/
│   │   │   └── ProductRepository.java          # Data access
│   │   ├── entity/
│   │   │   └── Product.java                    # JPA entity
│   │   └── dto/
│   │       ├── ProductRequest.java             # Request DTO
│   │       ├── ProductResponse.java            # Response DTO
│   │       └── ExternalApiResponse.java        # External API DTO
│   └── resources/
│       ├── application.properties              # Configuration
│       ├── schema.sql                          # Database schema
│       └── data.sql                            # Sample data
└── pom.xml                                     # Maven dependencies
```

## Database Schema

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    external_api_response TEXT,
    last_updated TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Sample Data

The project includes two sample data files:

- **`data.sql`** - 5 basic products (auto-loaded on startup)
- **`sample-data-100.sql`** - 100 diverse products across 10 categories

To load the 100-record sample dataset, see **`LOAD_SAMPLE_DATA.md`** for detailed instructions.

## Logging

The application provides detailed logging for:
- Database operations
- Connection pool statistics
- External API calls
- Business logic execution

Check the console output to monitor the application behavior.

## Troubleshooting

### Connection Issues

If you encounter connection errors:

1. Verify YugabyteDB is running:
```bash
./bin/yugabyted status
```

2. Check if port 5433 is accessible:
```bash
telnet localhost 5433
```

3. Verify credentials in `application.properties`

### Driver Not Found

If you get a driver class error, ensure the YugabyteDB JDBC driver is included in `pom.xml`:
```xml
<dependency>
    <groupId>com.yugabyte</groupId>
    <artifactId>jdbc-yugabytedb</artifactId>
    <version>42.3.5-yb-5</version>
</dependency>
```

## External API Integration

The application integrates with **Postman Echo Delay API** for realistic network delay testing:
- URL: `https://postman-echo.com/delay/{seconds}`
- Configurable delay for testing connection pool behavior
- Real HTTP calls (not simulated with Thread.sleep)

### Configuration

```properties
# Postman Echo API with configurable delay
external.delay.api.base-url=https://postman-echo.com/delay
external.delay.api.delay-seconds=2
```

### Change Delay

```properties
# 1 second delay
external.delay.api.delay-seconds=1

# 5 second delay
external.delay.api.delay-seconds=5
```

See [POSTMAN_ECHO_GUIDE.md](POSTMAN_ECHO_GUIDE.md) for detailed documentation.

## License

This project is provided as-is for demonstration purposes.

## Additional Resources

- [YugabyteDB Documentation](https://docs.yugabyte.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [YugabyteDB Smart Drivers](https://docs.yugabyte.com/preview/drivers-orms/smart-drivers/)

