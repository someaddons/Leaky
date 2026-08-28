package com.leaky.mixin.storage;

import com.leaky.Leaky;
import com.leaky.storage.IClusterItem;
import com.leaky.storage.ItemCluster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ClusterItemMixin implements IClusterItem
{
    @Unique
    private ItemCluster cluster = null;

    @Override
    public void setCluster(ItemCluster cluster)
    {
        if (this.cluster != null)
        {
            Leaky.LOGGER.warn("Duplicate association?!");
        }

        this.cluster = cluster;
    }

    @Override
    public ItemCluster getCluster()
    {
        return cluster;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1))
    private void onDiscard(final CallbackInfo ci)
    {
        if (cluster == null)
        {
            return;
        }

        cluster.onDespawn((ItemEntity) (Object) this);
        cluster = null;
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void onPickup(final Player p_32040_, final CallbackInfo ci)
    {
        if (cluster == null)
        {
            return;
        }

        cluster.onPickUp((ItemEntity) (Object) this);
        cluster = null;
    }
}
