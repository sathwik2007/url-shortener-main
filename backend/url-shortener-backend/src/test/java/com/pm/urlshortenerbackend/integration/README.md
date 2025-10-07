# Integration Tests for URL Shortener MVP

This directory contains comprehensive integration tests that cover the complete flow of the URL shortener application as specified in task 10.1.

## Test Coverage

### 1. UrlShortenerIntegrationTest.java
**Complete end-to-end flow testing with TestContainers**

- **Requirements Covered**: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2
- **Features Tested**:
  - Complete flow from URL creation to redirect
  - URL validation and error handling
  - Redis caching behavior
  - Database persistence
  - Duplicate URL handling
  - System resilience

**Key Test Methods**:
- `testCompleteFlow_CreateAndRedirect_Success()` - Tests the full user journey
- `testCompleteFlow_InvalidUrl_ErrorHandling()` - Tests validation errors
- `testCompleteFlow_InvalidShortCode_NotFound()` - Tests 404 responses
- `testCompleteFlow_DuplicateUrl_ReturnExisting()` - Tests duplicate handling

### 2. RedisCachingIntegrationTest.java
**Focused Redis caching integration testing**

- **Requirements Covered**: 3.1, 3.2
- **Features Tested**:
  - Cache-aside pattern implementation
  - Cache hit/miss scenarios
  - Cache TTL and expiration
  - Cache fallback mechanisms
  - Performance characteristics
  - Concurrent access patterns

**Key Test Methods**:
- `testCacheAsidePattern_CacheMiss_LoadFromDatabase()` - Tests cache miss behavior
- `testCacheAsidePattern_CacheHit_SkipDatabase()` - Tests cache hit optimization
- `testCacheTTL_ExpirationBehavior()` - Tests cache expiration
- `testCacheConcurrency_MultipleThreads()` - Tests concurrent access

### 3. ErrorConditionsIntegrationTest.java
**Comprehensive error handling and edge case testing**

- **Requirements Covered**: 1.2, 2.2, 5.4
- **Features Tested**:
  - Invalid URL formats and protocols
  - Malformed requests
  - System resilience under load
  - Concurrent operations
  - Edge cases (very long URLs, special characters)
  - Resource exhaustion scenarios

**Key Test Methods**:
- `testInvalidUrl_MalformedUrl()` - Tests various invalid URL formats
- `testConcurrentUrlCreation_SameUrl()` - Tests concurrent duplicate creation
- `testSystemResilience_DatabaseRecovery()` - Tests system recovery
- `testEdgeCases_VeryLongUrl()` - Tests boundary conditions

## TestContainers Integration

All integration tests use TestContainers to provide:
- **PostgreSQL 15**: Real database for persistence testing
- **Redis 7**: Real cache for caching behavior testing
- **Isolated Environment**: Each test runs with fresh containers
- **Realistic Testing**: Tests against actual database and cache implementations

## Configuration

Tests are configured with:
- Dynamic port allocation for containers
- Automatic schema creation/cleanup
- Configurable TTL settings
- CORS configuration for frontend integration

## Running the Tests

### Prerequisites
- Docker must be running on the system
- Java 17+
- Maven 3.6+

### Commands
```bash
# Run all integration tests
mvn test -Dtest="*IntegrationTest"

# Run specific test class
mvn test -Dtest=UrlShortenerIntegrationTest

# Run specific test method
mvn test -Dtest=UrlShortenerIntegrationTest#testCompleteFlow_CreateAndRedirect_Success
```

### Test Execution Flow
1. TestContainers starts PostgreSQL and Redis containers
2. Spring Boot application context loads with test configuration
3. Database schema is created automatically
4. Tests execute with real database and cache
5. Containers are automatically cleaned up after tests

## Performance Considerations

The integration tests include performance validation:
- Cache effectiveness measurements
- Response time comparisons (cache vs database)
- Concurrent access testing
- Load testing with multiple URLs

## Error Scenarios Covered

1. **URL Validation Errors**:
   - Malformed URLs
   - Invalid protocols
   - URLs exceeding length limits
   - Empty/null URLs

2. **System Errors**:
   - Database connectivity issues
   - Redis unavailability
   - Concurrent access conflicts
   - Resource exhaustion

3. **Edge Cases**:
   - Very long URLs (near 2048 character limit)
   - URLs with special characters and Unicode
   - Single character short codes
   - Maximum length short codes

## Integration with Existing Tests

These integration tests complement the existing unit tests:
- Unit tests focus on individual component behavior
- Integration tests verify component interactions
- End-to-end tests validate complete user workflows
- Performance tests ensure scalability requirements

## Maintenance Notes

- Tests use @Transactional for automatic rollback
- TestContainers handles container lifecycle automatically
- Dynamic property configuration ensures no port conflicts
- Comprehensive cleanup ensures test isolation