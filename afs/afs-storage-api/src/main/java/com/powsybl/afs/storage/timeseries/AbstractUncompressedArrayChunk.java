/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractUncompressedArrayChunk {

    protected final int offset;

    public AbstractUncompressedArrayChunk(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isCompressed() {
        return false;
    }

    public double getCompressionFactor() {
        return 1d;
    }

    protected abstract void writeValuesJson(JsonGenerator generator) throws IOException;

    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField("offset", offset);
        generator.writeFieldName("values");
        writeValuesJson(generator);
        generator.writeEndObject();
    }
}