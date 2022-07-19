/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.core.descriptor.IdsDescriptorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.serialization.calendar.XmlGregorianCalendarModule;
import org.eclipse.dataspaceconnector.ids.core.serialization.policy.IdsConstraintImpl;
import org.eclipse.dataspaceconnector.ids.core.service.CatalogServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceSettings;
import org.eclipse.dataspaceconnector.ids.core.transform.IdsTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.descriptor.IdsDescriptorService;
import org.eclipse.dataspaceconnector.ids.spi.domain.DefaultValues;
import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.serializer.JsonLdSerializer;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the IDS Controller REST API.
 */
@Provides({ CatalogService.class, ConnectorService.class, IdsDescriptorService.class,
        CatalogService.class, ConnectorService.class, IdsTransformerRegistry.class })
public class IdsCoreServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_CATALOG_ID = "edc.ids.catalog.id";

    public static final String DEFAULT_EDC_IDS_CATALOG_ID = "urn:catalog:default";

    private static final String WARNING_USING_DEFAULT_SETTING = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
    private static final String ERROR_INVALID_SETTING = "IDS Settings: Invalid setting for '%s'. Was %s'.";

    private Monitor monitor;

    @Inject
    private ContractOfferService contractOfferService;

    @Inject
    private IdentityService identityService;

    @Inject
    private OkHttpClient okHttpClient;

    @Override
    public String name() {
        return "IDS Core";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        customizeTypeManager(context);

        List<String> settingErrors = new ArrayList<>();
        ConnectorServiceSettings connectorServiceSettings = null;
        String dataCatalogId = null;

        try {
            connectorServiceSettings = new ConnectorServiceSettings(context, monitor);
        } catch (EdcException e) {
            settingErrors.add(e.getMessage());
        }

        try {
            dataCatalogId = resolveCatalogId(context);
        } catch (EdcException e) {
            settingErrors.add(e.getMessage());
        }

        if (!settingErrors.isEmpty()) {
            throw new EdcException(String.join(", ", settingErrors));
        }

        context.registerService(IdsTransformerRegistry.class, new IdsTransformerRegistryImpl());

        var dataCatalogService = new CatalogServiceImpl(monitor, dataCatalogId, contractOfferService);
        context.registerService(CatalogService.class, dataCatalogService);

        var connectorService = new ConnectorServiceImpl(monitor, connectorServiceSettings, dataCatalogService);
        context.registerService(ConnectorService.class, connectorService);

        context.registerService(IdsDescriptorService.class, new IdsDescriptorServiceImpl());
    }

    private String resolveCatalogId(ServiceExtensionContext context) {
        var value = context.getSetting(EDC_IDS_CATALOG_ID, null);
        if (value == null) {
            monitor.warning(String.format(WARNING_USING_DEFAULT_SETTING, EDC_IDS_CATALOG_ID, DEFAULT_EDC_IDS_CATALOG_ID));
            value = DEFAULT_EDC_IDS_CATALOG_ID;
        }

        try {
            // use stringified uri to keep uri path and query
            var idsId = IdsIdParser.parse(value);
            if (idsId.getType() == IdsType.CATALOG) {
                return idsId.getValue();
            } else {
                throw new EdcException(String.format(ERROR_INVALID_SETTING, EDC_IDS_CATALOG_ID, value));
            }
        } catch (IllegalArgumentException e) {
            throw new EdcException(String.format(ERROR_INVALID_SETTING, EDC_IDS_CATALOG_ID, value));
        }
    }

    private void customizeTypeManager(ServiceExtensionContext context) {
        var typeManager = context.getTypeManager();

        var customMapper = new ObjectMapper();
        customMapper.registerModule(new JavaTimeModule());
        customMapper.registerModule(new XmlGregorianCalendarModule());
        customMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        customMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        customMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        customMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        // register custom object mapper
        typeManager.registerContext("ids", customMapper);

        // register custom IDS constraint serializer
        typeManager.registerTypes("ids", IdsConstraintImpl.class);

        // register serializer for used IDS objects
        typeManager.registerSerializer("ids", ArtifactRequestMessage.class, new JsonLdSerializer<>(ArtifactRequestMessage.class, DefaultValues.CONTEXT));
        // TODO add all used IDS objects
    }
}
