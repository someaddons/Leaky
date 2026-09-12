package com.leaky.storage;

import com.leaky.Leaky;
import com.leaky.config.CommonConfiguration;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Dimension specific cluster manager
 */
public class ItemClusterManager
{
    /**
     * Spatial storage for fast lookup
     */
    private Long2ObjectOpenHashMap<List<ItemCluster>> clusterSpatialStorage = new Long2ObjectOpenHashMap<>();

    /**
     * List based storage
     */
    private List<ItemCluster> clusterStorage = new ArrayList<>();

    /**
     * List of activity history, especially removals and their backups
     */
    private List<ClusterHistory> historyList = new ArrayList<>();

    /**
     * The cluster managers level
     */
    private final Level level;

    public ItemClusterManager(final Level level)
    {
        this.level = level;
    }

    /**
     * Called when a group of items exceeds the initial tracking size
     *
     * @param entity
     * @param items
     * @param detectionSource
     */
    public void detectedItemLeak(final ItemEntity entity, final List<ItemEntity> items, final DetectionSource detectionSource)
    {
        // Associate with cluster
        final ItemCluster cluster = getExistingClusterOrNew(entity, detectionSource);
        for (final ItemEntity itemEntity : items)
        {
            cluster.tryAdd(itemEntity);
        }

        int size = Math.max(items.size(), cluster.count());
        // Large leaks bypass the cooldown, to allow deletion before server crashes
        if (size >= CommonConfiguration.config.getCommonConfig().autoremovenodelaythreshold)
        {
            cluster.sendNotification(cluster.createDeletionNotification(), ClusterState.PENDING_DELETION);
            removeCluster(cluster.id());
        }
    }

    /**
     * Server level tick callback, used to check/evict stale clusters
     *
     * @param serverLevel
     */
    public void onTick(final Level serverLevel)
    {
        if (serverLevel.getServer().getTickCount() % (20 * 10) == 1)
        {
            clusterStorage.removeIf(cluster -> {
                if (cluster.count() == 0)
                {
                    final long index = calculateCellIndex(cluster.position);
                    final List<ItemCluster> spatialClusterList = clusterSpatialStorage.get(index);
                    if (spatialClusterList != null && spatialClusterList.remove(cluster))
                    {
                        if (spatialClusterList.isEmpty())
                        {
                            clusterSpatialStorage.remove(index);
                        }
                    }
                    else
                    {
                        Leaky.LOGGER.warn("Tried to remove nonexisting cluster: " + cluster + " from storage:" + spatialClusterList);
                    }

                    return true;
                }

                return false;
            });
            clusterStorage.forEach(ItemCluster::checkState);
        }
    }

    /**
     * Gets the cluster for a detected item leak, or creates a new one
     *
     * @param entity
     * @return
     */
    private ItemCluster getExistingClusterOrNew(final ItemEntity entity, final DetectionSource detectionSource)
    {
        ItemCluster cluster = getExistingCluster(entity, detectionSource);
        if (cluster == null)
        {
            cluster = create(entity);
        }
        return cluster;
    }

    /**
     * Gets the stored cluster or creates a new one
     *
     * @param item
     * @param range the range at which a cluster can be searched
     * @return
     */
    private ItemCluster getExistingCluster(final ItemEntity item, final DetectionSource detectionSource)
    {
        if (item instanceof IClusterItem iNearbyItemAwareEntity)
        {
            if (iNearbyItemAwareEntity.getCluster() != null)
            {
                return iNearbyItemAwareEntity.getCluster();
            }

            final BlockPos itemPos = item.blockPosition();
            final int minCellX = (itemPos.getX() - detectionSource.range) >> 5;
            final int maxCellX = (itemPos.getX() + detectionSource.range) >> 5;

            final int minCellZ = (itemPos.getZ() - detectionSource.range) >> 5;
            final int maxCellZ = (itemPos.getZ() + detectionSource.range) >> 5;

            int dist = Integer.MAX_VALUE;
            ItemCluster closest = null;
            for (int x = minCellX; x <= maxCellX; x++)
            {
                for (int z = minCellZ; z <= maxCellZ; z++)
                {
                    final List<ItemCluster> clusters = clusterSpatialStorage.get(ChunkPos.pack(x, z));
                    if (clusters != null)
                    {
                        for (final ItemCluster cluster : clusters)
                        {
                            final int sqDist = (int) cluster.position.distSqr(itemPos);
                            if (sqDist < dist && sqDist <= detectionSource.range * detectionSource.range)
                            {
                                dist = sqDist;
                                closest = cluster;
                            }
                        }
                    }
                }
            }

            if (closest != null)
            {
                closest.tryAdd(item);
            }

            return closest;
        }

        return null;
    }

    /**
     * Creates a new item cluster and stores it
     *
     * @param pos
     * @param time
     * @return
     */
    private ItemCluster create(final ItemEntity entity)
    {
        ItemCluster cluster = new ItemCluster(entity.level(), entity.blockPosition(), entity.level().getGameTime());
        clusterStorage.add(cluster);
        long index = calculateCellIndex(entity.blockPosition());
        List<ItemCluster> clusterList = clusterSpatialStorage.get(index);
        if (clusterList == null)
        {
            clusterList = new ArrayList<>();
            clusterSpatialStorage.put(index, clusterList);
        }

        for (final ItemCluster existingCluster : clusterList)
        {
            if (existingCluster.position.distManhattan(entity.blockPosition()) < 10)
            {
                Leaky.LOGGER.warn("Adding cluster twice!", new Exception());
            }
        }

        clusterList.add(cluster);
        return cluster;
    }

    /**
     * Tries to remove all items of that given item cluster
     *
     * @param clusterID
     * @return
     */
    public boolean removeCluster(final int clusterID)
    {
        for (Iterator<ItemCluster> iterator = clusterStorage.iterator(); iterator.hasNext(); )
        {
            final ItemCluster cluster = iterator.next();
            if (cluster.id() == clusterID)
            {
                iterator.remove();
                cluster.clearItems();
                clusterSpatialStorage.get(calculateCellIndex(cluster.position)).remove(cluster);
                return true;
            }
        }

        return false;
    }

    public ItemCluster getCluster(final int id)
    {
        for (final ItemCluster cluster : clusterStorage)
        {
            if (cluster.id() == id)
            {
                return cluster;
            }
        }

        return null;
    }

    /**
     * Calculates the index for the given position, note that items may be right at the boundary
     */
    private long calculateCellIndex(final BlockPos pos)
    {
        int indexX = pos.getX() >> 5;
        int indexZ = pos.getZ() >> 5;

        return ChunkPos.pack(indexX, indexZ);
    }

    /**
     * Generates a status report
     *
     * @param source
     */
    public void reportStatus(final CommandSourceStack source)
    {
        if (clusterStorage.isEmpty())
        {
            return;
        }

        final List<ItemCluster> clusters = new ArrayList<>(clusterStorage);
        if (clusters.isEmpty())
        {
            return;
        }

        source.sendSystemMessage(Component.translatable(level.dimension().identifier().toLanguageKey()).withStyle(ChatFormatting.DARK_AQUA));

        clusters.sort(Comparator.comparing(ItemCluster::getClusterState));
        for (final ItemCluster cluster : clusters)
        {
            source.sendSystemMessage(cluster.minimalStatusReport());
        }
    }

    /**
     * Generates a status report
     *
     * @param source
     */
    public boolean reportHistory(final CommandSourceStack source)
    {
        if (historyList.isEmpty())
        {
            return false;
        }

        source.sendSystemMessage(Component.translatable(level.dimension().identifier().toLanguageKey()).withStyle(ChatFormatting.DARK_AQUA));

        for (int i = historyList.size() - 1; i >= 0; i--)
        {
            final ClusterHistory history = historyList.get(i);
            source.sendSystemMessage(history.report(source));
        }

        return true;
    }

    /**
     * Adds the given history
     *
     * @param history
     */
    public void addHistory(final ClusterHistory history)
    {
        historyList.add(history);
    }

    /**
     * Get the history by ID
     *
     * @param historyClusterID
     * @return
     */
    public ClusterHistory getHistory(final int historyClusterID)
    {
        for (final ClusterHistory history : historyList)
        {
            if (history.id == historyClusterID)
            {
                return history;
            }
        }

        return null;
    }

    /**
     * Removes the given id from history
     *
     * @param historyClusterID
     */
    public void removeHistory(final int historyClusterID)
    {
        for (Iterator<ClusterHistory> iterator = historyList.iterator(); iterator.hasNext(); )
        {
            final ClusterHistory history = iterator.next();
            if (history.id == historyClusterID)
            {
                iterator.remove();
            }
        }
    }
}
