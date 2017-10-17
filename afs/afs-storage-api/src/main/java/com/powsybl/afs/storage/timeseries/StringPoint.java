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
public class StringPoint extends AbstractPoint {

    private final String value;

    public StringPoint(int index, Instant instant, String value) {
        super(index, instant);
        this.value = Objects.requireNonNull(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, instant, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringPoint) {
            StringPoint other = (StringPoint) obj;
            return index == other.index && instant.equals(other.instant) && value.equals(other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return "StringPoint(" + index + ", " + instant + ", " + value + ")";
    }
}
