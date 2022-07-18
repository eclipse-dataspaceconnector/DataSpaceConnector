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

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.dataspaceconnector.serializer.types.UriDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.UriSerializer;
import org.eclipse.dataspaceconnector.serializer.types.number.BigDecimalDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.BigDecimalSerializer;
import org.eclipse.dataspaceconnector.serializer.types.number.BigIntegerDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.BigIntegerSerializer;
import org.eclipse.dataspaceconnector.serializer.types.number.FloatDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.FloatSerializer;
import org.eclipse.dataspaceconnector.serializer.types.number.LongDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.LongSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;

public class JsonLdModule extends SimpleModule {

    public JsonLdModule() {
        super();

        addSerializer(URI.class, new UriSerializer());
        addSerializer(BigDecimal.class, new BigDecimalSerializer());
        addSerializer(BigInteger.class, new BigIntegerSerializer());
        addSerializer(Long.class, new LongSerializer());
        addSerializer(Float.class, new FloatSerializer());

        addDeserializer(URI.class, new UriDeserializer());
        addDeserializer(BigInteger.class, new BigIntegerDeserializer());
        addDeserializer(BigDecimal.class, new BigDecimalDeserializer());
        addDeserializer(Long.class, new LongDeserializer());
        addDeserializer(Float.class, new FloatDeserializer());
    }
}
