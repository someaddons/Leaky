package com.leaky.storage;

import com.leaky.Leaky;
import com.leaky.util.ComponentUtil;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static com.leaky.storage.ClusterState.getStateColor;
import static com.leaky.util.ComponentUtil.*;

public class ClusterHistory
{
    public final  int                id;
    public final  BlockPos           position;
    private final List<ItemStack>    stacks = new ArrayList<>();
    public final  long               time;
    public final  ClusterState       state;
    public final  ResourceKey<Level> dimension;

    public ClusterHistory(final int id, final BlockPos position, final List<ItemEntity> itemEntities, final long time, final ClusterState state, final ResourceKey<Level> dimension)
    {
        this.id = id;
        this.position = position;
        for (final ItemEntity entity : itemEntities)
        {
            if (!entity.isRemoved() && !entity.getItem().isEmpty())
            {
                stacks.add(entity.getItem().copy());
            }
        }
        this.time = time;
        this.state = state;
        this.dimension = dimension;
    }

    public boolean restore(final CommandContext<CommandSourceStack> context, final ServerLevel level)
    {
        if (!level.dimension().equals(dimension))
        {
            Leaky.LOGGER.warn("Cannot restore cluster: " + id + " as it is trying to restore in dimension: " + level.dimension().location() + " but was saved for dimension: "
                + dimension.location());
            return false;
        }

        for (final ItemStack stack : stacks)
        {
            final ItemEntity entity = EntityType.ITEM.create(level);
            entity.setItem(stack);
            entity.setPos(position.getX(), position.getY(), position.getZ());
            level.addFreshEntity(entity);
        }

        context.getSource().sendSystemMessage(Component.literal("Restored " + stacks.size() + " items at: ").append(ComponentUtil.createLocationComponent(position, level.dimension())));
        return true;
    }

    public Component report(final CommandSourceStack source)
    {
        final long ageSeconds = (source.getLevel().getGameTime() - time) / 20;
        return Component.literal("Deletion History Item Cluster #" + id + " - ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(state.toString()).withStyle(getStateColor(state)))
            .append("\n")
            .append(Component.literal("Deleted " + stacks.size() + " items").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" | " + formatAge(ageSeconds) + " old").withStyle(ChatFormatting.GRAY))
            .append("\n")
            .append(createLocationComponent(position, dimension)).append(" ").append(createRestoreComponent(id, dimension));
    }
}
