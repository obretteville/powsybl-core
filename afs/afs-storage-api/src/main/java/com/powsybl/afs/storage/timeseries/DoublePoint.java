/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.timeseries;

import java.time.Instant;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DoublePoint extends AbstractPoint {

    private final double value;

    public DoublePoint(int index, Instant instant, double value) {
        super(index, instant);
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, instant, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DoublePoint) {
            DoublePoint other = (DoublePoint) obj;
            return index == other.index && instant.equals(other.instant) && value == other.value;
        }
        return false;
    }

    @Override
    public String toString() {
        return "DoublePoint(" + index + ", " + instant + ", " + value + ")";
    }
}
