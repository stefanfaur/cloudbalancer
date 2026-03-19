package com.cloudbalancer.common.executor;

import java.util.List;

public record ValidationResult(boolean valid, List<String> errors) {
    public static ValidationResult ok() { return new ValidationResult(true, List.of()); }
    public static ValidationResult invalid(String... errors) { return new ValidationResult(false, List.of(errors)); }
}
