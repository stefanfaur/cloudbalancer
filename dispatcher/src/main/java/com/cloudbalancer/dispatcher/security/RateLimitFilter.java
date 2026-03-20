package com.cloudbalancer.dispatcher.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitProperties rateLimitProperties;

    public RateLimitFilter(RateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String bucketKey;
        int limit;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            bucketKey = "user:" + auth.getName();
            String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ANONYMOUS");
            limit = rateLimitProperties.getLimitForRole(role);
        } else {
            bucketKey = "ip:" + request.getRemoteAddr();
            limit = rateLimitProperties.getAnonymous();
        }

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(limit));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }

    private Bucket createBucket(int tokensPerMinute) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(tokensPerMinute)
                .refillGreedy(tokensPerMinute, Duration.ofMinutes(1))
                .build())
            .build();
    }
}
