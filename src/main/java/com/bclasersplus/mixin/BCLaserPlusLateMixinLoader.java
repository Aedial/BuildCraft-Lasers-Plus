package com.bclasersplus.mixin;

import java.util.ArrayList;
import java.util.List;

import zone.rong.mixinbooter.ILateMixinLoader;

import com.bclasersplus.config.BCLaserPlusConfig;


public class BCLaserPlusLateMixinLoader implements ILateMixinLoader {
    private static final String MIXIN_CONFIG_PREFIX = "mixins.bclasersplus.";

    @Override
    public List<String> getMixinConfigs() {
        BCLaserPlusConfig.load();

        List<String> configs = new ArrayList<>();

        if (BCLaserPlusConfig.mixins.stopSpuriousSearchMixin) {
            configs.add(MIXIN_CONFIG_PREFIX + "4764.json");
        }

        if (BCLaserPlusConfig.mixins.getLaserBlockMixin) {
            configs.add(MIXIN_CONFIG_PREFIX + "4765.json");
        }

        if (BCLaserPlusConfig.mixins.performanceMixin) {
            configs.add(MIXIN_CONFIG_PREFIX + "4766.json");
        }

        if (BCLaserPlusConfig.mixins.fixLaserListenerLeaking) {
            configs.add(MIXIN_CONFIG_PREFIX + "4767.json");
        }

        if (BCLaserPlusConfig.mixins.fixLaserNotWorkingMixin) {
            configs.add(MIXIN_CONFIG_PREFIX + "4768.json");
        }

        if (BCLaserPlusConfig.mixins.fixLaserPowerOverflow) {
            configs.add(MIXIN_CONFIG_PREFIX + "4769.json");
        }

        if (BCLaserPlusConfig.mixins.batteryCapacityMixin) {
            configs.add(MIXIN_CONFIG_PREFIX + "4770.json");
        }

        return configs;
    }
}
