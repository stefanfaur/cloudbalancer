# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/security/JwtService.java

## Overview

The `JwtService` is a Spring-managed service located in the `metrics-aggregator` module. It serves as the primary security component for handling JSON Web Tokens (JWT). Its core responsibilities include validating incoming tokens, extracting identity claims (username and role), and providing utility methods for token generation during testing.

**Note:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). As a critical security component, any modifications to this class carry a high risk of introducing authentication or authorization vulnerabilities.

## Public API

| Method | Description |
| :--- | :--- |
| `extractUsername(String token)` | Parses the token and returns the subject (username). |
| `extractRole(String token)` | Parses the token and returns the associated `Role` enum. |
| `isTokenValid(String token)` | Validates the signature and checks if the token expiration date is in the future. |
| `generateAccessToken(String, Role)` | Generates a valid JWT for testing purposes. |
| `generateExpiredToken(String, Role)` | Generates a JWT with an expired timestamp for testing purposes. |

## Dependencies

- **`com.cloudbalancer.common.model.Role`**: Used for type-safe role extraction.
- **`io.jsonwebtoken` (jjwt)**: The underlying library used for JWT signing, parsing, and claim management.
- **`javax.crypto.SecretKey`**: Used to store the HMAC-SHA key derived from the application configuration.
- **Spring Framework**: Utilizes `@Service` for component scanning and `@Value` for configuration injection.

## Usage Notes

### Configuration
The service requires the following properties to be defined in the application environment:
- `cloudbalancer.security.jwt-secret`: A Base64-encoded string used as the HMAC-SHA signing key.
- `cloudbalancer.security.access-token-expiration-seconds`: (Optional) The duration in seconds for which a generated token remains valid. Defaults to `900` (15 minutes).

### Security Implementation
- **Parsing**: The `parseClaims` method is the internal engine for all operations. It uses the `Jwts.parser()` builder to verify the signature against the `signingKey`. If the signature is invalid or the token is malformed, a `JwtException` is thrown.
- **Validation**: The `isTokenValid` method wraps the parsing logic in a `try-catch` block. It returns `false` if the token is expired, improperly signed, or structurally invalid, ensuring that the application does not crash on malicious or malformed input.

### Hotspot Warning
Given its status as a high-activity file, developers should:
1. **Regression Testing**: Always run existing test suites when modifying token parsing logic.
2. **Exception Handling**: Ensure that any changes to `parseClaims` do not inadvertently expose stack traces or internal details to the caller.
3. **Dependency Updates**: When updating the `jjwt` library, verify that the builder patterns (e.g., `Jwts.parser().verifyWith(...)`) remain compatible with the current implementation.

### Example Usage
```java
@Autowired
private JwtService jwtService;

public void processRequest(String token) {
    if (jwtService.isTokenValid(token)) {
        String username = jwtService.extractUsername(token);
        Role role = jwtService.extractRole(token);
        // Proceed with authorized logic
    }
}
```