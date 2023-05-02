package com.leaky.mixin;

import com.leaky.Leaky;
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
    @Shadow private int age;

    public ItemEntityMixin(final EntityType<?> entityTypeIn, final Level worldIn)
    {
        super(entityTypeIn, worldIn);
    }

    @Unique
    ItemEntity self = (ItemEntity) (Object) this;

    @Unique
    boolean reported = false;


    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void checkSize(CallbackInfo ci)
    {
        if (reported || age < 20 * 60 || tickCount % 40 != 0 || Leaky.rand.nextInt(10) != 0)
        {
            return;
        }

        List<ItemEntity> items = this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.5D, 0.0D, 0.5D));

        if (items.size() > Leaky.getConfig().getCommonConfig().reportThreshold)
        {
            if (getLevel().isClientSide && Leaky.getConfig().getCommonConfig().highlightitems)
            {
                for (final ItemEntity item : items)
                {
                    item.setSharedFlag(6, true);
                }
            }

            reported = true;

            if (!level.isClientSide)
            {
                Leaky.detectedItemLeak(self, items);
            }
        }
    }
}