package com.leaky.mixin;

import com.leaky.Leaky;
import com.leaky.config.CommonConfiguration;
import com.leaky.storage.DetectionSource;
import com.leaky.storage.ServerLevelClusterManager;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(EntitySection.class)
public class EntitySectionMixin<T extends EntityAccess>
{
    @Shadow
    @Final
    private ClassInstanceMultiMap<T> storage;

    @Unique
    private long lastReportTime = 0;

    @Unique
    private long lastReportAmount = 0;

    @Inject(method = "add", at = @At("RETURN"))
    private void leaky$addEntity(final T entity, final CallbackInfo ci)
    {
        final int storageSize = storage.size();
        if (entity instanceof ItemEntity && storageSize > CommonConfiguration.config.getCommonConfig().autoremovethreshold)
        {
            Collection<ItemEntity> collection = this.storage.find(ItemEntity.class);

            if (collection.size() > CommonConfiguration.config.getCommonConfig().autoremovethreshold)
            {
                if (((ItemEntity) entity).level().isClientSide() && CommonConfiguration.config.getCommonConfig().highlightitems)
                {
                    for (final ItemEntity item : collection)
                    {
                        item.setSharedFlag(6, true);
                    }
                }

                if (((ItemEntity) entity).level() instanceof ServerLevelClusterManager serverLevelClusterManager)
                {
                    serverLevelClusterManager.leaky$getItemClusterManager()
                        .detectedItemLeak(((ItemEntity) entity), new ArrayList<>(collection), DetectionSource.ENTITY_SECTION);
                }
            }
        }

        if (!(entity instanceof ItemEntity) && storageSize > CommonConfiguration.config.getCommonConfig().entitySectionLogThreshold && entity instanceof Entity realEntity
            && realEntity.level() != null && (realEntity.level().getGameTime() - lastReportTime) > 20 * 60 * 5)
        {
            Collection<ItemEntity> collection = this.storage.find(ItemEntity.class);
            int actualSize = storageSize - collection.size();

            if (actualSize > CommonConfiguration.config.getCommonConfig().entitySectionLogThreshold && actualSize > lastReportAmount + 10)
            {
                lastReportAmount = actualSize;
                lastReportTime = realEntity.level().getGameTime();
                Leaky.LOGGER.warn("Detected large amount of entities: "+actualSize+" in: "+realEntity.level().dimension().toString()+" pos:"+realEntity.blockPosition(), new Exception());
            }
        }
    }
}
