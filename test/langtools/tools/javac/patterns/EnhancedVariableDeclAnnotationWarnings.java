/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test /nodynamiccopyright/
 * @summary Check ELVD annotation warnings and suppression boundaries against ordinary locals
 * @enablePreview
 * @compile/ref=EnhancedVariableDeclAnnotationWarnings.out -XDrawDiagnostics -Xlint:unchecked,rawtypes,deprecation,serial EnhancedVariableDeclAnnotationWarnings.java
 */

import java.util.List;

public class EnhancedVariableDeclAnnotationWarnings {
    record Box<T>(T value) {}

    void uncheckedSuppressionIsConfinedToEachDeclaration(Object obj) {
        {
            Box<String>(var value) = (Box<String>) obj;
            Box<String> other = (Box<String>) obj;
        }
        {
            Box<String>(var value) = (Box<String>) obj;
            @SuppressWarnings("unchecked") Box<String> other = (Box<String>) obj;
        }
        {
            @SuppressWarnings("unchecked") Box<String>(var value) = (Box<String>) obj;
            Box<String> other = (Box<String>) obj;
        }
        {
            @SuppressWarnings("unchecked") Box<String>(var value) = (Box<String>) obj;
            @SuppressWarnings("unchecked") Box<String> other = (Box<String>) obj;
        }
        {
            Box<String>(var value) = (Box<String>) obj;
            Box<String> _ = (Box<String>) obj;
        }
        {
            Box<String>(var value) = (Box<String>) obj;
            @SuppressWarnings("unchecked") Box<String> _ = (Box<String>) obj;
        }
        {
            @SuppressWarnings("unchecked") Box<String>(var value) = (Box<String>) obj;
            Box<String> _ = (Box<String>) obj;
        }
        {
            @SuppressWarnings("unchecked") Box<String>(var value) = (Box<String>) obj;
            @SuppressWarnings("unchecked") Box<String> _ = (Box<String>) obj;
        }
    }

    void deprecatedAnnotationsOnLocalDeclarationsProduceWarnings(List<Box<String>> rows) {
        @Deprecated Box<String> named = new Box<>("named");
        @Deprecated Box<String> _ = new Box<>("unnamed");
        @Deprecated Box<String>(var value) = new Box<>("enhanced");
        for (@Deprecated Box<String> row : rows) {}
        for (@Deprecated Box<String> _ : rows) {}
        for (@Deprecated Box<String>(var item) : rows) {}
    }

    void declarationSuppressionHidesIneffectiveDeprecatedWarnings(List<Box<String>> rows) {
        @SuppressWarnings("deprecation") @Deprecated Box<String> named = new Box<>("named");
        @SuppressWarnings("deprecation") @Deprecated Box<String> _ = new Box<>("unnamed");
        @SuppressWarnings("deprecation") @Deprecated Box<String>(var value) = new Box<>("enhanced");
        for (@SuppressWarnings("deprecation") @Deprecated Box<String> row : rows) {}
        for (@SuppressWarnings("deprecation") @Deprecated Box<String> _ : rows) {}
        for (@SuppressWarnings("deprecation") @Deprecated Box<String>(var item) : rows) {}
    }

    @SuppressWarnings("deprecation")
    void methodSuppressionHidesIneffectiveDeprecatedWarnings(List<Box<String>> rows) {
        @Deprecated Box<String> named = new Box<>("named");
        @Deprecated Box<String> _ = new Box<>("unnamed");
        @Deprecated Box<String>(var value) = new Box<>("enhanced");
        for (@Deprecated Box<String> row : rows) {}
        for (@Deprecated Box<String> _ : rows) {}
        for (@Deprecated Box<String>(var item) : rows) {}
    }

    void loopHeaderSuppressionDoesNotCoverBodyOrFollowingStatements(List<Box<String>> rows, Box<String>[] array,
            List<Box<Box<String>>> nestedRows, Box<Box<String>>[] nestedArray, Object obj) {
        for (@SuppressWarnings("unchecked") Box<String> box : rows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<String> box : array) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<String>(var value) : rows) {
            List<String> inBody = (List<String>) obj;
        }
        List<String> afterExplicit = (List<String>) obj;
        for (@SuppressWarnings("unchecked") Box<String>(var value) : array) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box(var value) : rows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box(var value) : array) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<String>(_) : rows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<String>(_) : array) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box(var _) : rows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box(var _) : array) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<Box<String>>(Box(var value)) : nestedRows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<Box<String>>(Box(var value)) : nestedArray) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<Box<String>>(Box(_)) : nestedRows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("unchecked") Box<Box<String>>(Box(_)) : nestedArray) {
            List<String> inBody = (List<String>) obj;
        }
        List<String> afterNested = (List<String>) obj;
    }

    void loopHeaderSuppressionDoesNotCoverIterableOrArrayExpression(Object obj) {
        for (@SuppressWarnings("unchecked") Box<String> box : (List<Box<String>>) obj) {}
        for (@SuppressWarnings("unchecked") Box<String> box : (Box<String>[]) obj) {}
        for (@SuppressWarnings("unchecked") Box<String>(var value) : (List<Box<String>>) obj) {}
        for (@SuppressWarnings("unchecked") Box<String>(var value) : (Box<String>[]) obj) {}
        for (@SuppressWarnings("unchecked") Box(var value) : (List<Box<String>>) obj) {}
        for (@SuppressWarnings("unchecked") Box(var value) : (Box<String>[]) obj) {}
        for (@SuppressWarnings("unchecked") Box<Box<String>>(Box(var value)) : (List<Box<Box<String>>>) obj) {}
        for (@SuppressWarnings("unchecked") Box<Box<String>>(Box(var value)) : (Box<Box<String>>[]) obj) {}
    }

    void loopHeaderSuppressionDoesNotCoverLocalClassSerialWarnings(List<Box<String>> rows, Box<String>[] array,
            List<Box<Box<String>>> nestedRows) {
        for (@SuppressWarnings("serial") Box<String> box : rows) {
            class Local implements java.io.Serializable {}
        }
        for (@SuppressWarnings("serial") Box<String>(var value) : rows) {
            class Local implements java.io.Serializable {}
        }
        for (@SuppressWarnings("serial") Box(var value) : array) {
            class Local implements java.io.Serializable {}
        }
        for (@SuppressWarnings("serial") Box<Box<String>>(Box(var value)) : nestedRows) {
            class Local implements java.io.Serializable {}
        }
    }

    @SuppressWarnings("unchecked")
    void methodSuppressionRemainsEffectiveInLoopBodies(List<Box<String>> rows, Box<String>[] array,
            List<Box<Box<String>>> nestedRows, Object obj) {
        for (@SuppressWarnings("rawtypes") Box<String> box : rows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("rawtypes") Box<String>(var value) : rows) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("rawtypes") Box(var value) : array) {
            List<String> inBody = (List<String>) obj;
        }
        for (@SuppressWarnings("rawtypes") Box<Box<String>>(Box(var value)) : nestedRows) {
            List<String> inBody = (List<String>) obj;
        }
    }

    void rawTypeWarningsInLoopHeadersRequireMatchingSuppression(@SuppressWarnings("rawtypes") List<Box<List>> rows,
                    @SuppressWarnings("rawtypes") Box<List>[] array) {
        for (Box<List> box : rows) {}
        for (@SuppressWarnings("rawtypes") Box<List> box : rows) {}
        for (@SuppressWarnings("unchecked") Box<List> box : rows) {}
        for (Box<List> box : array) {}
        for (@SuppressWarnings("rawtypes") Box<List> box : array) {}
        for (@SuppressWarnings("unchecked") Box<List> box : array) {}
        for (Box<List>(var value) : rows) {}
        for (@SuppressWarnings("rawtypes") Box<List>(var value) : rows) {}
        for (@SuppressWarnings("unchecked") Box<List>(var value) : rows) {}
        for (Box<List>(var value) : array) {}
        for (@SuppressWarnings("rawtypes") Box<List>(var value) : array) {}
        for (@SuppressWarnings("unchecked") Box<List>(var value) : array) {}
        for (Box<List>(_) : rows) {}
        for (@SuppressWarnings("rawtypes") Box<List>(_) : rows) {}
        for (@SuppressWarnings("unchecked") Box<List>(_) : rows) {}
        for (Box<List>(_) : array) {}
        for (@SuppressWarnings("rawtypes") Box<List>(_) : array) {}
        for (@SuppressWarnings("unchecked") Box<List>(_) : array) {}
        for (Box<List>(List _) : rows) {}
        for (@SuppressWarnings("rawtypes") Box<List>(List _) : rows) {}
        for (@SuppressWarnings("unchecked") Box<List>(List _) : rows) {}
        for (Box<List>(List _) : array) {}
        for (@SuppressWarnings("rawtypes") Box<List>(List _) : array) {}
        for (@SuppressWarnings("unchecked") Box<List>(List _) : array) {}
    }
}
