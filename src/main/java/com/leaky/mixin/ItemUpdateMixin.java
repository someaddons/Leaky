package com.leaky.mixin;

import com.leaky.INearbyItemAwareEntity;
import com.leaky.Leaky;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemEntity.class, priority = 999)
public abstract class ItemUpdateMixin extends Entity implements INearbyItemAwareEntity
{
    @Shadow
    private int age;

    public ItemUpdateMixin(final EntityType<?> p_19870_, final Level p_19871_)
    {
        super(p_19870_, p_19871_);
    }

    @Unique
    private int updateRate = 1;

    @Unique
    private int nearbyItems = 0;

    @Unique
    private boolean waterState = false;

    @Unique
    private Player closePlayer = null;

    @Unique
    private BlockPos previousPos = null;

    @Unique
    private int delay = 0;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"), require = 0)
    private boolean checkCollisions(final Level instance, final Entity entity, final AABB aabb)
    {
        if (tickCount < 100 || (tickCount + getId()) % updateRate == 0)
        {
            return this.level().noCollision(this, this.getBoundingBox().deflate(1.0E-7D));
        }
        else
        {
            return !noPhysics;
        }
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 4, ordinal = 0))
    private int adaptUpdates(final int constant)
    {
        return constant + updateRate;
    }

    @Override
    protected boolean updateInWaterStateAndDoFluidPushing()
    {
        if (tickCount < 20 || (tickCount + getId()) % updateRate == 0)
        {
            return waterState = super.updateInWaterStateAndDoFluidPushing();
        }
        else
        {
            return waterState;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void checkRate(final CallbackInfo ci)
    {
        if (tickCount <= 1)
        {
            closePlayer = level().getNearestPlayer(this, 32.0);
        }

        if ((tickCount + getId()) % 20 == 0)
        {
            calculateUpdateRate();
        }
    }

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void onInteract(final Player p_32040_, final CallbackInfo ci)
    {
        updateRate = 1;
        delay = 300;
    }

    @Unique
    private void calculateUpdateRate()
    {
        updateRate = 1;
        if (tickCount < 20 * 15)
        {
            return;
        }

        if (delay > 0)
        {
            delay -= 20;
            return;
        }

        if (!Leaky.config.getCommonConfig().improveItemPerformance)
        {
            return;
        }

        // Tick slower the longer it exists
        updateRate += tickCount / 200.0;

        // If player is far away tick slower
        if (closePlayer != null && closePlayer.blockPosition().distSqr(blockPosition()) > 32 * 32)
        {
            updateRate += 5;
            age += 5;
        }

        // If many items are stacked slow down ticking and accelerate decay
        if (nearbyItems > 0)
        {
            updateRate += nearbyItems / 10;
            age += (nearbyItems / 15);
        }

        // On movement reset
        if (previousPos != null && previousPos != blockPosition() && !previousPos.equals(blockPosition()))
        {
            updateRate = 1;
            delay = 300;
        }
        previousPos = blockPosition();
    }

    @Override
    public int getNearbyItems()
    {
        return nearbyItems;
    }

    @Override
    public void setNearbyItems(final int items)
    {
        nearbyItems = Math.max(nearbyItems, items);
    }
}
