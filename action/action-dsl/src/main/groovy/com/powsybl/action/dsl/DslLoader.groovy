/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.dsl

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class DslLoader {

    protected final GroovyCodeSource dslSrc

    protected final boolean allowProperties

    DslLoader(GroovyCodeSource dslSrc, boolean allowProperties) {
        this.dslSrc = Objects.requireNonNull(dslSrc)
        this.allowProperties = allowProperties;
    }

    DslLoader(File dslFile, boolean allowProperties) {
        this(new GroovyCodeSource(dslFile), allowProperties)
    }

    DslLoader(String script, boolean allowProperties) {
        this(new GroovyCodeSource(script, "script", GroovyShell.DEFAULT_CODE_BASE), allowProperties)
    }

    DslLoader(GroovyCodeSource dslSrc) {
        this(dslSrc, false);
    }

    DslLoader(File dslFile) {
        this(dslFile, false)
    }

    DslLoader(String script) {
        this(script, false)
    }

    static GroovyShell createShell(Binding binding) {
        def astCustomizer = new ASTTransformationCustomizer(new ActionDslAstTransformation())
        def imports = new ImportCustomizer()
        def config = new CompilerConfiguration()
        config.addCompilationCustomizers(astCustomizer, imports)
        new GroovyShell(binding, config)
    }

}
