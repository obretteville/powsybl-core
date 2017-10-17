package com.powsybl.afs.storage.timeseries;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class StringPointTest {

    @Test
    public void testGetters() {
        StringPoint point = new StringPoint(0, Instant.parse("2015-01-01T00:00:00Z"), "a");
        assertEquals(0, point.getIndex());
        assertEquals(Instant.parse("2015-01-01T00:00:00Z"), point.getInstant());
        assertEquals("a", point.getValue());
        assertEquals("StringPoint(0, 2015-01-01T00:00:00Z, a)", point.toString());
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(new StringPoint(0, Instant.parse("2015-01-01T00:00:00Z"), "a"),
                                  new StringPoint(0, Instant.parse("2015-01-01T00:00:00Z"), "a"))
                .addEqualityGroup(new StringPoint(1, Instant.parse("2015-01-01T00:15:00Z"), "b"),
                                  new StringPoint(1, Instant.parse("2015-01-01T00:15:00Z"), "b"))
                .testEquals();
    }
}
