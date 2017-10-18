/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class RegularTimeSeriesIndexTest {

    @Test
    public void test() throws IOException {
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"),
                                                                     Duration.ofMinutes(15), 1, 1);
        assertEquals("2015-01-01T00:00:00Z", Instant.ofEpochMilli(index.getStartTime()).toString());
        assertEquals("2015-01-01T01:00:00Z", Instant.ofEpochMilli(index.getEndTime()).toString());
        assertEquals(15 * 60 * 1000, index.getSpacing());
        assertEquals(1, index.getFirstVersion());
        assertEquals(1, index.getVersionCount());
        assertEquals(5, index.getPointCount());
        assertEquals(Instant.ofEpochMilli(index.getStartTime() + 15 * 60 * 1000).toEpochMilli(), index.getTimeAt(1));
        assertEquals("index(startTime=2015-01-01T00:00:00Z, endTime=2015-01-01T01:00:00Z, spacing=PT15M, firstVersion=1, versionCount=1)",
                     index.toString());
        try (StringWriter writer = new StringWriter();
             JsonGenerator generator = new JsonFactory().createGenerator(writer)) {
            index.writeJson(generator);
            generator.flush();
            assertEquals("{\"startTime\":1420070400000,\"endTime\":1420074000000,\"spacing\":900000,\"firstVersion\":1,\"versionCount\":1}",
                         writer.toString());
        }
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"), Duration.ofMinutes(15), 1, 1),
                                  RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"), Duration.ofMinutes(15), 1, 1))
                .addEqualityGroup(RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:15:00Z"), Duration.ofMinutes(30), 2, 1),
                                  RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:15:00Z"), Duration.ofMinutes(30), 2, 1))
                .testEquals();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContructorError() {
        RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T00:10:00Z"),
                                      Duration.ofMinutes(15), 1, 1);
    }
}