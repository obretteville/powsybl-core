/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.util;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class GeneratorScalable extends AbstractScalable {

    private final String id;

    GeneratorScalable(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public float initialValue(Network n) {
        Objects.requireNonNull(n);

        Generator g = n.getGenerator(id);
        return g != null && !Float.isNaN(g.getTerminal().getP()) ? g.getTerminal().getP() : 0;
    }

    @Override
    public void reset(Network n) {
        Objects.requireNonNull(n);

        Generator g = n.getGenerator(id);
        if (g != null) {
            g.setTargetP(0f);
        }
    }

    @Override
    public float maximumValue(Network n) {
        Objects.requireNonNull(n);

        Generator g = n.getGenerator(id);
        return g != null ? g.getMaxP() : 0;
    }

    @Override
    public void listGenerators(Network n, List<Generator> generators, List<String> notFoundGenerators) {
        Objects.requireNonNull(n);
        Objects.requireNonNull(generators);

        Generator g = n.getGenerator(id);
        if (g != null) {
            generators.add(g);
        } else {
            if (notFoundGenerators != null) {
                notFoundGenerators.add(id);
            }
        }
    }

    @Override
    public float scale(Network n, float asked) {
        Objects.requireNonNull(n);

        Generator g = n.getGenerator(id);
        float done = 0;
        if (g != null) {
            Terminal t = g.getTerminal();
            if (!t.isConnected()) {
                t.connect();
                if (g.isVoltageRegulatorOn()) {
                    Bus bus = t.getBusView().getBus();
                    if (bus != null) {
                        // set voltage setpoint to the same as other generators connected to the bus
                        float targetV = Float.NaN;
                        for (Generator g2 : bus.getGenerators()) {
                            targetV = g2.getTargetV();
                            break;
                        }
                        // if no other generator connected to the bus, set voltage setpoint to network voltage
                        if (Float.isNaN(targetV) && !Float.isNaN(bus.getV())) {
                            g.setTargetV(bus.getV());
                        }
                    }
                }
                LOGGER.info("Connecting {}", g.getId());
            }
            done = Math.min(asked, g.getMaxP() - g.getTargetP());
            float oldTargetP = g.getTargetP();
            g.setTargetP(g.getTargetP() + done);
            LOGGER.info("Change active power setpoint of {} from {} to {} (pmax={})",
                    g.getId(), oldTargetP, g.getTargetP(), g.getMaxP());
        } else {
            LOGGER.warn("Generator {} not found", id);
        }
        return done;
    }
}
