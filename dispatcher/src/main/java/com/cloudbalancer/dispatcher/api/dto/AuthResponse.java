package com.cloudbalancer.dispatcher.api.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}
