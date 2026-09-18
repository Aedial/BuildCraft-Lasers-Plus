package com.bclasersplus;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.silicon.block.BlockLaser;

import com.bclasersplus.config.BCLaserPlusConfig;


@SuppressWarnings("deprecation")
public class BlockLaserPlus extends BlockLaser {
    private static final AxisAlignedBB AABB_BASE =
            new AxisAlignedBB(0.0D, 12.0D / 16.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final AxisAlignedBB AABB_BEAM =
            new AxisAlignedBB(5.0D / 16.0D, 3.0D / 16.0D, 5.0D / 16.0D,
                    11.0D / 16.0D, 12.0D / 16.0D, 11.0D / 16.0D);
    private static final AxisAlignedBB AABB = AABB_BASE.union(AABB_BEAM);

    private final int index;

    public BlockLaserPlus(int index) {
        super(Material.IRON, Tags.MODID + ".block.laser_plus_" + index);

        this.index = index;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);

        int factor = BCLaserPlusConfig.getLaserFactor(index);
        tooltip.add(I18n.format("tooltip.bclasersplus.laser_plus", factor));
    }

    @Override
    @Nonnull
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    private AxisAlignedBB getRotatedAABB(@Nonnull IBlockState state, @Nonnull AxisAlignedBB aabb) {
        return RotationUtil.rotateAABB(aabb, state.getValue(getFacingProperty()));
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
            @Nonnull BlockPos pos) {
        return getRotatedAABB(state, AABB);
    }

    @Override
    public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos,
            @Nonnull AxisAlignedBB entityBox, @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
            boolean isPistonMoving) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, getRotatedAABB(state, AABB_BASE));
        addCollisionBoxToList(pos, entityBox, collidingBoxes, getRotatedAABB(state, AABB_BEAM));
    }

    @Override
    @Nullable
    public RayTraceResult collisionRayTrace(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos,
            @Nonnull Vec3d start, @Nonnull Vec3d end) {
        RayTraceResult base = super.rayTrace(pos, start, end, getRotatedAABB(state, AABB_BASE));
        RayTraceResult beam = super.rayTrace(pos, start, end, getRotatedAABB(state, AABB_BEAM));

        if (base != null) base.subHit = 0;
        if (beam != null) beam.subHit = 1;
        if (base == null) return beam;
        if (beam == null) return base;

        return base.hitVec.squareDistanceTo(start) <= beam.hitVec.squareDistanceTo(start) ? base : beam;
    }

    @Override
    @Nonnull
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(@Nonnull IBlockState state, @Nonnull World world,
            @Nonnull BlockPos pos) {
        AxisAlignedBB aabb = AABB;

        RayTraceResult trace = Minecraft.getMinecraft().objectMouseOver;
        if (trace != null && pos.equals(trace.getBlockPos())) {
            if (trace.subHit == 0) aabb = AABB_BASE;
            else if (trace.subHit == 1) aabb = AABB_BEAM;
        }

        return getRotatedAABB(state, aabb).offset(pos);
    }

    int getIndex() {
        return index;
    }

    @Override
    public TileBC_Neptune createTileEntity(World world, IBlockState state) {
        return new TileLaserPlus(index);
    }
}
