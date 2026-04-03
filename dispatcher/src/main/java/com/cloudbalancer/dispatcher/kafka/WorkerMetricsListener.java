package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.WorkerMetricsEvent;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.service.AutoScalerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WorkerMetricsListener {

    private static final Logger log = LoggerFactory.getLogger(WorkerMetricsListener.class);
    private final AutoScalerService autoScalerService;

    public WorkerMetricsListener(AutoScalerService autoScalerService) {
        this.autoScalerService = autoScalerService;
    }

    @KafkaListener(topics = "workers.metrics", groupId = "dispatcher-autoscaler")
    public void onMetrics(String message) {
        try {
            WorkerMetricsEvent event = JsonUtil.mapper().readValue(message, WorkerMetricsEvent.class);
            autoScalerService.recordMetrics(event.workerId(), event.metrics().cpuUsagePercent());
            log.debug("Recorded metrics from worker {}: CPU {}%",
                event.workerId(), event.metrics().cpuUsagePercent());
        } catch (Exception e) {
            log.error("Failed to process worker metrics", e);
        }
    }
}
