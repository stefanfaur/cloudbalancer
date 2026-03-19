package com.cloudbalancer.common.model;

import java.util.List;

public record TaskIO(
    List<InputArtifact> inputs,
    List<OutputArtifact> outputs
) {
    public record InputArtifact(String name, String location, ArtifactSource source) {}
    public record OutputArtifact(String name, String path) {}
    public enum ArtifactSource { HTTP, OBJECT_STORAGE, INLINE }

    public static TaskIO none() {
        return new TaskIO(List.of(), List.of());
    }
}
