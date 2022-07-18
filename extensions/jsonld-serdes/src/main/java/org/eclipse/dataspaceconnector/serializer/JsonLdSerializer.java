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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;

import java.io.IOException;

public class JsonLdSerializer<T> extends JsonSerializer<T> {
    private final Class<T> type;
    private final String contextInformation;

    public JsonLdSerializer(Class<T> type, String contextInformation) {
        this.type = type;
        this.contextInformation = contextInformation;
    }

    @Override
    public void serialize(Object value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        var javaType = provider.constructType(type);
        var beanDescription = provider.getConfig().introspect(javaType);
        var staticTyping = provider.isEnabled(MapperFeature.USE_STATIC_TYPING);
        var serializer = BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDescription, staticTyping);
        serializer.unwrappingSerializer(null).serialize(value, generator, provider);
        generator.writeObjectField("@context", contextInformation);
        generator.writeEndObject();
    }
}
