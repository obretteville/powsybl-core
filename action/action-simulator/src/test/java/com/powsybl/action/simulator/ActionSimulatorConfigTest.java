/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.simulator;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;

import static org.junit.Assert.assertEquals;

public class ActionSimulatorConfigTest {

    @Test
    public void test() throws IOException {
        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix())) {
            InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
            // missing action-simulator module
            ActionSimulatorConfig config = ActionSimulatorConfig.load(platformConfig);
            assertEquals(ActionSimulatorConfig.PropertyMode.SAFE, config.getPropertyMode());

            // mising property-mode
            MapModuleConfig moduleConfig = platformConfig.createModuleConfig("action-simulator");
            config = ActionSimulatorConfig.load(platformConfig);
            assertEquals(ActionSimulatorConfig.PropertyMode.SAFE, config.getPropertyMode());

            moduleConfig.setStringProperty("property-mode", "UNSAFE");
            config = ActionSimulatorConfig.load(platformConfig);
            assertEquals(ActionSimulatorConfig.PropertyMode.UNSAFE, config.getPropertyMode());
        }
    }

}
