# Application Monitoring and Health Checks

This package contains comprehensive monitoring and health check components for the URL Shortener MVP application, implementing task 10.2 requirements.

## Overview

The monitoring system provides real-time health status, application metrics, and startup verification to ensure the application runs reliably in production environments.

## Components

### 1. Health Indicators

#### RedisHealthIndicator
- **Purpose**: Monitors Redis connectivity and availability
- **Endpoint**: `/actuator/health/redis`
- **Implementation**: Uses Redis PING command to verify connection
- **Status Indicators**:
  - `UP`: Redis responds with "PONG"
  - `DOWN`: Connection failed or unexpected response

#### DatabaseHealthIndicator
- **Purpose**: Monitors PostgreSQL database connectivity
- **Endpoint**: `/actuator/health/db`
- **Implementation**: Executes count query on UrlMapping table
- **Status Indicators**:
  - `UP`: Database accessible, includes total URL count
  - `DOWN`: Connection failed or query error

### 2. Application Information

#### UrlShortenerInfoContributor
- **Purpose**: Provides application metadata and statistics
- **Endpoint**: `/actuator/info`
- **Information Provided**:
  - Application name, version, and description
  - Total URLs in the system
  - Application uptime
  - Error details if statistics unavailable

### 3. Custom Metrics

#### UrlShortenerMetrics
- **Purpose**: Tracks application performance and usage metrics
- **Metrics Collected**:
  - `url_shortener_urls_created_total`: Total URLs created
  - `url_shortener_urls_retrieved_total`: Total URL retrievals
  - `url_shortener_cache_hits_total`: Cache hit count
  - `url_shortener_cache_misses_total`: Cache miss count
  - `url_shortener_errors_total`: Total error count
  - `url_shortener_url_creation_duration`: Time to create URLs
  - `url_shortener_url_retrieval_duration`: Time to retrieve URLs

### 4. Startup Verification

#### StartupHealthVerifier
- **Purpose**: Verifies all dependencies during application startup
- **Implementation**: Runs health checks on Redis and database at startup
- **Logging**: Provides clear startup status messages

## Configuration

### Application Properties
```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,env,logger
management.endpoint.health.show-details=always
management.endpoint.health.show-components=always
management.health.redis.enabled=true
management.health.db.enabled=true

# Custom health check timeouts
app.health.redis.timeout=5000
app.health.database.timeout=10000

# Logging configuration
logging.level.com.pm.urlshortenerbackend=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.file.name=logs/url-shortener.log
```

## Available Endpoints

### Health Endpoints
- `GET /actuator/health` - Overall application health
- `GET /actuator/health/redis` - Redis-specific health
- `GET /actuator/health/db` - Database-specific health

### Information Endpoints
- `GET /actuator/info` - Application information and statistics

### Metrics Endpoints
- `GET /actuator/metrics` - All available metrics
- `GET /actuator/metrics/{metric-name}` - Specific metric details

### Example Responses

#### Health Check Response
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "redis": "available",
        "response": "PONG"
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "Available",
        "totalUrls": 42,
        "status": "Connected"
      }
    }
  }
}
```

#### Application Info Response
```json
{
  "application": {
    "name": "URL Shortener",
    "version": "1.0.0",
    "description": "A simple URL shortening service"
  },
  "statistics": {
    "totalUrls": 42,
    "uptime": 123456789
  }
}
```

## Integration with Services

### UrlService Integration
The `UrlServiceImpl` has been enhanced with:
- **Metrics Collection**: Automatic tracking of operations and performance
- **Enhanced Logging**: Structured logging for debugging and monitoring
- **Timer Measurements**: Performance timing for creation and retrieval operations
- **Error Tracking**: Automatic error counting and logging

### Key Integrations
```java
// Metrics tracking in service methods
Timer.Sample sample = metrics.startUrlCreationTimer();
try {
    // Service logic
    metrics.incrementUrlCreation();
    return result;
} catch (Exception e) {
    metrics.incrementError();
    throw e;
} finally {
    metrics.recordUrlCreationTime(sample);
}
```

## Testing

### Test Coverage
- **RedisHealthIndicatorTest**: Tests Redis connectivity scenarios
- **DatabaseHealthIndicatorTest**: Tests database connectivity scenarios
- **UrlShortenerInfoContributorTest**: Tests application info generation
- **UrlShortenerMetricsTest**: Tests metrics collection and timing

### Test Scenarios Covered
1. **Health Indicators**:
   - Successful connections
   - Connection failures
   - Unexpected responses
   - Exception handling

2. **Metrics**:
   - Counter increments
   - Timer measurements
   - Multiple operations
   - Performance tracking

3. **Info Contributor**:
   - Normal operation
   - Database errors
   - Statistics generation

## Monitoring Best Practices

### Production Deployment
1. **Health Check Monitoring**: Set up alerts for health endpoint failures
2. **Metrics Collection**: Integrate with monitoring systems (Prometheus, Grafana)
3. **Log Aggregation**: Centralize logs for analysis and debugging
4. **Performance Thresholds**: Set alerts for response time degradation

### Key Metrics to Monitor
- **Availability**: Health check status
- **Performance**: Response times for URL operations
- **Usage**: URL creation and retrieval rates
- **Cache Efficiency**: Cache hit/miss ratios
- **Error Rates**: Error frequency and types

### Alerting Recommendations
- Redis or database connectivity failures
- High error rates (>5% of requests)
- Slow response times (>500ms for URL operations)
- Low cache hit ratios (<80%)
- High memory or CPU usage

## Requirements Fulfilled

### Requirement 5.3: Application Monitoring and Observability
✅ **Implemented**:
- Custom metrics for URL operations
- Performance timing measurements
- Cache efficiency tracking
- Error rate monitoring
- Structured logging throughout the application

### Requirement 5.5: Health Checks and Startup Verification
✅ **Implemented**:
- Redis connectivity health checks
- Database connectivity health checks
- Startup dependency verification
- Comprehensive health status reporting
- Automatic health check execution

## Future Enhancements

### Potential Improvements
1. **Advanced Metrics**: Add percentile measurements for response times
2. **Custom Dashboards**: Create Grafana dashboards for monitoring
3. **Alerting Integration**: Add Slack/email notifications for critical issues
4. **Performance Profiling**: Add detailed performance breakdowns
5. **Business Metrics**: Track URL popularity and usage patterns

### Scalability Considerations
- Metrics collection is lightweight and non-blocking
- Health checks have configurable timeouts
- Logging is asynchronous to avoid performance impact
- All monitoring components are stateless and thread-safe

This monitoring implementation provides a solid foundation for production deployment and operational visibility of the URL Shortener application.