package com.leaky;

import com.leaky.storage.ClusterHistory;
import com.leaky.storage.ItemCluster;
import com.leaky.storage.ItemClusterManager;
import com.leaky.storage.ServerLevelClusterManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;

public class Command
{
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(Leaky.MODID).requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_ADMIN)).then(Commands.literal("status").executes(context -> {
                context.getSource().sendSystemMessage(Component.translatable("leaky.status"));
                for (ServerLevel level : context.getSource().getServer().getAllLevels())
                {
                    if (level instanceof ServerLevelClusterManager serverLevelClusterManager)
                    {
                        ItemClusterManager clusterManager = serverLevelClusterManager.leaky$getItemClusterManager();
                        clusterManager.reportStatus(context.getSource());
                    }
                }

                return 1;
            }))
            .then(Commands.literal("inspect").then(Commands.argument("clusterid", IntegerArgumentType.integer()).executes(context -> {
                final int clusterID = IntegerArgumentType.getInteger(context, "clusterid");
                final Level level = context.getSource().getLevel();
                if (level instanceof ServerLevelClusterManager serverLevelClusterManager)
                {
                    ItemClusterManager clusterManager = serverLevelClusterManager.leaky$getItemClusterManager();
                    final ItemCluster cluster = clusterManager.getCluster(clusterID);
                    if (cluster != null)
                    {
                        context.getSource().sendSystemMessage(cluster.inspectStatus());
                    }
                    else
                    {
                        context.getSource().sendSystemMessage(Component.translatable("leaky.command.cluster_not_found"));
                    }
                }
                return 1;
            })))
            .then(Commands.literal("deleteCluster").then(Commands.argument("clusterid", IntegerArgumentType.integer()).executes(context -> {
                final int clusterID = IntegerArgumentType.getInteger(context, "clusterid");
                final Level level = context.getSource().getLevel();
                if (level instanceof ServerLevelClusterManager serverLevelClusterManager)
                {
                    ItemClusterManager clusterManager = serverLevelClusterManager.leaky$getItemClusterManager();
                    if (!clusterManager.removeCluster(clusterID))
                    {
                        context.getSource().sendSystemMessage(Component.translatable("leaky.command.cluster_not_found"));
                    }
                    else
                    {
                        context.getSource().sendSystemMessage(Component.translatable("leaky.command.cluster_removed", clusterID));
                    }
                }
                return 1;
            })))
            .then(Commands.literal("deletionHistory").executes(context -> {
                context.getSource().sendSystemMessage(Component.translatable("leaky.command.history.header"));

                for (ServerLevel level : context.getSource().getServer().getAllLevels())
                {
                    if (level instanceof ServerLevelClusterManager serverLevelClusterManager)
                    {
                        ItemClusterManager clusterManager = serverLevelClusterManager.leaky$getItemClusterManager();
                        clusterManager.reportHistory(context.getSource());
                    }
                }
                return 1;
            }))
            .then(Commands.literal("restore")
                .then(Commands.argument("clusterid", IntegerArgumentType.integer())
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context -> {
                            final int clusterID = IntegerArgumentType.getInteger(context, "clusterid");
                            final ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                            if (level instanceof ServerLevelClusterManager serverLevelClusterManager)
                            {
                                ItemClusterManager clusterManager = serverLevelClusterManager.leaky$getItemClusterManager();
                                final ClusterHistory history = clusterManager.getHistory(clusterID);
                                if (history != null)
                                {
                                    if (history.restore(context, level))
                                    {
                                        clusterManager.removeHistory(clusterID);
                                    }
                                }
                            }
                            return 1;
                        }))));
    }
}
