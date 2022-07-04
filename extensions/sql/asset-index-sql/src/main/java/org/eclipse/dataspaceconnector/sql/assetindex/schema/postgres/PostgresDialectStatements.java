/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.assetindex.schema.postgres;

import org.eclipse.dataspaceconnector.sql.assetindex.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.dialect.PostgresDialect;

public class PostgresDialectStatements extends BaseSqlDialectStatements {
    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
