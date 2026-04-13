# Symbol: web-dashboard.src.api.types.ResourceProfile

## Purpose

The `ResourceProfile` interface defines the standardized hardware and runtime requirements for tasks or workers within the system. It acts as a central contract for resource allocation, ensuring that components across the dashboard—from task submission forms to cluster monitoring views—have a consistent schema for describing compute needs.

## Signature

```typescript
interface ResourceProfile
```

## Parameters

| Property | Type | Description |
| :--- | :--- | :--- |
| `cpuCores` | `number` | The number of CPU cores required for the process. |
| `memoryMB` | `number` | The amount of RAM required, measured in megabytes. |
| `diskMB` | `number` | The amount of persistent or ephemeral disk storage required, in megabytes. |
| `gpuRequired` | `boolean` | Indicates whether the workload requires GPU acceleration. |
| `estimatedDurationSeconds` | `number` | The expected execution time for the task, used for scheduling and cost estimation. |
| `networkAccessRequired` | `boolean` | Indicates if the workload requires external network connectivity. |

## Returns

*N/A (This is an interface definition, not a function.)*

## Example Usage

```typescript
import { ResourceProfile } from 'web-dashboard/src/api/types';

const standardTaskProfile: ResourceProfile = {
  cpuCores: 2,
  memoryMB: 4096,
  diskMB: 10240,
  gpuRequired: false,
  estimatedDurationSeconds: 300,
  networkAccessRequired: true
};

function submitTask(profile: ResourceProfile) {
  console.log(`Submitting task requiring ${profile.cpuCores} cores...`);
  // API call logic here
}
```