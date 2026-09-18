package com.bclasersplus.mixin;

import org.spongepowered.asm.mixin.Mixin;

import buildcraft.lib.block.LocalBlockUpdateNotifier;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.silicon.tile.TileLaser;


@Mixin(TileLaser.class)
public abstract class MixinTileLaserListener extends TileBC_Neptune {
    // https://github.com/BuildCraft/BuildCraft/pull/4767
    // Prevent lasers from leaking their listener
    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (!world.isRemote) {
            LocalBlockUpdateNotifier.instance(world).removeSubscriberFromUpdateNotifications((TileLaser) (Object) this);
        }
    }
}
