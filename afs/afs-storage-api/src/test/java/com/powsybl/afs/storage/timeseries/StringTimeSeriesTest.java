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
public class StringTimeSeriesTest {

    @Test
    public void test() throws IOException {
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"),
                                                                     Duration.ofMinutes(15), 1, 1);
        TimeSeriesMetadata metadata = new TimeSeriesMetadata("ts1", TimeSeriesDataType.STRING, Collections.emptyMap(), index);
        UncompressedStringArrayChunk chunk = new UncompressedStringArrayChunk(2, new String[] {"a", "b"});
        StringTimeSeries timeSeries = new StringTimeSeries(metadata, chunk);
        assertSame(metadata, timeSeries.getMetadata());
        assertEquals(Collections.singletonList(chunk), timeSeries.getChunks());
        assertArrayEquals(new String[] {null, null, "a", "b", null}, timeSeries.toArray());
        assertArrayEquals(new StringPoint[] {new StringPoint(0, Instant.parse("2015-01-01T00:00:00Z").toEpochMilli(), null),
                                             new StringPoint(2, Instant.parse("2015-01-01T00:30:00Z").toEpochMilli(), "a"),
                                             new StringPoint(3, Instant.parse("2015-01-01T00:45:00Z").toEpochMilli(), "b"),
                                             new StringPoint(4, Instant.parse("2015-01-01T01:00:00Z").toEpochMilli(), null)},
                          timeSeries.stream().toArray());
        String jsonRef = String.join(System.lineSeparator(),
                "{",
                "  \"metadata\" : {",
                "    \"name\" : \"ts1\",",
                "    \"dataType\" : \"STRING\",",
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
                "    \"values\" : [ \"a\", \"b\" ]",
                "  } ]",
                "}");
        assertEquals(jsonRef, JsonUtil.toJson(timeSeries::writeJson));
    }
}
