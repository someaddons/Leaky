package com.leaky.mixin;

import com.leaky.Leaky;
import com.leaky.config.CommonConfiguration;
import com.leaky.storage.DetectionSource;
import com.leaky.storage.IClusterItem;
import com.leaky.storage.ServerLevelClusterManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemEntity.class)
/**
 * Reports too many items at one place, to find leaking farms
 */
public abstract class ItemEntityMixin extends Entity
{
    @Shadow
    private int age;

    public ItemEntityMixin(final EntityType<?> entityTypeIn, final Level worldIn)
    {
        super(entityTypeIn, worldIn);
    }

    @Unique
    ItemEntity self = (ItemEntity) (Object) this;

    @Unique
    boolean checked = false;

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void checkSize(CallbackInfo ci)
    {
        if (checked || age < 20 * 60 || tickCount % 400 != 0 || (this instanceof IClusterItem iClusterItem && iClusterItem.getCluster() != null))
        {
            return;
        }

        checked = true;

        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(2.5D, 1.0D, 2.5D));

        if (level().isClientSide && CommonConfiguration.config.getCommonConfig().highlightitems && items.size() > CommonConfiguration.config.getCommonConfig().reportThreshold)
        {
            for (final ItemEntity item : items)
            {
                item.setSharedFlag(6, true);
            }
        }

        if (items.size() > CommonConfiguration.config.getCommonConfig().detectionThreshold)
        {
            if (level() instanceof ServerLevelClusterManager serverLevelClusterManager)
            {
                serverLevelClusterManager.leaky$getItemClusterManager().detectedItemLeak(self, items, DetectionSource.ITEM_TICK);
            }
        }
    }
}