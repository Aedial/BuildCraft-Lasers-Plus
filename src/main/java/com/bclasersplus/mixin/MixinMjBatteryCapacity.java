package com.bclasersplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import buildcraft.api.mj.MjBattery;

import com.bclasersplus.duck.MjBatteryCapacityAccess;


@Mixin(value = MjBattery.class, remap = false)
public interface MixinMjBatteryCapacity extends MjBatteryCapacityAccess {
    @Override
    @Mutable
    @Accessor("capacity")
    void bcLaserPlus$setCapacity(long capacity);
}
