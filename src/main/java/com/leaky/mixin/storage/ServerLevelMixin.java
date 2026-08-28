package com.leaky.mixin.storage;

import com.leaky.storage.ItemClusterManager;
import com.leaky.storage.ServerLevelClusterManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements ServerLevelClusterManager
{
    @Unique
    private final ItemClusterManager clusterManager = new ItemClusterManager((Level) (Object) this);

    @Inject(method = "tick", at = @At("HEAD"))
    private void beforeTick(final BooleanSupplier p_8794_, final CallbackInfo ci)
    {
        clusterManager.onTick((ServerLevel) (Object) this);
    }

    @Override
    public ItemClusterManager leaky$getItemClusterManager()
    {
        return clusterManager;
    }
}
