package com.powsybl.action.dsl;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import groovy.lang.GroovyCodeSource;
import org.junit.Before;
import org.junit.Test;

public class MisspellingTest {
    private Network network;

    @Before
    public void setUp() {
        network = EurostagTutorialExample1Factory.create();
    }

    @Test
    public void test() {
        ActionDb actionDb = new ActionDslLoader(new GroovyCodeSource(getClass().getResource("/testMisspelling.groovy"))).load(network);
        Action someAction = actionDb.getAction("misspelling"); // try to set targettP on GEN
        someAction.run(network, null);
    }
}
