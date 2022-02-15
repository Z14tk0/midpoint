/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.PreInboundsProcessing;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Evaluates "pre-mappings" i.e. inbound mappings that are evaluated before the actual clockwork is run.
 * (This is currently done to simplify the correlation process.)
 *
 * FIXME only a fake implementation for now
 */
class PreMappingsEvaluation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(PreMappingsEvaluation.class);

    @NotNull private final SynchronizationContext<F> syncCtx;
    @NotNull private final F preFocus;
    @NotNull private final ModelBeans beans;

    PreMappingsEvaluation(@NotNull SynchronizationContext<F> syncCtx, @NotNull ModelBeans beans) throws SchemaException {
        this.syncCtx = syncCtx;
        this.preFocus = syncCtx.getPreFocus();
        this.beans = beans;
    }

    /**
     * We simply copy matching attributes from the resource object to the focus.
     */
    public void evaluate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        MappingEvaluationEnvironment env =
                new MappingEvaluationEnvironment(
                        "pre-inbounds", beans.clock.currentTimeXMLGregorianCalendar(), syncCtx.getTask());
        new PreInboundsProcessing<>(syncCtx, beans, env, result)
                .collectAndEvaluateMappings();

        LOGGER.info("Pre-focus:\n{}", preFocus.debugDumpLazily(1));
    }
}
