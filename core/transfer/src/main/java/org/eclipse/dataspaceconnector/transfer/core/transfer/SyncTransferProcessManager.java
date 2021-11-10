package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferResponse;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.net.ConnectException;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;

public class SyncTransferProcessManager implements TransferProcessManager {

    private final DataProxyManager dataProxyManager;
    private final TransferProcessStore transferProcessStore;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public SyncTransferProcessManager(DataProxyManager dataProxyManager, TransferProcessStore transferProcessStore, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.dataProxyManager = dataProxyManager;
        this.transferProcessStore = transferProcessStore;
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @Override
    public TransferResponse initiateConsumerRequest(DataRequest dataRequest) {
        var id = UUID.randomUUID().toString();
        var transferProcess = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).state(TransferProcessStates.COMPLETED.code()).type(CONSUMER).build();
        transferProcessStore.create(transferProcess);

        var future = dispatcherRegistry.send(Object.class, dataRequest, transferProcess::getId);
        try {
            var result = future.join();
            // reload from store, could have been modified
            transferProcess = transferProcessStore.find(transferProcess.getId());

            if (transferProcess.getState() == TransferProcessStates.ERROR.code()) {
                return TransferResponse.Builder.newInstance().error(transferProcess.getErrorDetail()).id(dataRequest.getId()).status(ResponseStatus.FATAL_ERROR).build();
            }

            // if there is a handler for this particular transfer type, return the result of this handler, otherwise return the
            // raw proxy object
            
            return TransferResponse.Builder.newInstance().data(result).id(dataRequest.getId()).status(ResponseStatus.OK).build();
        } catch (Exception ex) {
            var status = isRetryable(ex.getCause()) ? ResponseStatus.ERROR_RETRY : ResponseStatus.FATAL_ERROR;
            return TransferResponse.Builder.newInstance().id(dataRequest.getId()).status(status).error(ex.getMessage()).build();
        }
    }

    @Override
    public TransferResponse initiateProviderRequest(DataRequest dataRequest) {
        //create a transfer process in the COMPLETED state
        var id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).state(TransferProcessStates.COMPLETED.code()).type(PROVIDER).build();
        transferProcessStore.create(process);

        var dataProxy = dataProxyManager.getProxy(dataRequest);
        if (dataProxy != null) {
            var proxyData = dataProxy.getData(dataRequest);
            return TransferResponse.Builder.newInstance().id(process.getId()).data(proxyData).status(ResponseStatus.OK).build();
        }
        return TransferResponse.Builder.newInstance().id(process.getId()).status(ResponseStatus.FATAL_ERROR).build();
    }

    private boolean isRetryable(Throwable ex) {
        if (ex instanceof ConnectException) { //we might need to add more retryable exceptions
            return true;
        }
        return false;
    }

}
