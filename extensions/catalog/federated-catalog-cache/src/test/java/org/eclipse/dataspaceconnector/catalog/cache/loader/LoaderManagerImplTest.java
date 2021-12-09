package org.eclipse.dataspaceconnector.catalog.cache.loader;

import org.eclipse.dataspaceconnector.catalog.spi.Loader;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.WaitStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoaderManagerImplTest {

    private LoaderManagerImpl loaderManager;
    private BlockingQueue<UpdateResponse> queue;
    private Loader loaderMock;
    private WaitStrategy waitStrategyMock;

    @BeforeEach
    void setup() {
        waitStrategyMock = mock(WaitStrategy.class);
        int batchSize = 3;
        queue = new ArrayBlockingQueue<>(batchSize); //default batch size of the loader
        loaderMock = mock(Loader.class);
        loaderManager = new LoaderManagerImpl(Collections.singletonList(loaderMock), batchSize, waitStrategyMock, mock(Monitor.class));
    }

    @Test
    @DisplayName("Verify that the loader manager waits one pass when the queue does not yet contain sufficient elements")
    void batchSizeNotReachedWithinTimeframe() throws InterruptedException {
        for (var i = 0; i < loaderManager.getBatchSize() - 1; i++) {
            queue.offer(new UpdateResponse());
        }
        var completionSignal = new CountDownLatch(1);

        // set the completion signal when the wait strategy was called
        when(waitStrategyMock.retryInMillis()).thenAnswer(i -> {
            completionSignal.countDown();
            return 10L;
        });

        loaderManager.start(queue);

        assertThat(completionSignal.await(20L, TimeUnit.MILLISECONDS)).isTrue();
    }

    @Test
    @DisplayName("Verify that the LoaderManager does not sleep when a complete batch was processed")
    void batchSizeReachedWithinTimeframe() throws InterruptedException {
        for (var i = 0; i < loaderManager.getBatchSize(); i++) {
            queue.offer(new UpdateResponse());
        }
        var completionSignal = new CountDownLatch(1);

        // set the completion signal when the wait strategy was called
        doAnswer(i -> {
            completionSignal.countDown();
            return null;
        }).when(waitStrategyMock).success();

        loaderManager.start(queue);

        //wait for completion signal
        assertThat(completionSignal.await(5, TimeUnit.SECONDS)).isTrue();

        verify(loaderMock, times(1)).load(any());
    }

}