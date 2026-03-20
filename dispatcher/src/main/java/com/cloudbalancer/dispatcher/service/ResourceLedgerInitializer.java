package com.cloudbalancer.dispatcher.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ResourceLedgerInitializer {

    private static final Logger log = LoggerFactory.getLogger(ResourceLedgerInitializer.class);
    private final WorkerRegistryService workerRegistryService;

    public ResourceLedgerInitializer(WorkerRegistryService workerRegistryService) {
        this.workerRegistryService = workerRegistryService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Application ready — rebuilding resource ledger");
        workerRegistryService.rebuildResourceLedger();
    }
}
