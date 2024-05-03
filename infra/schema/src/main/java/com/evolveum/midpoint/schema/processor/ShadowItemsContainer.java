/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

/** Supertype for {@link ShadowAttributesContainer} and {@link ShadowAssociationsContainer}. */
public interface ShadowItemsContainer {

    ShadowItemsContainer clone();
}
