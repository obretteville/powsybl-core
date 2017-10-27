/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

rule ('test') {
    when !contingencyOccurred()
    life 1
    apply 'misspelling'
}

action ('misspelling') {
    tasks {
        script {
            generator('GEN').targetP = 42
            generator('GEN').targettP = 24
        }
    }
}