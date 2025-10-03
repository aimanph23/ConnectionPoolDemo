# Project Overview

## Architecture

This is a Spring Boot 3.x application demonstrating connection pooling with YugabyteDB using the YugabyteDB Smart Driver.

```
┌─────────────┐
│   Client    │
│  (Browser/  │
│   cURL)     │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────────────────────────────────┐
│         Spring Boot Application             │
│  ┌──────────────────────────────────────┐   │
│  │    ProductController (REST API)      │   │
│  │  - /api/products                     │   │
│  │  - /api/products/{id}/process        │   │
│  └───────────────┬──────────────────────┘   │
│                  ▼                           │
│  ┌──────────────────────────────────────┐   │
│  │      ProductService (Business)       │───┼──► External API
│  │  - Query DB                          │   │   (JSONPlaceholder)
│  │  - Call external API                 │   │
│  │  - Update based on result            │   │
│  └───────────────┬──────────────────────┘   │
│                  ▼                           │
│  ┌──────────────────────────────────────┐   │
│  │   ProductRepository (Data Access)    │   │
│  │  - JPA/Hibernate                     │   │
│  └───────────────┬──────────────────────┘   │
│                  ▼                           │
│  ┌──────────────────────────────────────┐   │
│  │      HikariCP Connection Pool        │   │
│  │  - Max: 10 connections               │   │
│  │  - Min idle: 5 connections           │   │
│  └───────────────┬──────────────────────┘   │
│                  ▼                           │
│  ┌──────────────────────────────────────┐   │
│  │    YugabyteDB Smart Driver           │   │
│  │  - Load balancing                    │   │
│  │  - Topology awareness                │   │
│  └───────────────┬──────────────────────┘   │
└──────────────────┼──────────────────────────┘
                   │ JDBC
                   ▼
        ┌──────────────────┐
        │   YugabyteDB     │
        │   Cluster        │
        │  (PostgreSQL-    │
        │   compatible)    │
        └──────────────────┘
```

## Key Components

### 1. **Controller Layer** (`ProductController.java`)
- Handles HTTP requests
- Validates input
- Returns JSON responses
- Main endpoint: `POST /api/products/{id}/process`

### 2. **Service Layer** (`ProductService.java`)
- Contains business logic
- Orchestrates database and API calls
- Implements the core workflow:
  1. Query product from database
  2. Call external API
  3. Update product based on API response

### 3. **Repository Layer** (`ProductRepository.java`)
- Extends JpaRepository
- Provides database operations (CRUD)
- Custom queries for advanced filtering

### 4. **Entity Layer** (`Product.java`)
- Maps to database table
- JPA annotations for persistence
- Automatic timestamp management

### 5. **DTO Layer**
- `ProductRequest`: Input validation
- `ProductResponse`: API responses
- `ExternalApiResponse`: External API mapping

## Main Endpoint Workflow

```
POST /api/products/{id}/process
        │
        ▼
   ┌────────────────────────────────────┐
   │  1. Query Database                 │
   │     SELECT * FROM products         │
   │     WHERE id = ?                   │
   └────────┬───────────────────────────┘
            │
            ▼
   ┌────────────────────────────────────┐
   │  2. Call External API              │
   │     GET jsonplaceholder.../posts/1 │
   │     Response: { title, body, ... } │
   └────────┬───────────────────────────┘
            │
            ▼
   ┌────────────────────────────────────┐
   │  3. Business Logic                 │
   │     IF title.length > 50           │
   │        stock += 10                 │
   │     ELSE                           │
   │        stock -= 5 (min 0)          │
   │     END                            │
   │     external_api_response = title  │
   └────────┬───────────────────────────┘
            │
            ▼
   ┌────────────────────────────────────┐
   │  4. Update Database                │
   │     UPDATE products SET            │
   │        stock_quantity = ?,         │
   │        external_api_response = ?,  │
   │        last_updated = NOW()        │
   │     WHERE id = ?                   │
   └────────┬───────────────────────────┘
            │
            ▼
   ┌────────────────────────────────────┐
   │  5. Return Response                │
   │     200 OK + Updated Product       │
   └────────────────────────────────────┘
```

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.1.5 |
| Language | Java | 17 |
| Build Tool | Maven | 3.6+ |
| Database | YugabyteDB | Latest |
| JDBC Driver | YugabyteDB Smart Driver | 42.3.5-yb-5 |
| Connection Pool | HikariCP | (included) |
| ORM | Hibernate/JPA | (included) |
| Web | Spring Web | (included) |
| Utilities | Lombok | (included) |

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

-- Indexes for performance
CREATE INDEX idx_products_stock_quantity ON products(stock_quantity);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_name ON products(name);
```

## Connection Pool Configuration

**HikariCP Settings:**
- **Maximum Pool Size**: 10 connections
- **Minimum Idle**: 5 connections
- **Connection Timeout**: 30 seconds
- **Idle Timeout**: 10 minutes
- **Max Lifetime**: 30 minutes

**YugabyteDB Smart Driver Features:**
- Load balancing across cluster nodes
- Topology awareness
- Automatic failover
- Connection distribution

## API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products/health` | Health check |
| POST | `/api/products` | Create product |
| GET | `/api/products` | Get all products |
| GET | `/api/products/{id}` | Get product by ID |
| **POST** | **`/api/products/{id}/process`** | **Main: Query DB + Call API + Update** |
| DELETE | `/api/products/{id}` | Delete product |

## External API Integration

**Endpoint**: https://jsonplaceholder.typicode.com/posts/1

**Sample Response:**
```json
{
  "userId": 1,
  "id": 1,
  "title": "sunt aut facere repellat provident...",
  "body": "quia et suscipit..."
}
```

**Usage**: The `title` field is stored in the product and used for business logic.

## File Structure

```
ConnectionPoolDemo/
├── pom.xml                          # Maven dependencies
├── docker-compose.yml               # YugabyteDB setup
├── README.md                        # Main documentation
├── SETUP_GUIDE.md                   # Quick start guide
├── PROJECT_OVERVIEW.md              # This file
├── api-requests.http                # Test requests
├── .gitignore                       # Git ignore rules
└── src/
    └── main/
        ├── java/com/example/connectionpool/
        │   ├── ConnectionPoolDemoApplication.java
        │   ├── controller/
        │   │   └── ProductController.java
        │   ├── service/
        │   │   └── ProductService.java
        │   ├── repository/
        │   │   └── ProductRepository.java
        │   ├── entity/
        │   │   └── Product.java
        │   └── dto/
        │       ├── ProductRequest.java
        │       ├── ProductResponse.java
        │       └── ExternalApiResponse.java
        └── resources/
            ├── application.properties
            ├── schema.sql
            └── data.sql
```

## Getting Started

### Quick Start (3 steps)

1. **Start YugabyteDB:**
   ```bash
   docker-compose up -d
   ```

2. **Run Application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Test Main Endpoint:**
   ```bash
   curl -X POST http://localhost:8080/api/products/1/process
   ```

### Detailed Instructions

See `SETUP_GUIDE.md` for comprehensive setup instructions.

## Monitoring

### Application Logs
- Database queries (SQL statements)
- Connection pool statistics
- External API calls
- Business logic execution

### YugabyteDB Admin UI
- URL: http://localhost:7000
- Monitor cluster health
- View query performance
- Check connection statistics

### Connection Pool Metrics
Visible in application logs:
- Active connections
- Idle connections
- Connection wait time
- Query execution time

## Business Logic Details

The main endpoint (`/process`) implements this logic:

1. **Fetch Product**: Retrieve from database by ID
2. **Call API**: GET request to JSONPlaceholder
3. **Apply Business Rules**:
   - Long title (>50 chars) → Increase stock by 10
   - Short title (≤50 chars) → Decrease stock by 5
   - Never go below 0 stock
4. **Store API Response**: Save title in `external_api_response`
5. **Update Timestamp**: Set `last_updated` to current time
6. **Persist Changes**: Save to database
7. **Return Result**: Send updated product as JSON

## Error Handling

- **Product Not Found**: Returns 404 with error message
- **API Call Failure**: Uses default response, continues processing
- **Database Errors**: Logged and returned as 500 errors
- **Validation Errors**: Returns 400 with validation messages

## Testing

Use the included `api-requests.http` file with:
- IntelliJ IDEA (built-in HTTP client)
- VS Code (REST Client extension)
- Or use cURL commands from README

## Future Enhancements

Potential improvements:
- Add authentication/authorization
- Implement caching (Redis)
- Add metrics (Prometheus/Grafana)
- Implement rate limiting
- Add circuit breaker for external API
- Create integration tests
- Add API documentation (Swagger/OpenAPI)
- Implement pagination for GET all products
- Add search and filtering capabilities

## Support

For issues or questions:
- Check `SETUP_GUIDE.md` for troubleshooting
- Review logs for error details
- Verify YugabyteDB is running
- Ensure all dependencies are installed

