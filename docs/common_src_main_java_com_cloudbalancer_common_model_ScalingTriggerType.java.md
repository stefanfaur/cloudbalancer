# File: common/src/main/java/com/cloudbalancer/common/model/ScalingTriggerType.java

## Overview

The `ScalingTriggerType` enum defines the operational modes available for initiating scaling events within the CloudBalancer infrastructure. It categorizes the origin or logic behind a scaling decision, allowing the system to differentiate between automated, metric-driven, and administrative interventions.

## Public API

### `ScalingTriggerType` (Enum)

| Constant | Description |
| :--- | :--- |
| `REACTIVE` | Scaling triggered automatically based on real-time system metrics (e.g., CPU or memory utilization). |
| `QUEUE_PRESSURE` | Scaling triggered by monitoring the depth or latency of incoming request queues. |
| `MANUAL` | Scaling initiated directly by an administrator or external orchestration tool. |

## Dependencies

This enum is a standalone component within the `com.cloudbalancer.common.model` package and does not depend on any external libraries or internal project classes.

## Usage Notes

*   **Integration**: This enum is primarily utilized by the `ScalingTriggerRequest` DTO in the `dispatcher` module to categorize incoming scaling requests.
*   **Extensibility**: When adding new trigger mechanisms (e.g., `SCHEDULED` or `PREDICTIVE`), ensure that all downstream consumers—such as the `ScalingAction` type definitions in the web dashboard—are updated to handle the new enum constant.
*   **Persistence**: When serializing this enum to JSON (e.g., for API communication), it is represented by its string name (e.g., `"REACTIVE"`). Ensure that API consumers are configured to handle these specific string values.