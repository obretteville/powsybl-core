/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface TimeSeriesIndex extends Serializable {

    int getPointCount();

    int getFirstVersion();

    int getVersionCount();

    long getTimeAt(int point);

    void writeJson(JsonGenerator generator);

    static TimeSeriesIndex parseJson(JsonParser parser) {
        try {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.START_OBJECT) {
                    String fieldName = parser.nextFieldName();
                    if ("regularIndex".equals(fieldName)) {
                        return RegularTimeSeriesIndex.parseJson(parser);
                    } else {
                        throw new IllegalStateException("Unknown index type " + fieldName);
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
