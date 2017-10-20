/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TimeSeriesMetadata implements Serializable {

    private static final long serialVersionUID = -4424175272094933109L;

    private final String name;

    private final TimeSeriesDataType dataType;

    private final Map<String, String> tags;

    private final TimeSeriesIndex index;

    public TimeSeriesMetadata(String name, TimeSeriesDataType dataType, Map<String, String> tags, TimeSeriesIndex index) {
        this.name = Objects.requireNonNull(name);
        this.dataType = Objects.requireNonNull(dataType);
        this.tags = Collections.unmodifiableMap(Objects.requireNonNull(tags));
        this.index = Objects.requireNonNull(index);
    }

    public String getName() {
        return name;
    }

    public TimeSeriesDataType getDataType() {
        return dataType;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public TimeSeriesIndex getIndex() {
        return index;
    }

    public void writeJson(JsonGenerator generator) {
        try {
            generator.writeFieldName("metadata");
            generator.writeStartObject();
            generator.writeStringField("name", name);
            generator.writeStringField("dataType", dataType.name());
            generator.writeFieldName("tags");
            generator.writeStartArray();
            for (Map.Entry<String, String> e : tags.entrySet()) {
                generator.writeStartObject();
                generator.writeStringField(e.getKey(), e.getValue());
                generator.writeEndObject();
            }
            generator.writeEndArray();
            index.writeJson(generator);
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dataType, tags, index);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TimeSeriesMetadata) {
            TimeSeriesMetadata other = (TimeSeriesMetadata) obj;
            return name.equals(other.name) &&
                    dataType == other.dataType &&
                    tags.equals(other.tags) &&
                    index.equals(other.index);
        }
        return false;
    }
}
