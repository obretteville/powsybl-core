/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ampl.converter;

import com.google.common.io.CharStreams;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AmplNetworkWriterTest {

    private void assertEqualsToRef(MemDataSource dataSource, String suffix, String refFileName) throws IOException {
        assertEquals(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/" + refFileName), StandardCharsets.UTF_8)),
                     new String(dataSource.getData(suffix, "txt"), StandardCharsets.UTF_8));
    }

    @Test
    public void test() {
        AmplExporter exporter = new AmplExporter();
        Assert.assertEquals("AMPL", exporter.getFormat());
    }

    @Test
    public void writeEurostag() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();

        MemDataSource dataSource = new MemDataSource();
        AmplExporter exporter = new AmplExporter();
        exporter.export(network, new Properties(), dataSource);

        assertEqualsToRef(dataSource, "_network_substations", "inputs/eurostag-tutorial-example1-substations.txt");
        assertEqualsToRef(dataSource, "_network_buses", "inputs/eurostag-tutorial-example1-buses.txt");
        assertEqualsToRef(dataSource, "_network_tct", "inputs/eurostag-tutorial-example1-tct.txt");
        assertEqualsToRef(dataSource, "_network_rtc", "inputs/eurostag-tutorial-example1-rtc.txt");
        assertEqualsToRef(dataSource, "_network_ptc", "inputs/eurostag-tutorial-example1-ptc.txt");
        assertEqualsToRef(dataSource, "_network_loads", "inputs/eurostag-tutorial-example1-loads.txt");
        assertEqualsToRef(dataSource, "_network_shunts", "inputs/eurostag-tutorial-example1-shunts.txt");
        assertEqualsToRef(dataSource, "_network_generators", "inputs/eurostag-tutorial-example1-generators.txt");
        assertEqualsToRef(dataSource, "_network_limits", "inputs/eurostag-tutorial-example1-limits.txt");
    }

    @Test
    public void writePhaseTapChanger() throws IOException {
        Network network = PhaseShifterTestCaseFactory.create();

        MemDataSource dataSource = new MemDataSource();
        export(network, dataSource);

        assertEqualsToRef(dataSource, "_network_ptc", "inputs/ptc-test-case.txt");
    }

    @Test
    public void writeSVC() throws IOException {
        Network network = SvcTestCaseFactory.create();

        MemDataSource dataSource = new MemDataSource();
        export(network, dataSource);

        assertEqualsToRef(dataSource, "_network_static_var_compensators", "inputs/svc-test-case.txt");
    }

    @Test
    public void writeLcc() throws IOException {
        Network network = HvdcTestNetwork.createLcc();

        MemDataSource dataSource = new MemDataSource();
        export(network, dataSource);

        assertEqualsToRef(dataSource, "_network_hvdc", "inputs/lcc-test-case.txt");
    }

    @Test
    public void writeVsc() throws IOException {
        Network network = HvdcTestNetwork.createVsc();

        MemDataSource dataSource = new MemDataSource();
        export(network, dataSource);

        assertEqualsToRef(dataSource, "_network_hvdc", "inputs/vsc-test-case.txt");
    }

    private static void export(Network network, DataSource dataSource) {
        AmplExporter exporter = new AmplExporter();
        exporter.export(network, new Properties(), dataSource);
    }
}