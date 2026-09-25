package com.bclasersplus;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.core.BCCore;
import buildcraft.lib.registry.RegistryConfig;
import buildcraft.silicon.client.render.RenderLaser;

import com.bclasersplus.config.BCLaserPlusConfig;


@Mod(
    modid = Tags.MODID,
    name = Tags.MODNAME,
    version = Tags.VERSION,
    dependencies = "required-after:buildcraftsilicon@[8.0.0,);after:mixinbooter@[8.0,)",
    guiFactory = "com.bclasersplus.config.BCLaserPlusGuiFactory"
)
public class BCLaserPlus {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Allow buildcraft to enable/disable our laser blocks
        Configuration objectConfig;
        try {
            objectConfig = RegistryConfig.useOtherModConfigFor(Tags.MODID, BCCore.MODID);
        } catch (IllegalStateException ignored) {
            File buildcraftConfigDir = new File(event.getModConfigurationDirectory(), "buildcraft");
            objectConfig = RegistryConfig.setRegistryConfig(Tags.MODID, new File(buildcraftConfigDir, "objects.cfg"));
        }

        BCLaserPlusConfig.load();
        BCLaserPlusDataFixer.register();
        ModRegistry.preInit();

        if (objectConfig.hasChanged()) objectConfig.save();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) bindRenderers();
    }

    @SideOnly(Side.CLIENT)
    private static void bindRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileLaserPlus.class, new RenderLaser());
    }
}
