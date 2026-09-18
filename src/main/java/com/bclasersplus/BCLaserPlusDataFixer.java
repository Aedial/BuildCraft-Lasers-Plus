package com.bclasersplus;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;

import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;


@Mod.EventBusSubscriber(modid = Tags.MODID)
public class BCLaserPlusDataFixer implements IFixableData {
    private static final int DATA_VERSION = 1;

    private static final Map<ResourceLocation, ResourceLocation> blockRenames = new HashMap<>();
    private static final Map<String, String> tileRenames = new HashMap<>();

    static {
        // Migrate owapote's Mk laser IDs to our laser IDs
        addBlockRename("bc_ex_laser:ex_laser_mk2", Tags.MODID + ":laser_plus_0");
        addBlockRename("bc_ex_laser:ex_laser_mk3", Tags.MODID + ":laser_plus_1");
        addBlockRename("bc_ex_laser:ex_laser_mk4", Tags.MODID + ":laser_plus_2");
        addBlockRename("bc_ex_laser:ex_laser_mk5", Tags.MODID + ":laser_plus_3");

        addTileRename("bc_ex_laser", "ex_laser_mk2", Tags.MODID + ":laser_plus");
        addTileRename("bc_ex_laser", "ex_laser_mk3", Tags.MODID + ":laser_plus");
        addTileRename("bc_ex_laser", "ex_laser_mk4", Tags.MODID + ":laser_plus");
        addTileRename("bc_ex_laser", "ex_laser_mk5", Tags.MODID + ":laser_plus");
    }

    private static void addBlockRename(String legacyLocation, String replacementLocation) {
        ResourceLocation legacyId = new ResourceLocation(legacyLocation);
        ResourceLocation replacementId = new ResourceLocation(replacementLocation);

        blockRenames.put(legacyId, replacementId);
    }

    private static void addTileRename(String modID, String legacyId, String replacementId) {
        tileRenames.put(legacyId, replacementId);
        tileRenames.put(modID + ":" + legacyId, replacementId);
    }

    // Registers the tile data fixer before worlds load.
    public static void register() {
        CompoundDataFixer fixer = FMLCommonHandler.instance().getDataFixer();
        ModFixs fixes = fixer.init(Tags.MODID, DATA_VERSION);
        fixes.registerFix(FixTypes.BLOCK_ENTITY, new BCLaserPlusDataFixer());
    }

    @SubscribeEvent
    public static void remapBlocks(RegistryEvent.MissingMappings<Block> missing) {
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : missing.getAllMappings()) {
            ResourceLocation replacementId = blockRenames.get(mapping.key);

            if (replacementId != null) {
                Block replacement = ForgeRegistries.BLOCKS.getValue(replacementId);
                if (replacement != null) mapping.remap(replacement);
            }
        }
    }

    @SubscribeEvent
    public static void remapItems(RegistryEvent.MissingMappings<Item> missing) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : missing.getAllMappings()) {
            ResourceLocation replacementId = blockRenames.get(mapping.key);

            if (replacementId != null) {
                Item replacement = ForgeRegistries.ITEMS.getValue(replacementId);
                if (replacement != null) mapping.remap(replacement);
            }
        }
    }

    @Override
    public int getFixVersion() {
        return DATA_VERSION;
    }

    @Override
    @Nonnull
    public NBTTagCompound fixTagCompound(@Nonnull NBTTagCompound compound) {
        String replacement = tileRenames.get(compound.getString("id"));
        if (replacement != null) compound.setString("id", replacement);

        return compound;
    }
}
