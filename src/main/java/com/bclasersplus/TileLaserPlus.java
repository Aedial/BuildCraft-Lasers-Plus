package com.bclasersplus;

import net.minecraft.block.Block;

import buildcraft.api.mj.MjAPI;
import buildcraft.silicon.tile.TileLaser;

import com.bclasersplus.config.BCLaserPlusConfig;
import com.bclasersplus.duck.LaserBatteryCapacityAccess;


public class TileLaserPlus extends TileLaser {
    private static final long BASE_CAPACITY = 1024L * MjAPI.MJ;

    private int index = -1;
    private int factor = 1;

    public TileLaserPlus() {
    }

    public TileLaserPlus(int index) {
        super();

        setTier(index);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BlockLaserPlus) setTier(((BlockLaserPlus) block).getIndex());
    }

    private void setTier(int index) {
        if (this.index == index) return;

        this.index = index;
        this.factor = BCLaserPlusConfig.getLaserFactor(index);

        if (this instanceof LaserBatteryCapacityAccess) {
            long capacity = BASE_CAPACITY * this.factor;
            ((LaserBatteryCapacityAccess) this).bcLaserPlus$setBatteryCapacity(capacity);
        }
    }

    @Override
    public long getMaxPowerPerTick() {
        return factor * super.getMaxPowerPerTick();
    }
}
