package com.powsybl.afs.storage.timeseries;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class DoublePointTest {

    @Test
    public void testGetters() {
        DoublePoint point = new DoublePoint(0, Instant.parse("2015-01-01T00:00:00Z"), 10d);
        assertEquals(0, point.getIndex());
        assertEquals(Instant.parse("2015-01-01T00:00:00Z"), point.getInstant());
        assertEquals(10d, point.getValue(), 0d);
        assertEquals("DoublePoint(0, 2015-01-01T00:00:00Z, 10.0)", point.toString());
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(new DoublePoint(0, Instant.parse("2015-01-01T00:00:00Z"), 10d),
                        new DoublePoint(0, Instant.parse("2015-01-01T00:00:00Z"), 10d))
                .addEqualityGroup(new DoublePoint(1, Instant.parse("2015-01-01T00:15:00Z"), 8d),
                        new DoublePoint(1, Instant.parse("2015-01-01T00:15:00Z"), 8d))
                .testEquals();
    }
}
