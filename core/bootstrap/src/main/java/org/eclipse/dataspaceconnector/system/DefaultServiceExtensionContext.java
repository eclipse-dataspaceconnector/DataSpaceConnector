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

package org.eclipse.dataspaceconnector.system;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.util.TopologicalSort;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;
import static org.eclipse.dataspaceconnector.spi.system.ServiceExtension.LoadPhase.DEFAULT;
import static org.eclipse.dataspaceconnector.spi.system.ServiceExtension.LoadPhase.PRIMORDIAL;

/**
 * Base service extension context.
 * <p>Prior to using, {@link #initialize()} must be called.</p>
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private final Monitor monitor;
    private final TypeManager typeManager;

    private final Map<Class<?>, List<Object>> services = new HashMap<>();
    private final ServiceLocator serviceLocator;
    private List<ConfigurationExtension> configurationExtensions;
    private String connectorId;

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor) {
        this(typeManager, monitor, new ServiceLocatorImpl());
    }

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor, ServiceLocator serviceLocator) {
        this.typeManager = typeManager;
        this.monitor = monitor;
        this.serviceLocator = serviceLocator;
        // register as services
        registerService(TypeManager.class, typeManager);
        registerService(Monitor.class, monitor);
    }

    @Override
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public TypeManager getTypeManager() {
        return typeManager;
    }

    /**
     * Attempts to resolve the setting by delegating to configuration extensions, VM properties, and then env variables, in that order; otherwise
     * the default value is returned.
     */
    @Override
    public String getSetting(String key, String defaultValue) {
        String value;
        for (ConfigurationExtension extension : configurationExtensions) {
            value = extension.getSetting(key);
            if (value != null) {
                return value;
            }
        }
        value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public <T> boolean hasService(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "Type must not be null");

        return services.containsKey(type);
    }

    @Override
    public <T> T getService(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "Type must not be null");

        return getService(type, false);
    }

    @NotNull
    @Override
    public <T> List<T> getServices(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "Type must not be null");

        return getServices(type, false);
    }

    @NotNull
    @Override
    public <T> List<T> getServices(@NotNull Class<T> type, boolean isOptional) {
        Objects.requireNonNull(type, "Type must not be null");

        List<T> serviceInstances = (List<T>) this.services.get(type);

        if (!isOptional && serviceInstances == null) {
            throw new EdcException("Service not found: " + type.getName());
        }

        return serviceInstances != null ? serviceInstances : Collections.emptyList();
    }

    @Override
    public <T> T getService(@NotNull Class<T> type, boolean isOptional) {
        Objects.requireNonNull(type, "Type must not be null");

        List<T> serviceInstances = getServices(type, isOptional);

        if (serviceInstances.size() > 1) {
            throw new EdcException("Ambiguous service found: " + type.getName());
        }

        return !serviceInstances.isEmpty() ? serviceInstances.iterator().next() : null;
    }

    @Override
    public <T> void registerService(@NotNull Class<T> type, @NotNull T service) {
        Objects.requireNonNull(type, "Type must not be null");
        Objects.requireNonNull(service, "Service must not be null");

        services.computeIfAbsent(type, k -> new LinkedList<>()).add(service);
    }

    @Override
    public List<ServiceExtension> loadServiceExtensions() {
        List<ServiceExtension> serviceExtensions = loadExtensions(ServiceExtension.class, true);
        List<ServiceExtension> primordialExtensions = serviceExtensions.stream().filter(ext -> ext.phase() == PRIMORDIAL).collect(toCollection(ArrayList::new));
        List<ServiceExtension> defaultExtensions = serviceExtensions.stream().filter(ext -> ext.phase() == DEFAULT).collect(toCollection(ArrayList::new));

        //the first sort is only to verify that there are no "upward" dependencies from PRIMORDIAL -> DEFAULT
        sortExtensions(primordialExtensions, Collections.emptySet());
        sortExtensions(defaultExtensions, primordialExtensions.stream().flatMap(e -> e.provides().stream()).collect(Collectors.toSet()));

        List<ServiceExtension> totalOrdered = new ArrayList<>(primordialExtensions);
        totalOrdered.addAll(defaultExtensions);
        return totalOrdered;
    }

    @Override
    public <T> List<T> loadExtensions(Class<T> type, boolean required) {
        return serviceLocator.loadImplementors(type, required);
    }

    @Override
    public <T> T loadSingletonExtension(Class<T> type, boolean required) {
        return serviceLocator.loadSingletonImplementor(type, required);
    }

    @Override
    public void initialize() {
        configurationExtensions = loadExtensions(ConfigurationExtension.class, false);
        configurationExtensions.forEach(ext -> ext.initialize(monitor));
        connectorId = getSetting("edc.connector.name", "edc-" + UUID.randomUUID());
    }

    private void sortExtensions(List<ServiceExtension> extensions, Set<String> loadedExtensions) {
        Map<String, List<ServiceExtension>> mappedExtensions = new HashMap<>();
        extensions.forEach(ext -> ext.provides().forEach(feature -> mappedExtensions.computeIfAbsent(feature, k -> new ArrayList<>()).add(ext)));

        TopologicalSort<ServiceExtension> sort = new TopologicalSort<>();
        extensions.forEach(ext -> ext.requires().forEach(feature -> {
            List<ServiceExtension> dependencies = mappedExtensions.get(feature);
            if (dependencies == null && !loadedExtensions.contains(feature)) {
                throw new EdcException(format("Extension feature required by %s not found: %s", ext.getClass().getName(), feature));
            } else if (dependencies != null) {
                dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
            }
        }));
        sort.sort(extensions);
    }

}
