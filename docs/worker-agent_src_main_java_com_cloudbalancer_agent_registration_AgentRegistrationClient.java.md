# File: worker-agent/src/main/java/com/cloudbalancer/agent/registration/AgentRegistrationClient.java

## Overview

The `AgentRegistrationClient` is a critical component of the `worker-agent` responsible for establishing a connection between the worker node and the central CloudBalancer dispatcher. It handles the initial handshake, resource reporting, and retrieval of Kafka connection credentials required for the agent to participate in the cluster.

**Note**: This file is a **HOTSPOT** within the repository. It exhibits high change frequency and significant complexity due to its role in network communication, retry logic, and process lifecycle management. Modifications to this class should be approached with caution, as errors here can prevent agents from joining the cluster or cause cascading failures.

## Public API

### `AgentRegistrationClient(AgentProperties props, ObjectMapper objectMapper)`
Constructs the client using the provided configuration properties and JSON mapper.

### `RegistrationResult register()`
Performs the registration process. 
- Sends a POST request to the dispatcher's `/api/agents/register` endpoint.
- Implements an exponential backoff retry strategy (up to 10 attempts).
- If successful, returns a `RegistrationResult` containing Kafka bootstrap servers and credentials.
- If the registration fails (e.g., 401 Unauthorized or exhaustion of retries), the method triggers a `System.exit(1)` to terminate the agent process.

### `RegistrationResult getCachedResult()`
Returns the `RegistrationResult` object cached from the most recent successful `register()` call. Returns `null` if registration has not yet occurred.

## Dependencies

- **`com.cloudbalancer.agent.config.AgentProperties`**: Used to retrieve the registration token, dispatcher URL, and hardware resource specifications.
- **`com.fasterxml.jackson.databind.ObjectMapper`**: Used for serializing registration requests and deserializing JSON responses from the dispatcher.
- **`java.net.http.HttpClient`**: Used for performing the synchronous HTTP communication with the dispatcher.

## Usage Notes

### Registration Lifecycle
The `register()` method is intended to be called during the agent startup sequence. Because it is a blocking operation with retry logic, it effectively acts as a gatekeeper for the agent's lifecycle.

### Error Handling & Exit Strategy
- **Unauthorized Access**: If the dispatcher returns a `401`, the client assumes the token is invalid or revoked and terminates the process immediately.
- **Retry Logic**: The client uses an exponential backoff starting at 1 second, doubling each time up to a maximum of 60 seconds per interval, for a total of 10 attempts.
- **Local Mode**: If the `registrationToken` is missing or blank in the `AgentProperties`, the client skips registration and returns `null`, allowing the agent to run in "local mode."

### Example Usage
```java
// Injecting the client into a service
@Autowired
private AgentRegistrationClient registrationClient;

public void initializeAgent() {
    // Perform registration
    AgentRegistrationClient.RegistrationResult result = registrationClient.register();
    
    if (result != null) {
        log.info("Agent connected to Kafka: {}", result.kafkaBootstrap());
    } else {
        log.info("Agent running in local mode.");
    }
}
```

### Potential Pitfalls
1. **Blocking Behavior**: Since `register()` is synchronous and includes `Thread.sleep()` for retries, calling this on the main application thread will delay agent startup.
2. **Process Termination**: The use of `System.exit(1)` inside the client makes it difficult to unit test without mocking the environment or handling the exit signal. Ensure that any calling code is aware that this method may terminate the JVM.
3. **Memory/CPU Reporting**: The registration payload relies on `props.getTotalCpuCores()` and `props.getTotalMemoryMb()`. Ensure these properties are correctly populated before calling `register()`, otherwise, the dispatcher may receive inaccurate resource data.