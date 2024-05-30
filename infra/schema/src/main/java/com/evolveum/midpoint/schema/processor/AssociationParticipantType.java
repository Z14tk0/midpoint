/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What kinds of objects can participate in given association?
 *
 * Some participants are object types, others are object classes. The former is obligatory for legacy simulated associations.
 * Modern simulated associations and native associations can have class-scoped participants.
 */
public class AssociationParticipantType implements Serializable {

    /** Identification of the object type of the participant, if it's type-scoped. Null for class-scoped ones. */
    @Nullable private final ResourceObjectTypeIdentification typeIdentification;

    /**
     * Definition of the object type of the participant. It may be a genuine type even for class-scoped participants,
     * if the object type is a default one for the object class.
     */
    @NotNull final ResourceObjectDefinition objectDefinition;

    private AssociationParticipantType(
            @Nullable ResourceObjectTypeIdentification typeIdentification,
            @NotNull ResourceObjectDefinition objectDefinition) {
        this.typeIdentification = typeIdentification;
        this.objectDefinition = objectDefinition;
    }

    static AssociationParticipantType forObjectClass(@NotNull ResourceObjectDefinition definition) {
        return new AssociationParticipantType(null, definition);
    }

    static AssociationParticipantType forObjectType(@NotNull ResourceObjectTypeDefinition definition) {
        return new AssociationParticipantType(
                definition.getTypeIdentification(),
                definition);
    }

    static Collection<AssociationParticipantType> forObjectTypes(Collection<? extends ResourceObjectTypeDefinition> definitions) {
        return definitions.stream()
                .map(def -> forObjectType(def))
                .toList();
    }

    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return typeIdentification;
    }

    public @NotNull ResourceObjectDefinition getObjectDefinition() {
        return objectDefinition;
    }

    public boolean matches(@NotNull ResourceObjectDefinition definition) {
        var candidateType = definition.getTypeIdentification();
        if (candidateType != null && typeIdentification != null) {
            return candidateType.equals(typeIdentification);
        } else {
            return definition.getObjectClassName().equals(objectDefinition.getObjectClassName());
        }
    }

    @Override
    public String toString() {
        if (typeIdentification != null) {
            return objectDefinition.getObjectClassName().getLocalPart() + " [" + typeIdentification + "]";
        } else {
            return objectDefinition.getObjectClassName().getLocalPart();
        }
    }
}