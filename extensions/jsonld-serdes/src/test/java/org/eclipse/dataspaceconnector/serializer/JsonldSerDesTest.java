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

import org.junit.jupiter.api.Test;

import java.io.IOException;

class JsonldSerDesTest {

    @Test
    void serialize() throws IOException {
    //        var serDes = new JsonldSerDes(mock(Monitor.class));
    //
    //        var msg = new DescriptionRequestMessageBuilder()
    //                ._issuerConnector_(URI.create("test"))
    //                ._modelVersion_("4.2.0")
    //                .build();
    //
    //        var stringWithoutContext = serDes.getObjectMapper().writeValueAsString(msg);
    //        assertFalse(stringWithoutContext.contains("@context"));
    //
    //        var jsonWithoutContext = serDes.serialize(msg);
    //        assertFalse(jsonWithoutContext.contains("@context"));
    //
    //        serDes.setContext(DefaultValues.CONTEXT);
    //        serDes.setSubtypes(IdsConstraintImpl.class);
    //        var jsonWithContext = serDes.serialize(msg);
    //        assertTrue(jsonWithContext.contains("@context"));
    }
}
