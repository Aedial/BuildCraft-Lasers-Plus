package com.bclasersplus.mixin;

import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import buildcraft.lib.client.render.laser.LaserCompiledBuffer;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;

import com.bclasersplus.duck.LaserCompiledBufferAccess;


@Mixin(value = LaserCompiledBuffer.class, remap = false)
public abstract class MixinLaserCompiledBuffer implements LaserCompiledBufferAccess {
    @Shadow @Final private int vertices;
    @Shadow @Final private double[] da;
    @Shadow @Final private int[] ia;

    // https://github.com/BuildCraft/BuildCraft/pull/4766
    // Improves performance by limiting lightmap refreshes
    @Unique private int bcLaserPlus$minBlockLight;
    @Unique private long bcLaserPlus$lastLightmapRefresh;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initializeLightmapRefresh(int vertices, double[] da, int[] ia, CallbackInfo ci) {
        bcLaserPlus$lastLightmapRefresh = System.nanoTime();
    }

    @Override
    @Unique
    public void bcLaserPlus$setMinBlockLight(int minBlockLight) {
        bcLaserPlus$minBlockLight = minBlockLight;
    }

    @Override
    @Unique
    public void bcLaserPlus$refreshLightmapIfNeeded(long time) {
        if (time - bcLaserPlus$lastLightmapRefresh < TimeUnit.SECONDS.toNanos(5)) {
            return;
        }

        for (int index = 0; index < vertices; index++) {
            int doubleIndex = 5 * index;
            ia[2 * index + 1] = LaserRenderer_BC8.computeLightmap(
                da[doubleIndex], da[doubleIndex + 1], da[doubleIndex + 2], bcLaserPlus$minBlockLight
            );
        }

        bcLaserPlus$lastLightmapRefresh = time;
    }
}
