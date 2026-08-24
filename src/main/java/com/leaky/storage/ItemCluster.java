package com.leaky.storage;

import com.leaky.config.CommonConfiguration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

import static com.leaky.storage.ClusterState.getStateColor;
import static com.leaky.util.ComponentUtil.*;

public class ItemCluster
{
    public final            BlockPos position;
    private final           long     creationTime;
    private final           int      id;
    public static volatile int      IDCOUNTER = 0;

    private final List<ItemEntity> itemEntities  = new ArrayList<>();
    private final Level            level;
    private       long             lastPruneTime = 0;

    private int initial       = 0;
    private int arrivals      = 0;
    private int despawns      = 0;
    private int pickups       = 0;
    private int otherRemovals = 0;

    private long lastPlayerPickup      = 0;
    private long autoDeletionTimePoint = 0;

    /**
     * Current detection state of this cluster
     */
    private ClusterState clusterState = ClusterState.CREATED;

    public ItemCluster(final Level level, final BlockPos position, final long creationTime)
    {
        this.position = position;
        this.creationTime = creationTime;
        this.id = ++IDCOUNTER;
        this.level = level;
        this.lastPruneTime = level.getGameTime();
        this.lastPlayerPickup = lastPruneTime;
    }

    public int id()
    {
        return id;
    }

    /**
     * Checks the item clusters state and adjusts it based on conditions
     */
    public void checkState()
    {
        final long age = (level.getGameTime() - creationTime);
        if (clusterState == ClusterState.CREATED)
        {
            // After 30 seconds we start considering the cluster, before its an uncertain period. E.g. a player may be breaking a couple chests
            if (age > 20 * 30)
            {
                clusterState = ClusterState.OBSERVING;
                arrivals = 0;
                despawns = 0;
                pickups = 0;
                initial = count();
                checkState();
            }
        }
        else if (clusterState == ClusterState.OBSERVING)
        {
            final int suspiciousThreshold = Math.min(CommonConfiguration.config.getCommonConfig().reportThreshold, 100);
            if ((age > 20 * 60 && arrivals > 0 && count() > initial && count() > suspiciousThreshold) || (age > 20 * 60 * 5 && arrivals > 100 && despawns > 0
                && count() >= suspiciousThreshold))
            {
                clusterState = ClusterState.SUSPICIOUS;
                checkState();
            }
        }
        else if (clusterState == ClusterState.SUSPICIOUS)
        {
            // Normal leak
            if (age > 20 * 90 && count() >= CommonConfiguration.config.getCommonConfig().reportThreshold && arrivals > 30)
            {
                sendNotification(createCountLeakNotification(), ClusterState.LEAK);
                clusterState = ClusterState.LEAK;

                checkState();
            }
            // Item despawn waste leak, most items being produced keep despawning
            else if (age > 20 * 60 * 8 && count() >= 100 && arrivals > 300 && despawns > 300 && (despawns >= arrivals * CommonConfiguration.config.getCommonConfig().wastePercent))
            {
                sendNotification(createWasteLeakNotification(), ClusterState.LEAK);
                clusterState = ClusterState.LEAK;

                checkState();
            }
        }
        else if (clusterState == ClusterState.LEAK && age > 20 * 120)
        {
            if (count() >= CommonConfiguration.config.getCommonConfig().autoremovethreshold)
            {
                sendNotification(createCriticalNotification(), ClusterState.PENDING_DELETION);
                clusterState = ClusterState.PENDING_DELETION;
                autoDeletionTimePoint = level.getGameTime() + CommonConfiguration.config.getCommonConfig().autoRemoveDelay;
                checkState();
            }
        }
        else if (clusterState == ClusterState.PENDING_DELETION)
        {
            if (level.getGameTime() > autoDeletionTimePoint && (level.getGameTime() - lastPlayerPickup > 20 * 60 || level.getGameTime() - autoDeletionTimePoint > 20 * 60 * 5))
            {
                sendNotification(createDeletionNotification(), clusterState);
                clearItems();
            }
        }
    }

    /**
     * Sends a leak detection/deletion notification to the relevant players
     *
     * @param message
     * @param state
     */
    public void sendNotification(Component message, final ClusterState state)
    {
        Set<Player> recipients = new HashSet<>();
        final String configSetting = CommonConfiguration.config.getCommonConfig().reportNotification.toUpperCase(Locale.ROOT);

        if (configSetting.contains("EVERYONE"))
        {
            for (final Player player : level.getServer().getPlayerList().getPlayers())
            {
                if (player instanceof ServerPlayer)
                {
                    recipients.add(player);
                }
            }
            message = message.copy().append("\n").append(Component.translatable(level.dimension().location().toLanguageKey()).withStyle(ChatFormatting.DARK_AQUA));
        }

        if (configSetting.contains("NEAREST_PLAYER") || configSetting.contains("NEAREST_PLAYER_LIMITED"))
        {
            double currentClosestDist = Double.MAX_VALUE;
            Player closest = null;
            for (final Player player : level.players())
            {
                final double playerDist = player.blockPosition().distSqr(position);
                if (player instanceof ServerPlayer && playerDist < currentClosestDist && (!configSetting.contains("NEAREST_PLAYER_LIMITED") || playerDist < 200 * 200))
                {
                    currentClosestDist = playerDist;
                    closest = player;
                }
            }

            if (closest != null)
            {
                recipients.add(closest);
            }
        }

        if (configSetting.contains("NEAREST_OP"))
        {
            double dist = Double.MAX_VALUE;
            Player closest = null;
            for (final Player player : level.getServer().getPlayerList().getPlayers())
            {
                final double playerDist = player.blockPosition().distSqr(position);
                if (level.getServer().getPlayerList().isOp(player.getGameProfile()) && playerDist < dist)
                {
                    dist = playerDist;
                    closest = player;
                }
            }
            if (closest != null)
            {
                recipients.add(closest);
            }
        }

        if (configSetting.contains("ALL_OP"))
        {
            for (final Player player : level.getServer().getPlayerList().getPlayers())
            {
                if (level.getServer().getPlayerList().isOp(player.getGameProfile()))
                {
                    recipients.add(player);
                }
            }

            message = message.copy().append("\n").append(Component.translatable(level.dimension().location().toLanguageKey()).withStyle(ChatFormatting.DARK_AQUA)).append("\n");
        }

        for (Player player : recipients)
        {
            if (level.getServer().getPlayerList().isOp(player.getGameProfile()))
            {
                var opMessage = message.copy();
                opMessage.append(createInspectComponent(id, level.dimension()));
                if (clusterState == ClusterState.PENDING_DELETION)
                {
                    opMessage.append(createRestoreComponent(id, level.dimension()));
                }

                player.sendSystemMessage(opMessage);
            }
            else
            {
                player.sendSystemMessage(message);
            }
        }
    }

    /**
     * Tries to associate the given item with the cluster
     *
     * @param itemEntity
     * @return
     */
    public boolean tryAdd(final ItemEntity itemEntity)
    {
        if (itemEntity instanceof IClusterItem clusterItem && clusterItem.getCluster() == null)
        {
            clusterItem.setCluster(this);
            arrivals++;
            itemEntities.add(itemEntity);
            return true;
        }

        return false;
    }

    public void onDespawn(final ItemEntity itemEntity)
    {
        despawns++;
        itemEntities.remove(itemEntity);
    }

    public void onPickUp(final ItemEntity itemEntity)
    {
        pickups++;
        itemEntities.remove(itemEntity);
        lastPlayerPickup = level.getGameTime();
    }

    public void pruneRemovedEntities()
    {
        int count = itemEntities.size();
        itemEntities.removeIf(entity -> entity.isRemoved() || entity.getItem().isEmpty());
        int removals = count - itemEntities.size();
        otherRemovals += removals;
        lastPruneTime = level.getGameTime();
    }

    /**
     * Removes al items
     */
    public void clearItems()
    {
        ClusterHistory history = new ClusterHistory(id, position, itemEntities, level.getGameTime(), clusterState, level.dimension());
        itemEntities.forEach(ItemEntity::discard);
        itemEntities.clear();
        if (level instanceof ServerLevelClusterManager serverLevelClusterManager)
        {
            serverLevelClusterManager.leaky$getItemClusterManager().addHistory(history);
        }
    }

    public int count()
    {
        if (level.getGameTime() - lastPruneTime > 20 * 5)
        {
            pruneRemovedEntities();
        }

        return itemEntities.size();
    }

    public ClusterState getClusterState()
    {
        return clusterState;
    }

    private Component createCountLeakNotification()
    {
        return Component.literal("Item leak detected - #" + id)
            .withStyle(ChatFormatting.GOLD)
            .append("\n")
            .append(Component.literal(count() + " items are continuing to accumulate").withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(Component.literal("Arrivals: " + arrivals + " | Age: " + formatAge((level.getGameTime() - creationTime) / 20)).withStyle(ChatFormatting.GRAY))
            .append("\n")
            .append(createLocationComponent(position, level.dimension()));
    }

    private Component createWasteLeakNotification()
    {
        final int wastePercentage = arrivals == 0 ? 0 : (int) Math.round(despawns * 100.0 / arrivals);

        return Component.literal("Despawning item leak detected - #" + id)
            .withStyle(ChatFormatting.GOLD)
            .append("\n")
            .append(Component.literal(despawns + " of " + arrivals + " arriving items have despawned (~" + wastePercentage + "%)").withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(Component.literal(count() + " items remain | Age: " + formatAge((level.getGameTime() - creationTime) / 20)).withStyle(ChatFormatting.GRAY))
            .append("\n")
            .append(createLocationComponent(position, level.dimension()));
    }

    private Component createCriticalNotification()
    {
        return Component.literal("Critical item concentration - #" + id)
            .withStyle(ChatFormatting.RED)
            .append("\n")
            .append(Component.literal(count() + " dropped items have reached the automatic cleanup threshold").withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(Component.literal("Leaky will clean this cluster if it remains unattended.").withStyle(ChatFormatting.GRAY))
            .append("\n")
            .append(createLocationComponent(position, level.dimension()));
    }

    public Component createDeletionNotification()
    {
        return Component.literal("Deleting critical item concentration - #" + id)
            .withStyle(ChatFormatting.RED)
            .append("\n")
            .append(Component.literal(count() + " dropped items have reached the automatic cleanup threshold").withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(Component.literal("Leaky removed those items successfully").withStyle(ChatFormatting.GRAY))
            .append("\n")
            .append(createLocationComponent(position, level.dimension()));
    }

    /**
     * Generates the status report for the cluster
     *
     * @return
     */
    public Component minimalStatusReport()
    {
        final long ageSeconds = (level.getGameTime() - creationTime) / 20;
        final int count = count();

        return Component.literal("Item Cluster #" + id + " - ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(clusterState.toString()).withStyle(getStateColor(clusterState)))
            .append("\n")
            .append(Component.literal(count + " items").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" | " + formatAge(ageSeconds) + " old").withStyle(ChatFormatting.GRAY))
            .append("\n")
            .append(createInspectComponent(id, level.dimension())).append(" ").append(createLocationComponent(position, level.dimension()));
    }

    /**
     * Generates the status report for the cluster
     *
     * @return
     */
    public Component inspectStatus()
    {
        final long ageSeconds = (level.getGameTime() - creationTime) / 20;
        final int count = count();

        return Component.literal("Item Cluster #" + id + " - ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(clusterState.toString()).withStyle(getStateColor(clusterState)))
            .append("\n")
            .append(Component.literal(count + " items").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" | " + formatAge(ageSeconds) + " old").withStyle(ChatFormatting.GRAY))
            .append("\n")
            .append(createInspectComponent(id, level.dimension())).append(" ").append(createLocationComponent(position, level.dimension()))
            .append("\n")
            .append(Component.literal("Arrivals: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(Integer.toString(arrivals)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" | Despawned: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(Integer.toString(despawns)).withStyle(ChatFormatting.RED))
            .append(Component.literal(" | Picked up: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(Integer.toString(pickups)).withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" | Unknown removals: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(Integer.toString(otherRemovals)).withStyle(ChatFormatting.AQUA));
    }
}
