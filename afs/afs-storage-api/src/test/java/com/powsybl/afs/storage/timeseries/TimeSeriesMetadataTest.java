/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TimeSeriesMetadataTest {

    @Test
    public void test() throws IOException {
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"),
                                                                     Duration.ofMinutes(15), 1, 1);
        ImmutableMap<String, String> tags = ImmutableMap.of("var1", "value1");
        TimeSeriesMetadata metadata = new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, tags, index);

        // test  getters
        assertEquals("ts1", metadata.getName());
        assertEquals(TimeSeriesDataType.DOUBLE, metadata.getDataType());
        assertEquals(tags, metadata.getTags());
        assertSame(index, metadata.getIndex());

        // test json
        try (StringWriter writer = new StringWriter();
             JsonGenerator generator = new JsonFactory().createGenerator(writer)) {
            metadata.writeJson(generator);
            generator.flush();
            assertEquals("{\"name\":\"ts1\",\"dataType\":\"DOUBLE\",\"tags\":[{\"var1\":\"value1\"}],\"index\":{\"startTime\":1420070400000,\"endTime\":1420074000000,\"spacing\":900000,\"firstVersion\":1,\"versionCount\":1}}",
                         writer.toString());
        }

        // test serializable
        SerializableTester.reserializeAndAssert(metadata);
    }
}
