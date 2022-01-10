package org.eclipse.dataspaceconnector.catalog.spi.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;

/**
 * {@link NodeQueryAdapter}s return {@code UpdateResponse} objects after a
 * catalog query returns. Contains information about the {@code source} (i.e. where the response comes from) and the
 * {@code assetNames}.
 * <p>
 * <p>
 * TODO: This must be updated to contain a list of {@link org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset}s after https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/pull/159 has been merged!
 */
public class UpdateResponse {
    private String source;
    private Catalog catalog;

    @JsonCreator
    public UpdateResponse(@JsonProperty("source") String source, @JsonProperty("catalog") Catalog assetNames) {
        this.source = source;
        catalog = assetNames;
    }

    public UpdateResponse() {

    }

    public Catalog getCatalog() {
        return catalog;
    }

    public String getSource() {
        return source;
    }
}
