package com.banula.navigationservice.listener;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.banula.navigationservice.client.CdrAdapterClient;
import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.event.SmartLocationsChangedEvent;
import com.banula.openlib.ocpi.exception.OCPICustomException;

class SmartLocationsChangedListenerTest {

    @Mock
    private CdrAdapterClient cdrAdapterClient;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    private SmartLocationsChangedListener listener;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        listener = new SmartLocationsChangedListener(cdrAdapterClient, applicationConfiguration);
    }

    @AfterEach
    void tearDown() throws Exception {
        listener.destroy();
        mocks.close();
    }

    @Test
    void shouldUpdateCdrAdapterOnChange() {
        when(applicationConfiguration.getCdrAdapterSyncEnabled()).thenReturn(true);

        listener.onSmartLocationsChanged(new SmartLocationsChangedEvent(this));

        verify(cdrAdapterClient, timeout(2000)).updateSmartLocations();
    }

    @Test
    void shouldSkipWhenSyncDisabled() {
        when(applicationConfiguration.getCdrAdapterSyncEnabled()).thenReturn(false);

        listener.onSmartLocationsChanged(new SmartLocationsChangedEvent(this));

        verify(cdrAdapterClient, never()).updateSmartLocations();
    }

    @Test
    void shouldCoalesceABurstOfChangesIntoASingleFollowUpUpdate() throws Exception {
        when(applicationConfiguration.getCdrAdapterSyncEnabled()).thenReturn(true);
        CountDownLatch firstCallStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstCall = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstCallStarted.countDown();
            releaseFirstCall.await(5, TimeUnit.SECONDS);
            return null;
        }).when(cdrAdapterClient).updateSmartLocations();

        listener.onSmartLocationsChanged(new SmartLocationsChangedEvent(this));
        assertTrue(firstCallStarted.await(5, TimeUnit.SECONDS), "first update never started");

        for (int i = 0; i < 500; i++) {
            listener.onSmartLocationsChanged(new SmartLocationsChangedEvent(this));
        }
        releaseFirstCall.countDown();

        // 500 writes must not become 500 full re-pulls on the CDR Adapter: one run was
        // in flight, exactly one more is queued behind the whole burst.
        verify(cdrAdapterClient, timeout(5000).times(2)).updateSmartLocations();
    }

    @Test
    void shouldKeepWorkingAfterAFailedUpdate() {
        when(applicationConfiguration.getCdrAdapterSyncEnabled()).thenReturn(true);
        doThrow(new OCPICustomException("boom")).when(cdrAdapterClient).updateSmartLocations();

        listener.onSmartLocationsChanged(new SmartLocationsChangedEvent(this));
        verify(cdrAdapterClient, timeout(5000)).updateSmartLocations();

        listener.onSmartLocationsChanged(new SmartLocationsChangedEvent(this));
        verify(cdrAdapterClient, timeout(5000).atLeast(2)).updateSmartLocations();
    }
}
