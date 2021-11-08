package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;

/**
 * Handles a certain subset of catalog instances inside a subset of a data space.
 * For example, a partition could be based on geographic regions. The most obvious use case for a {@code PartitionManager}
 * would be to dispatch {@link Crawler} objects to gather catalogs from other connectors.
 */
public interface PartitionManager {


    /**
     * Schedules all crawlers for execution according to this ExecutionPlan
     */
    void schedule(ExecutionPlan executionPlan);

    /**
     * Waits until all crawlers have finished their task.
     */
    void stop();
}
