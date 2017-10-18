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
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface ArrayChunk<P extends AbstractPoint> extends Serializable {

    /**
     * Get array chunk offset.
     * @return array chunk offset
     */
    int getOffset();

    /**
     * Get array chunk length
     * @return array chunk length
     */
    int getLength();

    /**
     * Get estimated size in bytes.
     * @return estimated size in bytes
     */
    int getEstimatedSize();

    /**
     * Get compression factor. 1 means no compression.
     * @return the compression factor
     */
    double getCompressionFactor();

    /**
     * Check if chunk is in compressed form.
     * @return true if chunk is in compressed form, false otherwise
     */
    boolean isCompressed();

    /**
     * Get a point stream.
     * @param index the time series index
     * @return a point stream
     */
    Stream<P> stream(TimeSeriesIndex index);

    /**
     * Serialize this array chunk to json.
     * @param generator a json generator (jackson)
     * @throws IOException in case of json writing error
     */
    void writeJson(JsonGenerator generator) throws IOException;
}