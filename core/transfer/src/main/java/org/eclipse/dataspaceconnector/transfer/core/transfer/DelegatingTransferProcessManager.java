package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.util.concurrent.CompletableFuture;

/**
 * This transfer process managers delegates {@link DataRequest} objects out to either the {@link SyncTransferProcessManager} or the
 * {@link AsyncTransferProcessManager} depending to whether the {@link DataRequest#isSync()} flag is {@code true} or {@code false}.
 */
public class DelegatingTransferProcessManager implements TransferProcessManager {
    private final AsyncTransferProcessManager asyncManager;
    private final SyncTransferProcessManager syncManager;

    public DelegatingTransferProcessManager(AsyncTransferProcessManager asyncManager, SyncTransferProcessManager syncManager) {
        this.asyncManager = asyncManager;
        this.syncManager = syncManager;
    }

    @Override
    public TransferInitiateResult initiateConsumerRequest(DataRequest dataRequest) {
        return dataRequest.isSync() ? syncManager.initiateConsumerRequest(dataRequest) : asyncManager.initiateConsumerRequest(dataRequest);
    }

    @Override
    public TransferInitiateResult initiateProviderRequest(DataRequest dataRequest) {
        return dataRequest.isSync() ? syncManager.initiateProviderRequest(dataRequest) : asyncManager.initiateProviderRequest(dataRequest);
    }

    @Override
    public CompletableFuture<Void> transitionRequestAck(String processId) {
        // TODO
        return null;
    }

    @Override
    public void transitionProvisioned(String processId) {

    }

    @Override
    public void transitionError(String processId, String detail) {

    }

    @Override
    public Result<TransferProcessStates> deprovision(String processId) {
        return null;
    }


    public void start(TransferProcessStore transferProcessStore) {
        asyncManager.start(transferProcessStore);
    }

    public void stop() {
        asyncManager.stop();
    }
}
