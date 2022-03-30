/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.contract.definition.store.model;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generatePolicy;

class ContractDefinitionDocumentSerializationTest {

    private TypeManager typeManager;


    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(PolicyDocument.class, PolicyDocument.class);
    }

    @Test
    void testSerialization() {
        var def = generatePolicy();
        var pk = "test-part-key";

        var document = new PolicyDocument(def, pk);

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("wrappedInstance")
                .contains("\"id\":\"" + def.getUid() + "\"")
                .contains("\"partitionKey\":\"" + pk + "\"");
    }

    @Test
    void testDeserialization() {
        var def = generatePolicy();

        var document = new PolicyDocument(def, "test-part-key");
        String json = typeManager.writeValueAsString(document);

        var transferProcessDeserialized = typeManager.readValue(json, PolicyDocument.class);
        assertThat(transferProcessDeserialized).usingRecursiveComparison().isEqualTo(document);
    }
}
