/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.json.ContingencyElementSerializer;
import com.powsybl.security.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Objects;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class SecurityAnalysisResultSerializer extends StdSerializer<SecurityAnalysisResult> {

    private static final String VERSION = "1.0";

    SecurityAnalysisResultSerializer() {
        super(SecurityAnalysisResult.class);
    }

    @Override
    public void serialize(SecurityAnalysisResult result, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("version", VERSION);
        jsonGenerator.writeObjectField("preContingencyResult", result.getPreContingencyResult());
        jsonGenerator.writeObjectField("postContingencyResults", result.getPostContingencyResults());
        jsonGenerator.writeEndObject();
    }

    /**
     * @deprecated use write(SecurityAnalysisResult, Writer) instead.
     */
    @Deprecated
    public static void write(SecurityAnalysisResult result, LimitViolationFilter filter, OutputStream outputStream) throws IOException {
        write(result, new OutputStreamWriter(outputStream));
    }

    public static void write(SecurityAnalysisResult result, Writer writer) throws IOException {
        Objects.requireNonNull(result);
        Objects.requireNonNull(writer);

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(SecurityAnalysisResult.class, new SecurityAnalysisResultSerializer());
        module.addSerializer(PostContingencyResult.class, new PostContingencyResultSerializer());
        module.addSerializer(LimitViolationsResult.class, new LimitViolationsResultSerializer());
        module.addSerializer(LimitViolation.class, new LimitViolationSerializer());
        module.addSerializer(ContingencyElement.class, new ContingencyElementSerializer());
        objectMapper.registerModule(module);

        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        objectWriter.writeValue(writer, result);

    }
}
