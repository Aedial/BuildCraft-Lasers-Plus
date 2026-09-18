package com.bclasersplus.mixin;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import gnu.trove.map.hash.TLongIntHashMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import buildcraft.lib.client.render.laser.LaserCompiledBuffer;
import buildcraft.lib.client.render.laser.LaserCompiledList;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;

import com.bclasersplus.duck.LaserCompiledBufferAccess;


@Mixin(value = LaserRenderer_BC8.class, remap = false)
public abstract class MixinLaserRenderer {
    @Shadow @Final private static LoadingCache<LaserData_BC8, LaserCompiledList> COMPILED_STATIC_LASERS;
    @Shadow @Final private static LoadingCache<LaserData_BC8, LaserCompiledBuffer> COMPILED_DYNAMIC_LASERS;

    // https://github.com/BuildCraft/BuildCraft/pull/4766
    // Improves performance by caching light samples during a client tick
    @Unique private static final TLongIntHashMap bcLaserPlus$blockLightmapCache = new TLongIntHashMap();
    @Unique private static final TLongIntHashMap bcLaserPlus$skyLightmapCache = new TLongIntHashMap();
    @Unique private static WeakReference<World> bcLaserPlus$dynamicLaserWorld = new WeakReference<>(null);
    @Unique private static WeakReference<World> bcLaserPlus$lightmapCacheWorld = new WeakReference<>(null);
    @Unique private static long bcLaserPlus$lightmapCacheTick = Long.MIN_VALUE;

    @SuppressWarnings("rawtypes")
    @Redirect(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/cache/CacheBuilder;expireAfterWrite(JLjava/util/concurrent/TimeUnit;)Lcom/google/common/cache/CacheBuilder;",
            ordinal = 1
        )
    )
    private static CacheBuilder configureDynamicLaserCache(CacheBuilder cache, long duration, TimeUnit unit) {
        return cache.maximumSize(4096).expireAfterAccess(60, TimeUnit.SECONDS);
    }

    @Inject(method = "clearModels", at = @At("TAIL"))
    private static void clearLaserCaches(CallbackInfo ci) {
        COMPILED_STATIC_LASERS.invalidateAll();
        COMPILED_DYNAMIC_LASERS.invalidateAll();
        bcLaserPlus$dynamicLaserWorld = new WeakReference<>(null);
        bcLaserPlus$clearLightmapCache();
    }

    @Inject(method = "computeLightmap", at = @At("HEAD"))
    private static void updateLightmapCache(double x, double y, double z, int minBlockLight, CallbackInfoReturnable<Integer> cir) {
        World world = Minecraft.getMinecraft().world;
        if (world != null) {
            bcLaserPlus$updateLightmapCache(world);
        }
    }

    @Redirect(
        method = "getLightFor",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getLightFor(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)I",
            remap = true
        )
    )
    private static int getCachedLight(World world, EnumSkyBlock type, BlockPos pos) {
        TLongIntHashMap lightmapCache = type == EnumSkyBlock.BLOCK
            ? bcLaserPlus$blockLightmapCache : bcLaserPlus$skyLightmapCache;
        long packedPos = pos.toLong();
        if (lightmapCache.containsKey(packedPos)) {
            return lightmapCache.get(packedPos);
        }

        int light = world.getLightFor(type, pos);
        lightmapCache.put(packedPos, light);
        return light;
    }

    @Inject(method = "renderLaserDynamic", at = @At("HEAD"))
    private static void clearDynamicLasersForNewWorld(LaserData_BC8 data, BufferBuilder buffer, CallbackInfo ci) {
        World world = Minecraft.getMinecraft().world;
        if (bcLaserPlus$dynamicLaserWorld.get() != world) {
            COMPILED_DYNAMIC_LASERS.invalidateAll();
            bcLaserPlus$dynamicLaserWorld = new WeakReference<>(world);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
        method = "renderLaserDynamic",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/cache/LoadingCache;getUnchecked(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private static Object refreshDynamicLightmap(LoadingCache cache, Object key) {
        LaserData_BC8 data = (LaserData_BC8) key;
        LaserCompiledBuffer compiled = (LaserCompiledBuffer) cache.getUnchecked(key);
        LaserCompiledBufferAccess access = (LaserCompiledBufferAccess) compiled;
        access.bcLaserPlus$setMinBlockLight(data.minBlockLight);
        access.bcLaserPlus$refreshLightmapIfNeeded(System.nanoTime());
        return compiled;
    }

    @Unique
    private static void bcLaserPlus$updateLightmapCache(World world) {
        long worldTime = world.getTotalWorldTime();
        if (bcLaserPlus$lightmapCacheWorld.get() == world && bcLaserPlus$lightmapCacheTick == worldTime) {
            return;
        }

        bcLaserPlus$blockLightmapCache.clear();
        bcLaserPlus$skyLightmapCache.clear();
        bcLaserPlus$lightmapCacheWorld = new WeakReference<>(world);
        bcLaserPlus$lightmapCacheTick = worldTime;
    }

    @Unique
    private static void bcLaserPlus$clearLightmapCache() {
        bcLaserPlus$blockLightmapCache.clear();
        bcLaserPlus$skyLightmapCache.clear();
        bcLaserPlus$lightmapCacheWorld = new WeakReference<>(null);
        bcLaserPlus$lightmapCacheTick = Long.MIN_VALUE;
    }
}
