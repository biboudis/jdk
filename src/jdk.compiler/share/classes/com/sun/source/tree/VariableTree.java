/*
 * Copyright (c) 2005, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.source.tree;

import javax.lang.model.element.Name;

import jdk.internal.javac.PreviewFeature;

/**
 * A tree node for a variable declaration.
 *
 * For example:
 * <pre>
 *   <em>modifiers</em> <em>type</em> <em>name</em> <em>initializer</em> ;
 *   <em>modifiers</em> <em>type</em> <em>qualified-name</em>.this
 * </pre>
 *
 * @jls 8.3 Field Declarations
 * @jls 14.4 Local Variable Declarations
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface VariableTree extends StatementTree {
    /**
     * Returns the modifiers, including any annotations, on the declaration.
     * @return the modifiers
     */
    ModifiersTree getModifiers();

    /**
     * Returns the name of the variable being declared, or an empty name
     * if the variable is unnamed.
     * @return the name
     */
    Name getName();

    /**
     * Returns the qualified identifier for the name being "declared".
     * This is only used in certain cases for the receiver of a
     * method declaration. Returns {@code null} in all other cases.
     * @return the qualified identifier of a receiver declaration
     */
    ExpressionTree getNameExpression();

    /// {@return the type of the variable being declared.}
    ///
    /// @apiNote
    /// The type of the variable can be one of the following:
    /// - if the variable is declared using {@code var}, then the returned value is a {@link VarTypeTree},
    /// - if the variable is a lambda parameter declared without a type (i.e. relying on type inference), then the returned value is {@code null},
    /// - if the declaration uses a record pattern, then the returned value is {@code null},
    /// - otherwise, the variable is declared with an explicit type, and the returned value is that type.
    Tree getType();

    /**
     * Returns the record pattern used by a pattern-based declaration,
     * or {@code null} for a declarator-based declaration.
     * A pattern-based declaration has kind {@link Tree.Kind#ENHANCED_VARIABLE}.
     *
     * @implSpec This implementation returns {@code null}.
     * @return the record pattern, or {@code null}
     * @since 28
     */
    @PreviewFeature(feature=PreviewFeature.Feature.ENHANCED_LOCAL_VARIABLE_DECLARATIONS, reflective=true)
    default PatternTree getPattern() {
        return null;
    }

    /**
     * Returns the initializer for the variable, or {@code null} if none.
     * @return the initializer
     */
    ExpressionTree getInitializer();
}
