package com.banula.navigationservice.listener;

import com.banula.navigationservice.client.CdrAdapterClient;
import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.event.SmartLocationsChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pushes every smart location change through to the CDR Adapter, so its mirror never
 * waits for the daily re-pull or for someone to press "Update From NSP".
 */
@Slf4j
@Component
public class SmartLocationsChangedListener implements DisposableBean {

    private final CdrAdapterClient cdrAdapterClient;
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * The CDR Adapter endpoint re-pulls the whole smart location set, so a burst of
     * writes (a 1000-row bulk import, a full location pull from a CPO) must not become
     * one re-pull per row. At most one update runs and one more is queued behind it:
     * any number of changes collapses into a single follow-up run, and because the flag
     * is cleared before the call, a change arriving mid-run still queues its own.
     */
    private final AtomicBoolean queued = new AtomicBoolean(false);

    private final ExecutorService executor;

    public SmartLocationsChangedListener(CdrAdapterClient cdrAdapterClient,
            ApplicationConfiguration applicationConfiguration) {
        this.cdrAdapterClient = cdrAdapterClient;
        this.applicationConfiguration = applicationConfiguration;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "nsp-cdr-adapter-sync");
            thread.setDaemon(true);
            return thread;
        });
    }

    @EventListener
    public void onSmartLocationsChanged(SmartLocationsChangedEvent event) {
        if (!Boolean.TRUE.equals(applicationConfiguration.getCdrAdapterSyncEnabled())) {
            return;
        }
        if (!queued.compareAndSet(false, true)) {
            return;
        }
        executor.execute(this::updateCdrAdapter);
    }

    private void updateCdrAdapter() {
        queued.set(false);
        try {
            cdrAdapterClient.updateSmartLocations();
        } catch (Exception e) {
            log.warn("Could not mirror smart location changes to the CDR Adapter: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
