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
 * @bug 8372170
 * @summary Check definite assignment and capture errors in enhanced local declarations
 * @enablePreview
 * @compile/fail/ref=EnhancedVariableDeclFlowErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW EnhancedVariableDeclFlowErrors.java
 */

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class EnhancedVariableDeclFlowErrors {
    record Box<T>(T value) {}

    void unassignedInitializer() {
        int source;
        Box(var value) = new Box<>(source);
    }

    void conditionallyAssignedInitializer(boolean first) {
        int source;
        Box(var value) = first ? new Box<>(source = 1) : new Box<>(0);
        int use = source;
    }

    void possiblyEmptyLoop(List<Box<Integer>> rows) {
        int assigned;
        for (Box(var value) : rows) { assigned = value; }
        int use = assigned;
    }

    void finalAssignmentInLoop(List<Box<Integer>> rows) {
        final int assigned;
        for (Box(var value) : rows) { assigned = value; }
    }

    void mutatedStatementBinding() {
        Box(var value) = new Box<>(1);
        Supplier<Integer> capture = () -> value;
        value++;
    }

    void mutatedLoopBinding(List<Box<Integer>> rows) {
        for (Box(var value) : rows) {
            Supplier<Integer> capture = () -> value;
            value++;
        }
    }

    void uncaughtStatementException() {
        Box(var value) = checkedBox();
    }

    void uncaughtLoopException() {
        for (Box(var value) : checkedRows()) {}
    }

    static Box<Integer> checkedBox() throws IOException {
        return new Box<>(1);
    }

    static Iterable<Box<Integer>> checkedRows() throws IOException {
        return List.of(new Box<>(1));
    }
}
