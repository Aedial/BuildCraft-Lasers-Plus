package com.bclasersplus.mixin;

import java.util.Objects;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;

import buildcraft.api.core.SafeTimeTracker;
import buildcraft.lib.misc.data.AverageLong;
import buildcraft.silicon.tile.TileLaser;


@Mixin(value = TileLaser.class, remap = false)
public abstract class MixinTileLaserRenderUpdate {
    @Shadow private BlockPos targetPos;
    @Shadow @Final private AverageLong avgPower;

    @Shadow public abstract long getMaxPowerPerTick();

    // https://github.com/BuildCraft/BuildCraft/pull/4766
    // Sends render data after visible changes and every ten seconds for current energy data
    @Unique private final SafeTimeTracker bcLaserPlus$forcedEnergySyncInterval = new SafeTimeTracker(20 * 10, 20);
    @Unique private BlockPos bcLaserPlus$lastRenderTargetPos;
    @Unique private int bcLaserPlus$lastRenderState = -1;

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lbuildcraft/silicon/tile/TileLaser;sendNetworkUpdate(I)V",
            remap = false
        ),
        remap = true
    )
    private void sendRenderDataWhenChanged(TileLaser laser, int id) {
        int renderState = bcLaserPlus$getRenderState();
        if (Objects.equals(bcLaserPlus$lastRenderTargetPos, targetPos)
                && bcLaserPlus$lastRenderState == renderState
                && !bcLaserPlus$forcedEnergySyncInterval.markTimeIfDelay(laser.getWorld())) {
            return;
        }

        bcLaserPlus$lastRenderTargetPos = targetPos;
        bcLaserPlus$lastRenderState = renderState;
        laser.sendNetworkUpdate(id);
    }

    @Unique
    private int bcLaserPlus$getRenderState() {
        long average = (long) avgPower.getAverage();
        if (average <= 200_000) {
            return 0;
        }

        average += 200_000;
        int index = (int) (average * 3 / getMaxPowerPerTick());
        return Math.min(index, 3) + 1;
    }
}
