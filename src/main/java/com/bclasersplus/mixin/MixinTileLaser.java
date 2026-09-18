package com.bclasersplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.block.BlockLaser;
import buildcraft.silicon.tile.TileLaser;


@Mixin(TileLaser.class)
public abstract class MixinTileLaser {
    // https://github.com/BuildCraft/BuildCraft/pull/4765
    // Allows custom laser blocks to search for targets
    @Redirect(
        method = "findPossibleTargets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/state/IBlockState;getBlock()Lnet/minecraft/block/Block;",
            ordinal = 0
        )
    )
    private Block getLaserBlock(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockLaser ? BCSiliconBlocks.laser : block;
    }
}
