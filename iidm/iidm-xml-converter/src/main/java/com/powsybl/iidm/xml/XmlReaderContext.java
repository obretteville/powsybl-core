/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.xml;

import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class XmlReaderContext extends XmlContext {

    private final XMLStreamReader reader;
    private final List<Runnable> endTasks = new ArrayList<>();

    public XmlReaderContext(Anonymizer anonymizer, XMLStreamReader reader) {
        super(anonymizer);
        this.reader = Objects.requireNonNull(reader);
    }

    public XMLStreamReader getReader() {
        return reader;
    }

    public List<Runnable> getEndTasks() {
        return endTasks;
    }

}
