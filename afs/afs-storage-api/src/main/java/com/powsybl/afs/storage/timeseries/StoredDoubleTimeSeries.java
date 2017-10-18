/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.afs.storage.AfsStorageException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class StoredDoubleTimeSeries implements DoubleTimeSeries {

    private final TimeSeriesMetadata metadata;

    private final List<DoubleArrayChunk> chunks;

    public StoredDoubleTimeSeries(TimeSeriesMetadata metadata, DoubleArrayChunk... chunks) {
        this(metadata, Arrays.asList(chunks));
    }

    public StoredDoubleTimeSeries(TimeSeriesMetadata metadata, List<DoubleArrayChunk> chunks) {
        this.metadata = Objects.requireNonNull(metadata);
        this.chunks = Objects.requireNonNull(chunks);
    }

    public List<DoubleArrayChunk> getChunks() {
        return chunks;
    }

    @Override
    public TimeSeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public Stream<DoublePoint> stream() {
        // sort chunks by offset
        List<DoubleArrayChunk> sortedChunks = chunks.stream()
                .sorted(Comparator.comparing(DoubleArrayChunk::getOffset))
                .collect(Collectors.toList());
        int pointCount = metadata.getIndex().getPointCount();
        int i = 0;
        List<DoubleArrayChunk> repairedChunks = new ArrayList<>(sortedChunks.size());
        for (DoubleArrayChunk chunk : sortedChunks) {
            // check chunk offset is included in index range
            if (chunk.getOffset() > pointCount - 1) {
                throw new AfsStorageException("Chunk offset " + chunk.getOffset() + " is out of index range [" + (pointCount - 1) +
                        ", " + (i + chunk.getLength()) + "]");
            }

            // check chunk overlap
            if (chunk.getOffset() < i) {
                throw new AfsStorageException("Chunk at offset " + chunk.getOffset() + " overlap with previous one");
            }

            // check all values are included in index range
            if (i + chunk.getLength() > pointCount - 1) {
                throw new AfsStorageException("Chunk value at " + (i + chunk.getLength()) + " is out of index range [" +
                        (pointCount - 1) + ", " + (i + chunk.getLength()) + "]");
            }

            // fill with NaN if there is a gap with previous chunk
            if (chunk.getOffset() > i) {
                repairedChunks.add(new CompressedDoubleArrayChunk(i, chunk.getOffset() - i, new double[] {Double.NaN},
                                                                  new int[] {chunk.getOffset() - i}));
                i = chunk.getOffset();
            }
            repairedChunks.add(chunk);

            i += chunk.getLength();
        }
        if (i < pointCount) {
            repairedChunks.add(new CompressedDoubleArrayChunk(i, pointCount - i, new double[] {Double.NaN},
                                                              new int[] {pointCount - i}));
        }

        return repairedChunks.stream().flatMap(chunk -> chunk.stream(metadata.getIndex()));
    }

    @Override
    public double[] toArray() {
        double[] array = new double[metadata.getIndex().getPointCount()];
        Arrays.fill(array, Double.NaN);
        chunks.forEach(chunk -> chunk.fillArray(array));
        return array;
    }

    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("metadata");
        metadata.writeJson(generator);
        generator.writeFieldName("chunks");
        generator.writeStartArray();
        for (DoubleArrayChunk chunk : chunks) {
            chunk.writeJson(generator);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    public void writeJson(BufferedWriter writer) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.useDefaultPrettyPrinter();
            writeJson(generator);
        }
    }

    public void writeJson(Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeJson(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
