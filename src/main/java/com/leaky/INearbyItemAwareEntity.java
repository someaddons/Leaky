package com.leaky;

/**
 * Interface for items that are aware of their surrounding item counts
 */
public interface INearbyItemAwareEntity
{
    int getNearbyItems();

    void setNearbyItems(int items);
}
