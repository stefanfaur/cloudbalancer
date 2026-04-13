# File: common/src/main/java/com/cloudbalancer/common/model/ScalingDecision.java

## Overview

The `ScalingDecision` class is an immutable data carrier (Java `record`) used within the CloudBalancer system to represent a finalized decision regarding infrastructure scaling. It encapsulates the details of a scaling event, including the nature of the action taken, the justification for the change, the triggering mechanism, and the state transition of the worker count.

## Public API

### Constructors
*   `ScalingDecision(ScalingAction action, String reason, ScalingTriggerType triggerType, int previousWorkerCount, int newWorkerCount, Instant timestamp)`: Constructs a new `ScalingDecision` record.

### Fields
*   `action` (`ScalingAction`): The type of scaling operation performed (e.g., SCALE_UP, SCALE_DOWN).
*   `reason` (`String`): A descriptive string explaining why the scaling decision was triggered.
*   `triggerType` (`ScalingTriggerType`): The source or policy that initiated the scaling event.
*   `previousWorkerCount` (`int`): The number of active workers prior to the scaling action.
*   `newWorkerCount` (`int`): The target number of workers after the scaling action.
*   `timestamp` (`Instant`): The precise time at which the decision was recorded.

## Dependencies

*   `java.time.Instant`: Used to provide a high-precision timestamp for the scaling event.
*   `com.cloudbalancer.common.model.ScalingAction` (Implicit): Defines the set of possible scaling operations.
*   `com.cloudbalancer.common.model.ScalingTriggerType` (Implicit): Defines the categories of triggers for scaling.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once a `ScalingDecision` is instantiated, its state cannot be modified.
*   **Logging and Auditing**: This class is primarily intended for use in audit logs, event streams, or monitoring dashboards to track the history of infrastructure adjustments.
*   **Validation**: Ensure that `previousWorkerCount` and `newWorkerCount` are non-negative integers when constructing this record, as the class does not perform internal validation logic.