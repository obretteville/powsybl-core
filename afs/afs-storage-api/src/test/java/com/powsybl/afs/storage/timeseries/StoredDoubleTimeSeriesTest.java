/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.powsybl.commons.json.JsonUtil;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class StoredDoubleTimeSeriesTest {

    @Test
    public void test() throws IOException {
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"),
                                                                     Duration.ofMinutes(15), 1, 1);
        TimeSeriesMetadata metadata = new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, Collections.emptyMap(), index);
        UncompressedDoubleArrayChunk chunk = new UncompressedDoubleArrayChunk(2, new double[] {1d, 2d});
        StoredDoubleTimeSeries timeSeries = new StoredDoubleTimeSeries(metadata, chunk);
        assertSame(metadata, timeSeries.getMetadata());
        assertEquals(Collections.singletonList(chunk), timeSeries.getChunks());
        assertArrayEquals(new double[] {Double.NaN, Double.NaN, 1d, 2d, Double.NaN}, timeSeries.toArray(), 0d);
        assertArrayEquals(new DoublePoint[] {new DoublePoint(0, Instant.parse("2015-01-01T00:00:00Z").toEpochMilli(), Double.NaN),
                                             new DoublePoint(2, Instant.parse("2015-01-01T00:30:00Z").toEpochMilli(), 1d),
                                             new DoublePoint(3, Instant.parse("2015-01-01T00:45:00Z").toEpochMilli(), 2d),
                                             new DoublePoint(4, Instant.parse("2015-01-01T01:00:00Z").toEpochMilli(), Double.NaN)},
                          timeSeries.stream().toArray());
        String jsonRef = String.join(System.lineSeparator(),
                "{",
                "  \"metadata\" : {",
                "    \"name\" : \"ts1\",",
                "    \"dataType\" : \"DOUBLE\",",
                "    \"tags\" : [ ],",
                "    \"regularIndex\" : {",
                "      \"startTime\" : 1420070400000,",
                "      \"endTime\" : 1420074000000,",
                "      \"spacing\" : 900000,",
                "      \"firstVersion\" : 1,",
                "      \"versionCount\" : 1",
                "    }",
                "  },",
                "  \"chunks\" : [ {",
                "    \"offset\" : 2,",
                "    \"values\" : [ 1.0, 2.0 ]",
                "  } ]",
                "}");
        assertEquals(jsonRef, JsonUtil.toJson(timeSeries::writeJson));
    }
}
