/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.dsl

import com.powsybl.action.dsl.ast.ExpressionNode
import com.powsybl.action.dsl.spi.DslTaskExtension
import com.powsybl.contingency.BranchContingency
import com.powsybl.contingency.BusbarSectionContingency
import com.powsybl.contingency.ContingencyImpl
import com.powsybl.contingency.GeneratorContingency
import com.powsybl.contingency.HvdcLineContingency
import com.powsybl.contingency.tasks.ModificationTask
import com.powsybl.iidm.network.BusbarSection
import com.powsybl.iidm.network.Generator
import com.powsybl.iidm.network.HvdcLine
import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Line
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.network.TwoWindingsTransformer
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.LoggerFactory

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class ActionDslLoader extends DslLoader {

    static LOGGER = LoggerFactory.getLogger(ActionDslLoader.class)

    static class ContingencySpec {

        String[] equipments

        void equipments(String[] equipments) {
            this.equipments = equipments
        }
    }

    static class RuleSpec {

        String description
        ExpressionNode when
        String[] apply
        int life = 1

        void description(String description) {
            this.description = description
        }

        void when(ExpressionNode when) {
            assert when != null
            this.when = when
        }

        void apply(String[] apply) {
            assert apply != null && apply.length > 0
            this.apply = apply
        }

        void life(int life) {
            assert life > 0
            this.life = life
        }
    }

    static class TasksSpec {
    }

    static class ActionSpec {

        String description

        final TasksSpec tasksSpec = new TasksSpec()

        void description(String description) {
            this.description = description
        }

        void tasks(Closure<Void> closure) {
            def cloned = closure.clone()
            cloned.delegate = tasksSpec
            cloned()
        }
    }

    ActionDslLoader(GroovyCodeSource dslSrc) {
        super(dslSrc)
    }

    ActionDslLoader(File dslFile) {
        super(dslFile)
    }

    ActionDslLoader(String script) {
        super(script)
    }

    ActionDb load(Network network) {
        load(network, null)
    }

    ActionDb load(Network network, ActionDslLoaderObserver observer) {
        ActionDb rulesDb = new ActionDb()
        LOGGER.debug("Loading DSL '{}'", dslSrc.getName())
        try {
            observer?.begin(dslSrc.getName())

            Binding binding = new Binding()

            // contingencies
            binding.contingency = { String id, Closure<Void> closure ->
                def cloned = closure.clone()
                ContingencySpec contingencySpec = new ContingencySpec()
                cloned.delegate = contingencySpec
                cloned()
                if (!contingencySpec.equipments) {
                    throw new ActionDslException("'equipments' field is not set")
                }
                if (contingencySpec.equipments.length == 0) {
                    throw new ActionDslException("'equipments' field is empty")
                }
                def elements = []
                for (String equipment : contingencySpec.equipments) {
                    Identifiable identifiable = network.getIdentifiable(equipment)
                    if (identifiable == null) {
                        throw new ActionDslException("Equipment '" + equipment + "' of contingency '" + id + "' not found")
                    }
                    if (identifiable instanceof Line || identifiable instanceof TwoWindingsTransformer) {
                        elements.add(new BranchContingency(equipment))
                    } else if (identifiable instanceof HvdcLine) {
                        elements.add(new HvdcLineContingency(equipment))
                    } else if (identifiable instanceof Generator) {
                        elements.add(new GeneratorContingency(equipment))
                    } else if (identifiable instanceof BusbarSection) {
                        elements.add(new BusbarSectionContingency(equipment))
                    } else {
                        throw new ActionDslException("Equipment type " + identifiable.getClass().name + " not supported in contingencies")
                    }
                }
                LOGGER.debug("Found contingency '{}'", id)
                observer?.contingencyFound(id)
                ContingencyImpl contingency = new ContingencyImpl(id, elements)
                rulesDb.addContingency(contingency)
            }

            ConditionDslLoader.prepareClosures(binding)

            // rules
            binding.rule = { String id, Closure<Void> closure ->
                def cloned = closure.clone()
                RuleSpec ruleSpec = new RuleSpec()
                cloned.delegate = ruleSpec
                cloned()
                if (!ruleSpec.when) {
                    throw new ActionDslException("'when' field is not set")
                }
                if (ruleSpec.apply.length == 0) {
                    throw new ActionDslException("'apply' field is empty")
                }
                Rule rule = new Rule(id, new ExpressionCondition(ruleSpec.when), ruleSpec.life, ruleSpec.apply)
                if (ruleSpec.description) {
                    rule.setDescription(ruleSpec.description)
                }
                rulesDb.addRule(rule)
                LOGGER.debug("Found rule '{}'", id)
                observer?.ruleFound(id)
            }

            // actions
            binding.action = { String id, Closure<Void> closure ->
                def cloned = closure.clone()

                ActionSpec actionSpec = new ActionSpec()

                // fill tasks spec with extensions
                List<ModificationTask> tasks = new ArrayList<>()
                for (DslTaskExtension taskExtension : ServiceLoader.load(DslTaskExtension.class)) {
                    taskExtension.addToSpec(actionSpec.tasksSpec.metaClass, tasks, binding)
                }

                cloned.delegate = actionSpec
                cloned()

                // create action
                Action action = new Action(id, tasks)
                if (actionSpec.description) {
                    action.setDescription(actionSpec.description)
                }
                rulesDb.addAction(action)

                LOGGER.debug("Found action '{}'", id)
                observer?.actionFound(id)
            }

            // set base network
            binding.setVariable("network", network)

            def shell = createShell(binding)

            shell.evaluate(dslSrc)

            observer?.end()
        } catch (CompilationFailedException e) {
            throw new ActionDslException(e.getMessage(), e)
        }
        rulesDb
    }
}
