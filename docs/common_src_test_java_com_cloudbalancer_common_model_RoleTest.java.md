# File: common/src/test/java/com/cloudbalancer/common/model/RoleTest.java

## Overview

`RoleTest` is a unit test suite located in the `common` module. It validates the integrity and serialization behavior of the `Role` enumeration, which defines the access levels within the CloudBalancer system. The tests ensure that the enum contains the expected set of roles and that it correctly integrates with the system's JSON serialization utilities.

## Public API

### `RoleTest`

| Method | Description |
| :--- | :--- |
| `roleEnumHasFourValues()` | Verifies that the `Role` enum contains exactly four defined constants: `ADMIN`, `OPERATOR`, `VIEWER`, and `API_CLIENT`. |
| `roleSerializesAsString()` | Validates that `Role` constants are serialized to and deserialized from their string representations using `JsonUtil`. |

## Dependencies

- **JUnit 5 (Jupiter)**: Used for the `@Test` annotation and test execution framework.
- **AssertJ**: Used for fluent assertions (`assertThat`).
- **`com.cloudbalancer.common.util.JsonUtil`**: Used to verify the JSON serialization/deserialization contract for the `Role` enum.
- **`com.cloudbalancer.common.model.Role`**: The target enumeration being tested.

## Usage Notes

- This test suite serves as a contract for the `Role` enum. Any modification to the available roles (adding or removing constants) will require an update to `roleEnumHasFourValues`.
- The `roleSerializesAsString` test ensures that the `Role` enum remains compatible with the system's standard JSON configuration. If the serialization format of the `Role` enum is changed (e.g., to use integer IDs instead of string names), this test will fail and must be updated to reflect the new serialization strategy.
- These tests are essential for maintaining consistency across the distributed components of CloudBalancer, as the `Role` enum is shared across multiple services.