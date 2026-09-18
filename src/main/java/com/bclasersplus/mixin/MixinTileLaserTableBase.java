package com.bclasersplus.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.block.state.IBlockState;

import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.silicon.tile.TileLaserTableBase;


@Mixin(TileLaserTableBase.class)
public abstract class MixinTileLaserTableBase extends TileBC_Neptune {
    // https://github.com/BuildCraft/BuildCraft/pull/4768
    // Fix lasers sometimes stopping working randomly
    @Override
    public void onLoad() {
        super.onLoad();
        if (!world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 0);
        }
    }
}
