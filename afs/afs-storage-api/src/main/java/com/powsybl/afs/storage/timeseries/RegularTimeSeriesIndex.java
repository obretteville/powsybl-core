/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonGenerator;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class RegularTimeSeriesIndex implements TimeSeriesIndex {

    private static final long serialVersionUID = 7997935378544325662L;

    private final long startTime; // in ms from epoch

    private final long endTime; // in ms from epoch

    private final long spacing; // in ms

    private final int firstVersion;

    private final int versionCount;

    protected RegularTimeSeriesIndex(long startTime, long endTime, long spacing, int firstVersion, int versionCount) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.spacing = spacing;
        if (spacing > endTime - startTime) {
            throw new IllegalArgumentException("Spacing " + spacing + " is longer than interval " + (endTime - startTime));
        }
        this.firstVersion = firstVersion;
        this.versionCount = versionCount;
    }

    public static RegularTimeSeriesIndex create(Interval interval, Duration spacing, int firstVersion, int versionCount) {
        return new RegularTimeSeriesIndex(interval.getStart().toEpochMilli(), interval.getEnd().toEpochMilli(),
                                          spacing.toMillis(), firstVersion, versionCount);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getSpacing() {
        return spacing;
    }

    public Interval getInterval() {
        return Interval.of(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime));
    }

    public Duration getSpacingDuration() {
        return Duration.ofMillis(spacing);
    }

    @Override
    public int getFirstVersion() {
        return firstVersion;
    }

    @Override
    public int getVersionCount() {
        return versionCount;
    }

    @Override
    public int getPointCount() {
        return Math.round(((float) (endTime - startTime)) / spacing) + 1;
    }

    @Override
    public long getEpochMsAt(int point) {
        return startTime + point * spacing;
    }

    @Override
    public Instant getInstantAt(int point) {
        return Instant.ofEpochMilli(startTime).plus(getSpacingDuration().multipliedBy(point));
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, spacing, firstVersion, versionCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TimeSeriesIndex) {
            RegularTimeSeriesIndex otherIndex = (RegularTimeSeriesIndex) obj;
            return startTime == otherIndex.startTime &&
                    endTime == otherIndex.endTime &&
                    spacing == otherIndex.spacing &&
                    firstVersion == otherIndex.firstVersion &&
                    versionCount == otherIndex.versionCount;
        }
        return false;
    }

    @Override
    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField("startTime", startTime);
        generator.writeNumberField("endTime", endTime);
        generator.writeNumberField("spacing", spacing);
        generator.writeNumberField("firstVersion", firstVersion);
        generator.writeNumberField("versionCount", versionCount);
        generator.writeEndObject();
    }

    @Override
    public String toString() {
        return "index(startTime=" + startTime + ", endTime=" + endTime + ", spacing=" + spacing +
                ", firstVersion=" + firstVersion + ", versionCount=" + versionCount + ")";
    }
}
