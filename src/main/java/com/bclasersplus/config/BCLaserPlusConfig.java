package com.bclasersplus.config;

import java.io.File;
import java.util.Arrays;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

import buildcraft.silicon.BCSilicon;

import com.bclasersplus.Tags;


/**
 * Server configuration for the BC EXLaser mod.
 * <p>
 * Supports in-game modification via the Forge config GUI.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
@Config(modid = Tags.MODID, name = Tags.MODID, category = "")
@Config.LangKey(Tags.MODID + ".config.title")
public final class BCLaserPlusConfig {

    private static final String PREFIX = Tags.MODID + ".config.";

    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_MIXINS = "mixins";
    private static final String CATEGORY_HIDDEN = "hidden";

    @Config.Name(CATEGORY_GENERAL)
    @Config.LangKey(PREFIX + "category.general")
    @Config.Comment("General settings for general behavior")
    public static GeneralCategory general = new GeneralCategory();

    @Config.Name(CATEGORY_MIXINS)
    @Config.LangKey(PREFIX + "category.mixins")
    @Config.Comment("Settings for mixins")
    public static MixinsCategory mixins = new MixinsCategory();

    @Config.Name(CATEGORY_HIDDEN)
    @Config.Comment("Hidden client preferences")
    public static HiddenCategory hidden = new HiddenCategory();

    private BCLaserPlusConfig() {
    }

    public static boolean isHiddenCategory(String categoryName) {
        return CATEGORY_HIDDEN.equals(categoryName);
    }

    /**
     * Syncs the annotated config.
     *
     * @param configFile The configuration file
     */
    public static void init(File configFile) {
        syncConfig();
    }

    public static void load() {
        syncConfig();
    }

    private static void syncConfig() {
        ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
    }

    private static boolean isBuildcraftVersionBelowOrEqual(String version) {
        ModContainer buildCraft = Loader.instance().getIndexedModList().get(BCSilicon.MODID);
        return buildCraft != null
            && buildCraft.getProcessedVersion().compareTo(new DefaultArtifactVersion(version)) <= 0;
    }

    public static class GeneralCategory {
        @Config.Name("laserFactors")
        @Config.LangKey(PREFIX + "general.laserFactors")
        @Config.Comment({
            "Speed and battery-capacity factors for lasers. " +
            "Each value corresponds to a different laser tier. " +
            "Requires restart for changes to take effect."
        })
        @Config.RangeInt(min = 1)
        public int[] laserFactors = { 4, 16, 64, 256 };
    }

    public static class MixinsCategory {
        @Config.Name("batteryCapacityMixin")
        @Config.LangKey(PREFIX + "mixins.batteryCapacityMixin")
        @Config.Comment("Enable scaling the Laser battery capacity. Requires restart.")
        public boolean batteryCapacityMixin = isBuildcraftVersionBelowOrEqual("8.0.0");

        @Config.Name("stopSpuriousSearchMixin")
        @Config.LangKey(PREFIX + "mixins.stopSpuriousSearchMixin")
        @Config.Comment("Stop false-positive laser target searches. Requires restart.")
        public boolean stopSpuriousSearchMixin = isBuildcraftVersionBelowOrEqual("8.0.0");

        @Config.Name("getLaserBlockMixin")
        @Config.LangKey(PREFIX + "mixins.getLaserBlockMixin")
        @Config.Comment("Allow Lasers to work. Requires restart.")
        public boolean getLaserBlockMixin = isBuildcraftVersionBelowOrEqual("8.0.0");

        @Config.Name("performanceMixin")
        @Config.LangKey(PREFIX + "mixins.performanceMixin")
        @Config.Comment("Enable performance optimizations for Lasers. Requires restart.")
        public boolean performanceMixin = isBuildcraftVersionBelowOrEqual("8.0.0");

        @Config.Name("fixLaserListenerLeaking")
        @Config.LangKey(PREFIX + "mixins.fixLaserListenerLeaking")
        @Config.Comment("Fix the issue where the Laser listener leaks. Requires restart.")
        public boolean fixLaserListenerLeaking = isBuildcraftVersionBelowOrEqual("8.0.0");

        @Config.Name("fixLaserNotWorkingMixin")
        @Config.LangKey(PREFIX + "mixins.fixLaserNotWorkingMixin")
        @Config.Comment("Enable the fix for Lasers stopping working randomly. Requires restart.")
        public boolean fixLaserNotWorkingMixin = isBuildcraftVersionBelowOrEqual("8.0.0");

        @Config.Name("fixLaserPowerOverflow")
        @Config.LangKey(PREFIX + "mixins.fixLaserPowerOverflow")
        @Config.Comment("Prevent output overflow from overfilled laser batteries. Requires restart.")
        public boolean fixLaserPowerOverflow = isBuildcraftVersionBelowOrEqual("8.0.0");

        @Config.Name("assemblyTableRecipeCacheMixin")
        @Config.LangKey(PREFIX + "mixins.assemblyTableRecipeCacheMixin")
        @Config.Comment("Cache selected Assembly Table recipes. Requires restart.")
        public boolean assemblyTableRecipeCacheMixin = isBuildcraftVersionBelowOrEqual("8.0.0");

    }

    public static class HiddenCategory {

    }

    public static int[] getLaserFactors() {
        int[] factors = general.laserFactors;
        if (factors.length == 0) {
            throw new IllegalArgumentException("The laserFactors configuration must contain at least one value");
        }

        return Arrays.copyOf(factors, factors.length);
    }

    public static int getLaserFactor(int index) {
        int[] factors = general.laserFactors;
        if (index < 0 || index >= factors.length) return 1;

        return Math.max(1, factors[index]);
    }
}
