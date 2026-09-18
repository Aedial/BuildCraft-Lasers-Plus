package com.bclasersplus;

import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;

import com.bclasersplus.Tags;
import com.bclasersplus.BlockLaserPlus;
import com.bclasersplus.TileLaserPlus;
import com.bclasersplus.config.BCLaserPlusConfig;


public final class ModRegistry {

    private static final int LASER_COUNT = BCLaserPlusConfig.getLaserFactors().length;

    static {
        TagManager.startBatch();

        for (int index = 0; index < LASER_COUNT; index++) {
            String id = "laser_plus_" + index;
            String locale = Tags.MODID + ".laser_plus." + index;
            TagManager.registerTag(Tags.MODID + ".block." + id).reg(id).locale(locale).model(id);
            TagManager.registerTag("item." + Tags.MODID + ".block." + id).reg(id).locale(locale).model(id);
        }

        TagManager.registerTag(Tags.MODID + ".tile.laser_plus").reg("laser_plus");

        TagManager.endBatch(TagManager.prependTags(Tags.MODID + ":", EnumTagType.REGISTRY_NAME,
            EnumTagType.MODEL_LOCATION).andThen(TagManager.setTab("buildcraft.main")));
    }

    private ModRegistry() {}

    public static void preInit() {
        // Ensures the shared BuildCraft tab exists before block constructors resolve their tag-defined creative tab
        CreativeTabManager.createTab("buildcraft.main");

        RegistrationHelper BCRegistry = new RegistrationHelper();

        for (int index = 0; index < LASER_COUNT; index++) {
            BCRegistry.addBlockAndItem(new BlockLaserPlus(index));
        }

        BCRegistry.registerTile(TileLaserPlus.class, Tags.MODID + ".tile.laser_plus");
    }
}
