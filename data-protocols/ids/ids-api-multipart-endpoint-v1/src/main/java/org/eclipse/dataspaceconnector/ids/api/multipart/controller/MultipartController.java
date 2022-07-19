/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements, refactoring
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.TokenFormat;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.malformedMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.notAuthenticated;

@Consumes({MediaType.MULTIPART_FORM_DATA})
@Produces({MediaType.MULTIPART_FORM_DATA})
@Path(MultipartController.PATH)
public class MultipartController {
    
    public static final String PATH = "/data";
    private static final String HEADER = "header";
    private static final String PAYLOAD = "payload";
    private static final String TOKEN_SCOPE = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";

    private final Monitor monitor;
    private final String connectorId;
    private final List<Handler> multipartHandlers;
    private final ObjectMapper objectMapper;
    private final IdentityService identityService;
    private final String idsWebhookAddress;

    public MultipartController(@NotNull Monitor monitor,
                               @NotNull String connectorId,
                               @NotNull ObjectMapper objectMapper,
                               @NotNull IdentityService identityService,
                               @NotNull List<Handler> multipartHandlers,
                               @NotNull String idsWebhookAddress) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.multipartHandlers = Objects.requireNonNull(multipartHandlers);
        this.identityService = Objects.requireNonNull(identityService);
        this.idsWebhookAddress = Objects.requireNonNull(idsWebhookAddress);
    }
    
    /**
     * Processes an incoming IDS multipart request. Validates the message header before passing the
     * request to a handler depending on the message type.
     *
     * @param headerInputStream the multipart header part.
     * @param payload the multipart payload part.
     * @return a multipart response with code 200. In case of error, the multipart header is a
     *         rejection message.
     */
    @POST
    public Response request(@FormDataParam(HEADER) InputStream headerInputStream,
                            @FormDataParam(PAYLOAD) String payload) {
        if (headerInputStream == null) {
            return Response.ok(buildMultipart(malformedMessage(null, connectorId))).build();
        }

        Message header;
        try {
            header = objectMapper.readValue(headerInputStream, Message.class);
        } catch (IOException e) {
            return Response.ok(buildMultipart(malformedMessage(null, connectorId))).build();
        }

        if (header == null) {
            return Response.ok(buildMultipart(malformedMessage(null, connectorId))).build();
        }
        
        // Check if any required header field missing
        if (header.getId() == null || header.getIssuerConnector() == null || header.getSenderAgent() == null) {
            return Response.ok(buildMultipart(malformedMessage(header, connectorId))).build();
        }

        // Check if DAT present
        var dynamicAttributeToken = header.getSecurityToken();
        if (dynamicAttributeToken == null || dynamicAttributeToken.getTokenValue() == null) {
            monitor.warning("MultipartController: Token is missing in header");
            return Response.ok(buildMultipart(notAuthenticated(header, connectorId))).build();
        }
    
        // Prepare DAT validation: IDS token validation requires issuerConnector
        var additional = new HashMap<String, Object>();
        additional.put("issuerConnector", header.getIssuerConnector());

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(dynamicAttributeToken.getTokenValue())
                .additional(additional)
                .build();
    
        // Validate DAT
        var verificationResult = identityService.verifyJwtToken(tokenRepresentation, idsWebhookAddress);
        if (verificationResult.failed()) {
            monitor.warning(format("MultipartController: Token validation failed %s", verificationResult.getFailure().getMessages()));
            return Response.ok(buildMultipart(notAuthenticated(header, connectorId))).build();
        }

        // Build the multipart request
        var claimToken = verificationResult.getContent();
        var multipartRequest = MultipartRequest.Builder.newInstance()
                .header(header)
                .payload(payload)
                .claimToken(claimToken)
                .build();

        // Find handler for the multipart request
        var handler = multipartHandlers.stream()
                .filter(h -> h.canHandle(multipartRequest))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            return Response.ok(buildMultipart(messageTypeNotSupported(header, connectorId))).build();
        }

        var multipartResponse = handler.handleRequest(multipartRequest);
        return Response.ok(buildMultipart(multipartResponse)).build();
    }
    
    /**
     * Creates a multipart body for the given response. Adds the security token to the response
     * header.
     *
     * @param multipartResponse the multipart response.
     * @return a multipart body.
     */
    private FormDataMultiPart buildMultipart(MultipartResponse multipartResponse) {
        addTokenToResponseHeader(multipartResponse.getHeader());
        return createFormDataMultiPart(multipartResponse.getHeader(), multipartResponse.getPayload());
    }
    
    /**
     * Creates a multipart body with the given message header and no payload. Adds the security
     * token to the response header.
     *
     * @param header the multipart response.
     * @return a multipart body.
     */
    private FormDataMultiPart buildMultipart(Message header) {
        addTokenToResponseHeader(header);
        return createFormDataMultiPart(header, null);
    }
    
    /**
     * Builds a form-data multipart body with the given header and payload.
     *
     * @param header the header.
     * @param payload the payload.
     * @return a multipart body.
     */
    private FormDataMultiPart createFormDataMultiPart(Message header, Object payload) {
        var multiPart = new FormDataMultiPart();
        if (header != null) {
            multiPart.bodyPart(new FormDataBodyPart(HEADER, toJson(header), MediaType.APPLICATION_JSON_TYPE));
        }

        if (payload != null) {
            multiPart.bodyPart(new FormDataBodyPart(PAYLOAD, toJson(payload), MediaType.APPLICATION_JSON_TYPE));
        }

        return multiPart;
    }
    
    /**
     * Retrieves an identity token and adds it to the given message.
     *
     * @param header the message.
     */
    private void addTokenToResponseHeader(Message header) {
        var tokenBuilder = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT);
        
        var tokenParameters = TokenParameters.Builder.newInstance()
                .scope(TOKEN_SCOPE)
                .audience(header.getIssuerConnector().toString())
                .build();
        var tokenResult = identityService.obtainClientCredentials(tokenParameters);
        
        if (tokenResult.succeeded()) {
            tokenBuilder._tokenValue_(tokenResult.getContent().getToken());
        } else {
            tokenBuilder._tokenValue_("invalid");
        }
        
        header.setSecurityToken(tokenBuilder.build());
    }

    private byte[] toJson(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }
}
