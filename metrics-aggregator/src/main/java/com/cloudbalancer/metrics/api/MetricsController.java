package com.cloudbalancer.metrics.api;

import com.cloudbalancer.metrics.api.dto.ClusterMetrics;
import com.cloudbalancer.metrics.api.dto.WorkerMetricsBucket;
import com.cloudbalancer.metrics.api.dto.WorkerMetricsSnapshot;
import com.cloudbalancer.metrics.service.MetricsAggregationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final Pattern BUCKET_PATTERN = Pattern.compile("^(\\d+)([mh])$");

    private final MetricsAggregationService aggregationService;

    public MetricsController(MetricsAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/workers")
    public List<WorkerMetricsSnapshot> getLatestMetrics() {
        return aggregationService.getLatestPerWorker();
    }

    @GetMapping("/workers/{id}/history")
    public List<WorkerMetricsBucket> getWorkerHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "1m") String bucket) {

        Instant fromInstant = from.isEmpty() ? Instant.now().minus(1, ChronoUnit.HOURS) : Instant.parse(from);
        Instant toInstant = to.isEmpty() ? Instant.now() : Instant.parse(to);
        int bucketMinutes = parseBucketToMinutes(bucket);

        return aggregationService.getWorkerHistory(id, fromInstant, toInstant, bucketMinutes);
    }

    @GetMapping("/cluster")
    public ClusterMetrics getClusterMetrics() {
        return aggregationService.getClusterMetrics();
    }

    private int parseBucketToMinutes(String bucket) {
        Matcher m = BUCKET_PATTERN.matcher(bucket);
        if (!m.matches()) {
            return 1;
        }
        int value = Integer.parseInt(m.group(1));
        String unit = m.group(2);
        return switch (unit) {
            case "h" -> value * 60;
            case "m" -> value;
            default -> 1;
        };
    }
}
