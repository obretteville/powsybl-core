/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.simulator;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MisspellingDefaultTest extends AbstractLoadFlowRulesEngineTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Override
    protected Network createNetwork() {
        Network network = EurostagTutorialExample1WithTemporaryLimitFactory.create();
        network.getVoltageLevel("VLHV1").getBusBreakerView().getBus("NHV1").setV(380).setAngle(0);
        network.getLine("NHV1_NHV2_2").getTerminal1().setP(300).setQ(100);
        return network;
    }

    @Override
    protected String getDslFile() {
        return "/testMisspelling.groovy";
    }

    @Test
    public void test() {
        thrown.expect(PowsyblException.class);
        thrown.expectMessage("LOAD has no p00 property");
        engine.start(actionDb);
    }
}
