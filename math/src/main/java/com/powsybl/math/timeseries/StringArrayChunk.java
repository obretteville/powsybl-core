/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.math.timeseries;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface StringArrayChunk extends ArrayChunk<StringPoint> {

    /**
     * Fill array with chunk values.
     * @param array the array to fill
     */
    void fillArray(String[] array);
}
