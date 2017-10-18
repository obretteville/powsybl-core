/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import com.powsybl.afs.storage.AfsStorageException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface DoubleTimeSeries extends TimeSeries<DoublePoint> {

    double[] toArray();

    static Stream<DoublePoint> stream(List<DoubleTimeSeries> timeSeriesList) {
        Objects.requireNonNull(timeSeriesList);
        if (timeSeriesList.isEmpty()) {
            return Stream.empty();
        }
        long indexCount = timeSeriesList.stream().map(DoubleTimeSeries::getMetadata)
                                                 .map(TimeSeriesMetadata::getIndex)
                                                 .distinct()
                                                 .count();
        if (indexCount > 1) {
            throw new AfsStorageException("Time series must have the same index");
        }
        throw new UnsupportedOperationException("TODO"); // TODO
    }
}
