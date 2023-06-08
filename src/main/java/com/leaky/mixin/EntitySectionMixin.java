package com.leaky.mixin;

import com.leaky.Leaky;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    @Inject(method = "add", at = @At("HEAD"))
    private void leaky$addEntity(final T entity, final CallbackInfo ci)
    {
        if (entity instanceof ItemEntity && storage.size() > Leaky.config.getCommonConfig().reportThreshold)
        {
            Collection<ItemEntity> collection = this.storage.find(ItemEntity.class);

            if (collection.size() > Leaky.config.getCommonConfig().reportThreshold)
            {
                if (((ItemEntity) entity).level.isClientSide && Leaky.config.getCommonConfig().highlightitems)
                {
                    for (final ItemEntity item : collection)
                    {
                        item.setSharedFlag(6, true);
                    }
                }

                if (!((ItemEntity) entity).level.isClientSide)
                {
                    Leaky.detectedItemLeak(((ItemEntity) entity), new ArrayList<>(collection));
                }
            }
        }
    }
}
