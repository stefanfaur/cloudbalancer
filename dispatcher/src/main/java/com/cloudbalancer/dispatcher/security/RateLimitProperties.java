package com.cloudbalancer.dispatcher.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudbalancer.security.rate-limit")
public class RateLimitProperties {
    private int admin = 200;
    private int operator = 100;
    private int viewer = 60;
    private int apiClient = 120;
    private int anonymous = 10;

    public int getAdmin() { return admin; }
    public void setAdmin(int admin) { this.admin = admin; }
    public int getOperator() { return operator; }
    public void setOperator(int operator) { this.operator = operator; }
    public int getViewer() { return viewer; }
    public void setViewer(int viewer) { this.viewer = viewer; }
    public int getApiClient() { return apiClient; }
    public void setApiClient(int apiClient) { this.apiClient = apiClient; }
    public int getAnonymous() { return anonymous; }
    public void setAnonymous(int anonymous) { this.anonymous = anonymous; }

    public int getLimitForRole(String role) {
        return switch (role) {
            case "ROLE_ADMIN" -> admin;
            case "ROLE_OPERATOR" -> operator;
            case "ROLE_VIEWER" -> viewer;
            case "ROLE_API_CLIENT" -> apiClient;
            default -> anonymous;
        };
    }
}
