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
public abstract class AbstractPoint {

    protected final int index;

    protected final Instant instant;

    protected AbstractPoint(int index, Instant instant) {
        this.index = index;
        this.instant = Objects.requireNonNull(instant);
    }

    public int getIndex() {
        return index;
    }

    public Instant getInstant() {
        return instant;
    }
}
