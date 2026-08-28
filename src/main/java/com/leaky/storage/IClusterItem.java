package com.leaky.storage;

/**
 * Interface for items that are aware of their surrounding item counts
 */
public interface IClusterItem
{
    /**
     * Associates an item with its given itemcluster
     *
     * @param cluster
     */
    public void setCluster(ItemCluster cluster);

    /**
     * Gets the current itemcluster
     *
     * @return
     */
    public ItemCluster getCluster();
}
