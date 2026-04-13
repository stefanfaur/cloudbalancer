# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/security/JwtServiceTest.java

## Overview

`JwtServiceTest` is a unit test suite designed to validate the functionality and security integrity of the `JwtService` class within the `dispatcher` module. It ensures that JSON Web Tokens (JWTs) are correctly generated, parsed, and validated according to the system's security requirements.

The test suite covers:
- Successful token generation and claim extraction.
- Validation logic for token expiration.
- Integrity checks against tampered tokens.
- Role-based access control (RBAC) claim verification.

## Public API

The `JwtServiceTest` class provides the following test methods:

- `setUp()`: Initializes the `JwtService` instance with a predefined secret key and expiration settings before each test execution.
- `generateTokenContainsExpectedClaims()`: Verifies that a generated token correctly encodes the username and role, and that these can be accurately retrieved.
- `expiredTokenIsInvalid()`: Confirms that tokens with a zero-second lifespan are correctly identified as invalid.
- `tamperedTokenIsInvalid()`: Ensures that any modification to the token string results in a validation failure.
- `differentRolesProduceDifferentClaims()`: Validates that distinct roles (e.g., `ADMIN` vs `VIEWER`) are correctly mapped and isolated within their respective tokens.

## Dependencies

- **JUnit 5 (Jupiter)**: Used for the test framework and lifecycle management (`@Test`, `@BeforeEach`).
- **AssertJ**: Used for fluent assertion syntax (`assertThat`).
- **com.cloudbalancer.common.model.Role**: The domain model representing user roles used in token claims.
- **com.cloudbalancer.dispatcher.security.JwtService**: The target class under test.

## Usage Notes

- **Secret Key**: The tests utilize a hardcoded base64-encoded 256-bit secret key. Ensure that any changes to the `JwtService` constructor signature are reflected in the `setUp` method.
- **Environment**: These tests are designed to run in a standard JVM environment and do not require a Spring context, making them fast and suitable for CI/CD pipelines.
- **Test Isolation**: Each test case instantiates or utilizes a `JwtService` instance to ensure that state (such as expiration settings) does not leak between test methods.