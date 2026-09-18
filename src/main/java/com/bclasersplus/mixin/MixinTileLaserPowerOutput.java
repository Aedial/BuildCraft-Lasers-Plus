package com.bclasersplus.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import buildcraft.api.mj.MjBattery;
import buildcraft.silicon.tile.TileLaser;


@Mixin(value = TileLaser.class, remap = false)
public abstract class MixinTileLaserPowerOutput {
    @Shadow @Final private MjBattery battery;

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;min(JJ)J",
            ordinal = 0,
            remap = false
        )
    )
    private long calculatePowerOutput(long ignoredPower, long maxPower) {
        long stored = battery.getStored();
        long halfCapacity = battery.getCapacity() / 2;
        long minStoredForMaxPower = Math.max(0, halfCapacity - maxPower);
        if (stored >= minStoredForMaxPower) {
            return maxPower;
        }
        return (long) ((double) maxPower * (stored + maxPower) / halfCapacity);
    }
}
