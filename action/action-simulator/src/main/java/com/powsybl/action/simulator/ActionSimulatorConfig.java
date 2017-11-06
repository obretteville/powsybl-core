/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.simulator;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

public class ActionSimulatorConfig {
    private static final PropertyMode DEFAULT_PROPERTY_MODE = PropertyMode.SAFE;

    public enum PropertyMode {
        SAFE,
        UNSAFE
    }

    public static ActionSimulatorConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static ActionSimulatorConfig load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        boolean exists = platformConfig.moduleExists("action-simulator");
        PropertyMode mode;
        if (exists) {
            ModuleConfig config = platformConfig.getModuleConfig("action-simulator");
            PropertyMode propertyMode = config.getEnumProperty("property-mode", PropertyMode.class, PropertyMode.SAFE);
            return new ActionSimulatorConfig(propertyMode);
        } else {
            return new ActionSimulatorConfig(DEFAULT_PROPERTY_MODE);
        }
    }

    private PropertyMode propertyMode;


    public ActionSimulatorConfig(PropertyMode propertyMode) {
        this.propertyMode = Objects.requireNonNull(propertyMode);
    }

    public PropertyMode getPropertyMode() {
        return propertyMode;
    }
}
