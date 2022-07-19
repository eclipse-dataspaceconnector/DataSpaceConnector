/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering, Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation, refactoring
 *       Daimler TSS GmbH - introduce factory to create RequestInProcessMessage
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.ContractOfferMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.messageTypeNotSupported;

/**
 * This class handles and processes incoming IDS {@link ContractOfferMessage}s.
 */
public class ContractOfferHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final String connectorId;
    private final ProviderContractNegotiationManager providerNegotiationManager;
    private final ConsumerContractNegotiationManager consumerNegotiationManager;

    public ContractOfferHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ProviderContractNegotiationManager providerNegotiationManager,
            @NotNull ConsumerContractNegotiationManager consumerNegotiationManager) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.providerNegotiationManager = Objects.requireNonNull(providerNegotiationManager);
        this.consumerNegotiationManager = Objects.requireNonNull(consumerNegotiationManager);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractOfferMessage;
    }

    @Override
    public @NotNull MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        var message = (ContractOfferMessage) multipartRequest.getHeader();

        ContractOffer contractOffer = null;
        try {
            contractOffer = objectMapper.readValue(multipartRequest.getPayload(), ContractOffer.class);
        } catch (IOException e) {
            monitor.severe("ContractOfferHandler: Contract Offer is invalid", e);
            return createMultipartResponse(badParameters(message, connectorId));
        }
    
        // TODO similar implementation to ContractRequestHandler (only required if counter offers supported, not needed for M1)
        return createMultipartResponse(messageTypeNotSupported(message, connectorId));
    }
}
