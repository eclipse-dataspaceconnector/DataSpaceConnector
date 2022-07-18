/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.serializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.ConnectorUpdateMessage;
import de.fraunhofer.iais.eis.ContentType;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.ContractOfferMessage;
import de.fraunhofer.iais.eis.ContractRejectionMessage;
import de.fraunhofer.iais.eis.ContractRequestMessage;
import de.fraunhofer.iais.eis.CustomMediaType;
import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Frequency;
import de.fraunhofer.iais.eis.Language;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.NotificationMessage;
import de.fraunhofer.iais.eis.ParticipantUpdateMessage;
import de.fraunhofer.iais.eis.PaymentModality;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import de.fraunhofer.iais.eis.Token;
import de.fraunhofer.iais.eis.TokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.dataspaceconnector.ids.core.serialization.IdsConstraintImpl;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.DefaultValues;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSerializerTest {
    private ObjectMapper objectMapper;

    private final String string = "example";
    private final String language = "en";
    private final String type = "json";
    private final BigInteger bigInt = BigInteger.valueOf(365);
    private final BigDecimal bigDecimal = BigDecimal.valueOf(365);
    private final XMLGregorianCalendar date = CalendarUtil.gregorianNow();
    private final URI uri = URI.create("http://example");

    private final TypedLiteral typedLiteral = new TypedLiteral(string, language);
    private final CustomMediaType mediaType = new CustomMediaTypeBuilder()._filenameExtension_(type).build();

    private final Language idsLanguage = Language.EN;
    private final Frequency idsFrequency = Frequency.DAILY;
    private final ContentType idsContentType = ContentType.SCHEMA_DEFINITION;
    private final PaymentModality idsPaymentModality = PaymentModality.FIXED_PRICE;
    private final RejectionReason reason = RejectionReason.BAD_PARAMETERS;
    private final TokenFormat format = TokenFormat.JWT;

    private final Token token = new TokenBuilder()._tokenFormat_(format)._tokenValue_(string).build();
    private final DynamicAttributeToken dat = new DynamicAttributeTokenBuilder()._tokenFormat_(format)._tokenValue_(string).build();

    @BeforeEach
    void setUp() {
        var typeManager = new TypeManager();

        var customMapper = JsonLdSerializationService.getObjectMapper();

        // register custom object mapper
        typeManager.registerContext("ids", customMapper);

        // register custom IDS constraint serializer
        typeManager.registerTypes("ids", IdsConstraintImpl.class);

        // register serializer for used IDS objects

        // messages
        typeManager.registerSerializer("ids", ArtifactRequestMessage.class, new JsonLdSerializer<>(ArtifactRequestMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", RequestInProcessMessage.class, new JsonLdSerializer<>(RequestInProcessMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", MessageProcessedNotificationMessage.class, new JsonLdSerializer<>(MessageProcessedNotificationMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", DescriptionRequestMessage.class, new JsonLdSerializer<>(DescriptionRequestMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", NotificationMessage.class, new JsonLdSerializer<>(NotificationMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ParticipantUpdateMessage.class, new JsonLdSerializer<>(ParticipantUpdateMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", RejectionMessage.class, new JsonLdSerializer<>(RejectionMessage.class, DefaultValues.CONTEXT));

        typeManager.registerSerializer("ids", ContractAgreementMessage.class, new JsonLdSerializer<>(ContractAgreementMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ContractRejectionMessage.class, new JsonLdSerializer<>(ContractRejectionMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ContractOfferMessage.class, new JsonLdSerializer<>(ContractOfferMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ContractRequestMessage.class, new JsonLdSerializer<>(ContractRequestMessage.class, DefaultValues.CONTEXT));

        typeManager.registerSerializer("ids", DynamicAttributeToken.class, new JsonLdSerializer<>(DynamicAttributeToken.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", TokenFormat.class, new JsonLdSerializer<>(TokenFormat.class, DefaultValues.CONTEXT));

        // contract/policy
        //        typeManager.registerSerializer("ids", ContractAgreement.class, new JsonLdSerializer<>
        //        (ContractAgreement.class, DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", ContractOffer.class, new JsonLdSerializer<>
        //        (ContractOffer.class, DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", Contract.class, new JsonLdSerializer<>(Contract
        //        .class, DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", Permission.class, new JsonLdSerializer<>
        //        (Permission.class, DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", Prohibition.class, new JsonLdSerializer<>
        //        (Prohibition.class, DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", Duty.class, new JsonLdSerializer<>(Duty.class,
        //        DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", Action.class, new JsonLdSerializer<>(Action
        //        .class, DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", LogicalConstraint.class, new JsonLdSerializer<>
        //        (LogicalConstraint.class, DefaultValues.CONTEXT));
        //        typeManager.registerSerializer("ids", AbstractConstraint.class, new JsonLdSerializer<>
        //        (AbstractConstraint.class, DefaultValues.CONTEXT));

        // asset
        typeManager.registerSerializer("ids", Artifact.class, new JsonLdSerializer<>(Artifact.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", BaseConnector.class, new JsonLdSerializer<>(BaseConnector.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Representation.class, new JsonLdSerializer<>(Representation.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Resource.class, new JsonLdSerializer<>(Resource.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", TypedLiteral.class, new JsonLdSerializer<>(TypedLiteral.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ResourceCatalog.class, new JsonLdSerializer<>(ResourceCatalog.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", CustomMediaType.class, new JsonLdSerializer<>(CustomMediaType.class, DefaultValues.CONTEXT));

        objectMapper = typeManager.getMapper("ids");
    }

    @Test
    void serialize() throws IOException {
        var obj = new ContractAgreementBuilder(URI.create("urn:contractagreement:1"))
                ._provider_(URI.create("http://provider"))
                ._consumer_(URI.create("http://consumer"))
                ._permission_(new PermissionBuilder()
                        ._target_(URI.create("urn:artifact:"))
                        .build())
                ._contractStart_(CalendarUtil.gregorianNow())
                ._contractEnd_(CalendarUtil.gregorianNow())
                ._contractDate_(CalendarUtil.gregorianNow())
                .build();

        var result = objectMapper.writeValueAsString(obj);
        assertTrue(result.contains("@context"));

        var agreement = objectMapper.readValue(result, JsonNode.class);

        assertTrue(agreement.get("@id").asText().contains("urn:contractagreement:1"));
        assertTrue(agreement.get("@type").asText().contains("ids:ContractAgreement"));
    }

    @Test
    void deserializeDscMessage() throws IOException {
        var obj = "{\n" +
                "  \"@context\" : {\n" +
                "  \"ids\" : \"https://w3id.org/idsa/core/\",\n" +
                "  \"idsc\" : \"https://w3id.org/idsa/code/\"\n" +
                "  },\n" +
                "  \"@type\" : \"ids:ConnectorUpdateMessage\",\n" +
                "  \"@id\" : \"https://w3id.org/idsa/autogen/connectorUpdateMessage/38e8a1e1-937a" +
                "-42b5-bb5b-984e4fa68c0b\",\n" +
                "  \"ids:affectedConnector\" : {\n" +
                "    \"@id\" : \"https://w3id.org/idsa/autogen/baseConnector/7b934432-a85e-41c5" +
                "-9f65-669219dde4ea\"\n" +
                "  },\n" +
                "  \"ids:issuerConnector\" : {\n" +
                "    \"@id\" : \"https://w3id.org/idsa/autogen/baseConnector/7b934432-a85e-41c5" +
                "-9f65-669219dde4ea\"\n" +
                "  },\n" +
                "  \"ids:modelVersion\" : \"4.2.7\",\n" +
                "  \"ids:senderAgent\" : {\n" +
                "    \"@id\" : \"https://w3id.org/idsa/autogen/baseConnector/7b934432-a85e-41c5" +
                "-9f65-669219dde4ea\"\n" +
                "  },\n" +
                "  \"ids:issued\" : {\n" +
                "    \"@value\" : \"2022-07-07T08:25:11.702+02:00\",\n" +
                "    \"@type\" : \"http://www.w3.org/2001/XMLSchema#dateTimeStamp\"\n" +
                "  }\n" +
                "}";

        var agreement = objectMapper.readValue(obj, ConnectorUpdateMessage.class);
        assertNotNull(agreement);
    }

    @Test
    void serialize_deserialize_rejectionMessage() throws IOException {
        var obj = new RejectionMessageBuilder()
                ._authorizationToken_(token)
                ._contentVersion_(string)
                ._correlationMessage_(uri)
                ._issued_(date)
                ._issuerConnector_(uri)
                ._modelVersion_(string)
                ._recipientAgent_(uri)
                ._recipientConnector_(uri)
                ._rejectionReason_(reason)
                ._securityToken_(dat)
                ._senderAgent_(uri)
                ._transferContract_(uri)
                .build();

        var resultString = objectMapper.writeValueAsString(obj);
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, RejectionMessage.class);
        assertNotNull(resultObj);
        assertEquals(token, resultObj.getAuthorizationToken());
        assertEquals(format, resultObj.getAuthorizationToken().getTokenFormat());
        assertEquals(string, resultObj.getAuthorizationToken().getTokenValue());
        assertEquals(string, resultObj.getContentVersion());
        assertEquals(uri, resultObj.getCorrelationMessage());
        assertEquals(date, resultObj.getIssued());
        assertEquals(uri, resultObj.getIssuerConnector());
        assertEquals(string, resultObj.getModelVersion());
        assertEquals(uri, resultObj.getRecipientAgent().get(0));
        assertEquals(uri, resultObj.getRecipientConnector().get(0));
        assertEquals(reason, resultObj.getRejectionReason());
        assertEquals(dat, resultObj.getSecurityToken());
        assertEquals(format, resultObj.getSecurityToken().getTokenFormat());
        assertEquals(string, resultObj.getSecurityToken().getTokenValue());
        assertEquals(uri, resultObj.getSenderAgent());
        assertEquals(uri, resultObj.getTransferContract());

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_artifact() throws IOException {
        var obj = getArtifact();

        var resultString = objectMapper.writeValueAsString(obj);
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, Artifact.class);
        assertNotNull(resultObj);

        assertEquals(bigInt, (resultObj).getByteSize());
        assertEquals(string, (resultObj).getCheckSum());
        assertEquals(bigDecimal, (resultObj).getDuration());
        assertEquals(date, (resultObj).getCreationDate());
        assertEquals(string, (resultObj).getFileName());

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_representation() throws IOException {
        var obj = getRepresentation();

        var resultString = objectMapper.writeValueAsString(obj);
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, Representation.class);
        assertNotNull(resultObj);

        assertEquals(date, resultObj.getCreated());
        assertEquals(1, resultObj.getDescription().size());
        assertEquals(typedLiteral, resultObj.getDescription().get(0));
        assertEquals(language, resultObj.getDescription().get(0).getLanguage());
        assertEquals(string, resultObj.getDescription().get(0).getValue());
        assertEquals(idsLanguage, resultObj.getLanguage());
        assertEquals(mediaType, resultObj.getMediaType());
        assertEquals(type, resultObj.getMediaType().getFilenameExtension());
        assertEquals(date, resultObj.getModified());
        assertEquals(uri, resultObj.getRepresentationStandard());
        assertEquals(uri, resultObj.getShapesGraph());
        assertEquals(1, resultObj.getTitle().size());
        assertEquals(typedLiteral, resultObj.getTitle().get(0));
        assertEquals(language, resultObj.getTitle().get(0).getLanguage());
        assertEquals(string, resultObj.getTitle().get(0).getValue());

        assertTrue(resultObj.getInstance().get(0) instanceof Artifact);

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_resource() throws IOException {
        var obj = getResource();

        var resultString = objectMapper.writeValueAsString(obj);
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, Resource.class);
        assertNotNull(resultObj);

        assertEquals(idsFrequency, resultObj.getAccrualPeriodicity());
        assertEquals(uri, resultObj.getContentStandard());
        assertEquals(idsContentType, resultObj.getContentType());
        assertEquals(date, resultObj.getCreated());
        assertEquals(uri, resultObj.getCustomLicense());
        assertEquals(typedLiteral, resultObj.getDescription().get(0));
        assertEquals(typedLiteral, resultObj.getKeyword().get(0));
        assertEquals(idsLanguage, resultObj.getLanguage().get(0));
        assertEquals(date, resultObj.getModified());
        assertEquals(idsPaymentModality, resultObj.getPaymentModality());
        assertEquals(uri, resultObj.getPublisher());
        assertEquals(uri, resultObj.getShapesGraph());
        assertEquals(uri, resultObj.getSovereign());
        assertEquals(uri, resultObj.getStandardLicense());
        assertEquals(idsFrequency, resultObj.getTemporalResolution());
        assertEquals(uri, resultObj.getTheme().get(0));
        assertEquals(typedLiteral, resultObj.getTitle().get(0));
        assertEquals(string, resultObj.getVersion());

        assertNotNull(resultObj.getRepresentation().get(0));

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_catalog() throws IOException {
        var obj = getCatalog();

        var resultString = objectMapper.writeValueAsString(obj);
        assertNotNull(resultString);

        //        var resultObj = objectMapper.readValue(resultString, ResourceCatalog.class);
        //        assertNotNull(resultObj);
        //
        //        assertNotNull(resultObj.getOfferedResource());
        //        assertNotNull(resultObj.getOfferedResourceAsUri().get(0));
        //
        //        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    // build objects

    private ResourceCatalog getCatalog() {
        return new ResourceCatalogBuilder()._offeredResource_(List.of(getResource())).build();
    }

    private Artifact getArtifact() {
        return new ArtifactBuilder()
                ._byteSize_(bigInt)
                ._checkSum_(string)
                ._duration_(bigDecimal)
                ._creationDate_(date)
                ._fileName_(string)
                .build();
    }

    private Representation getRepresentation() {
        return new RepresentationBuilder()
                ._created_(date)
                ._description_(typedLiteral)
                ._instance_(getArtifact())
                ._language_(idsLanguage)
                ._mediaType_(mediaType)
                ._modified_(date)
                ._representationStandard_(uri)
                ._shapesGraph_(uri)
                ._title_(typedLiteral)
                .build();
    }

    private Resource getResource() {
        return new ResourceBuilder()
                ._accrualPeriodicity_(idsFrequency)
                ._contentStandard_(uri)
                ._contentType_(idsContentType)
                //                ._contractOffer_()
                ._created_(date)
                ._customLicense_(uri)
                ._description_(typedLiteral)
                ._keyword_(typedLiteral)
                ._language_(idsLanguage)
                ._modified_(date)
                ._paymentModality_(idsPaymentModality)
                ._publisher_(uri)
                ._representation_(getRepresentation())
                ._shapesGraph_(uri)
                ._sovereign_(uri)
                ._standardLicense_(uri)
                ._temporalResolution_(idsFrequency)
                ._theme_(uri)
                ._title_(typedLiteral)
                ._version_(string)
                .build();
    }
}
