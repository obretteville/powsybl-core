/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CompressedStringArrayChunk extends AbstractCompressedArrayChunk implements StringArrayChunk {

    private final String[] stepValues;

    private int estimatedSize;

    private int uncompressedEstimatedSize;

    public CompressedStringArrayChunk(int offset, int uncompressedLength, String[] stepValues, int[] stepLengths) {
        super(offset, uncompressedLength, stepLengths);
        CompressedDoubleArrayChunk.check(offset, uncompressedLength, stepValues.length, stepLengths.length);
        this.stepValues = Objects.requireNonNull(stepValues);
        updateEstimatedSize();
    }

    private void updateEstimatedSize() {
        estimatedSize = 0;
        uncompressedEstimatedSize = 0;
        for (int i = 0; i < stepValues.length; i++) {
            String stepValue = stepValues[i];
            if (stepValue != null) {
                int stepLength = stepLengths[i];
                estimatedSize += stepValue.length() * Character.BYTES + Integer.BYTES;
                uncompressedEstimatedSize += stepValue.length() * Character.BYTES * stepLength;
            }
        }
    }

    public String[] getStepValues() {
        return stepValues;
    }

    @Override
    public int getEstimatedSize() {
        return estimatedSize;
    }

    @Override
    public int getUncompressedEstimatedSize() {
        return uncompressedEstimatedSize;
    }

    @Override
    public void fillArray(String[] array) {
        Objects.requireNonNull(array);
        int k = 0;
        for (int i = 0; i < stepValues.length; i++) {
            String value = stepValues[i];
            for (int j = 0; j < stepLengths[i]; j++) {
                array[offset + k++] = value;
            }
        }
    }

    @Override
    public Stream<StringPoint> stream(TimeSeriesIndex index) {
        Objects.requireNonNull(index);
        Iterator<StringPoint> iterator = new Iterator<StringPoint>() {

            private int i = offset;
            private int step = 0;

            @Override
            public boolean hasNext() {
                return i < offset + uncompressedLength;
            }

            @Override
            public StringPoint next() {
                StringPoint point = new StringPoint(i, index.getTimeAt(i), stepValues[step]);
                i += stepLengths[step];
                step++;
                return point;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }

    @Override
    protected void writeStepValuesJson(JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (String stepValue : stepValues) {
            generator.writeString(stepValue);
        }
        generator.writeEndArray();
    }
}
