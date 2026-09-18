package com.bclasersplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import buildcraft.api.mj.ILaserTargetBlock;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.silicon.block.BlockLaser;
import buildcraft.silicon.tile.TileLaser;


@Mixin(TileLaser.class)
public abstract class MixinTileLaserWorldUpdate {
    // https://github.com/BuildCraft/BuildCraft/pull/4764
    // Skips target re-scan when no blocks changed (false-positives)
    @Inject(method = "setWorldUpdated", at = @At("HEAD"), cancellable = true, remap = false)
    private void updateForMaterialChange(World world, BlockPos eventPos, IBlockState oldState,
            IBlockState newState, int flags, CallbackInfo ci) {
        
        // We still need to update if the laser's facing changed, so we don't cancel in that case
        boolean laserFacingChanged = eventPos.equals(((TileLaser) (Object) this).getPos())
            && oldState.getBlock() instanceof BlockLaser
            && oldState.getBlock() == newState.getBlock()
            && oldState.getValue(BuildCraftProperties.BLOCK_FACING_6)
                != newState.getValue(BuildCraftProperties.BLOCK_FACING_6);
        if (laserFacingChanged) {
            return;
        }

        if (oldState.getMaterial() == newState.getMaterial()
            // Lasers' targets are not skipped, because they might send an update without any change
            && !(oldState.getBlock() instanceof ILaserTargetBlock)
            && !(newState.getBlock() instanceof ILaserTargetBlock)) {
            ci.cancel();
        }
    }
}
