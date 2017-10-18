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
public abstract class AbstractTimeSeries<P extends AbstractPoint, C extends ArrayChunk<P>> {

    protected final TimeSeriesMetadata metadata;

    protected final List<C> chunks;

    public AbstractTimeSeries(TimeSeriesMetadata metadata, C... chunks) {
        this(metadata, Arrays.asList(chunks));
    }

    public AbstractTimeSeries(TimeSeriesMetadata metadata, List<C> chunks) {
        this.metadata = Objects.requireNonNull(metadata);
        this.chunks = Objects.requireNonNull(chunks);
    }

    public List<C> getChunks() {
        return chunks;
    }

    public TimeSeriesMetadata getMetadata() {
        return metadata;
    }

    protected abstract C createGapFillingChunk(int i, int length);

    private List<C> fillGap() {
        // sort chunks by offset
        List<C> sortedChunks = chunks.stream()
                .sorted(Comparator.comparing(C::getOffset))
                .collect(Collectors.toList());
        int pointCount = metadata.getIndex().getPointCount();
        int i = 0;
        List<C> repairedChunks = new ArrayList<>(sortedChunks.size());
        for (C chunk : sortedChunks) {
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
                repairedChunks.add(createGapFillingChunk(i, chunk.getOffset() - i));
                i = chunk.getOffset();
            }
            repairedChunks.add(chunk);

            i += chunk.getLength();
        }
        if (i < pointCount) {
            repairedChunks.add(createGapFillingChunk(i, pointCount - i));
        }
        return repairedChunks;
    }

    public Stream<P> stream() {
        return fillGap().stream().flatMap(chunk -> chunk.stream(metadata.getIndex()));
    }

    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("metadata");
        metadata.writeJson(generator);
        generator.writeFieldName("chunks");
        generator.writeStartArray();
        for (C chunk : chunks) {
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
