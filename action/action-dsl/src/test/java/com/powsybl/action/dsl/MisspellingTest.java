package com.powsybl.action.dsl;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import groovy.lang.GroovyCodeSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MisspellingTest {

    private Network network;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        network = EurostagTutorialExample1Factory.create();
    }

    @Test
    public void test() {
        ActionDb actionDb = new ActionDslLoader(new GroovyCodeSource(getClass().getResource("/testMisspelling.groovy")), false).load(network);
        Action someAction = actionDb.getAction("misspelling"); // try to set targettP on GEN
        thrown.expect(PowsyblException.class);
        thrown.expectMessage("GEN has no targettP property");
        someAction.run(network, null);
    }

    @Test
    public void test2() {
        ActionDb actionDb = new ActionDslLoader(new GroovyCodeSource(getClass().getResource("/testMisspelling.groovy")), true).load(network);
        Action someAction = actionDb.getAction("misspelling"); // try to set targettP on GEN
        someAction.run(network, null);
    }
}
