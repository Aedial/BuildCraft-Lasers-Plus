package com.bclasersplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;

import buildcraft.api.mj.MjBattery;
import buildcraft.silicon.tile.TileLaser;

import com.bclasersplus.duck.LaserBatteryCapacityAccess;
import com.bclasersplus.duck.MjBatteryCapacityAccess;


@Mixin(value = TileLaser.class, remap = false)
public abstract class MixinTileLaserBatteryCapacity implements LaserBatteryCapacityAccess {
    @Shadow @Final private MjBattery battery;

    @Override
    public void bcLaserPlus$setBatteryCapacity(long capacity) {
        ((MjBatteryCapacityAccess) battery).bcLaserPlus$setCapacity(capacity);
    }
}
