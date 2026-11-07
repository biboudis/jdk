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
 * @test
 * @bug 8372170
 * @summary Check variable flow through enhanced local declarations and loop headers
 * @enablePreview
 * @compile EnhancedVariableDeclFlow.java
 * @run main EnhancedVariableDeclFlow
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class EnhancedVariableDeclFlow {
    record Box<T>(T value) {}
    record Pair<A, B>(A first, B second) {}

    public static void main(String[] args) throws IOException {
        initializerAssignments(true);
        initializerAssignments(false);
        capturedBindings();
        loopAssignments();
        checkedEvaluation();
    }

    static void initializerAssignments(boolean first) {
        int source;
        Box(var value) = new Box<>(source = 7);
        check(7, source);
        check(7, value);

        final int nestedSource;
        Box(Box(var nested)) = new Box<>(new Box<>(nestedSource = first ? 8 : 9));
        check(nestedSource, nested);

        int conditionalSource;
        Box(var conditional) = first ? new Box<>(conditionalSource = 3)
                                     : new Box<>(conditionalSource = 4);
        check(conditionalSource, conditional);

        int ignoredSource;
        Box(Box(_)) = new Box<>(new Box<>(ignoredSource = 5));
        check(5, ignoredSource);
        Box(Box(var _)) = new Box<>(new Box<>(ignoredSource = 6));
        check(6, ignoredSource);

        int ordinarySource;
        var _ = ordinarySource = 10;
        check(10, ordinarySource);
    }

    static void capturedBindings() {
        Box(var value) = new Box<>("statement");
        Supplier<String> statement = () -> value;
        check("statement", statement.get());

        var captures = new ArrayList<Supplier<String>>();
        List<Pair<Box<String>, Box<Integer>>> rows = List.of(
                new Pair<>(new Box<>("first"), new Box<>(1)),
                new Pair<>(new Box<>("second"), new Box<>(2)));
        for (Pair(Box(var text), Box(var _)) : rows) {
            captures.add(() -> text);
        }
        check("first", captures.get(0).get());
        check("second", captures.get(1).get());
    }

    static void loopAssignments() {
        List<Box<Integer>> values = List.of(new Box<>(1), new Box<>(2));
        int sum = 0;
        for (Box(var value) : values) {
            value++;
            sum += value;
        }
        check(5, sum);

        int count = 0;
        for (Box(_) : values) { count++; }
        for (Box(var _) : values) { count++; }
        for (var _ : values) { count++; }
        check(6, count);

        final int assignedAfterLoop;
        for (Box(_) : values) {}
        assignedAfterLoop = 11;
        check(11, assignedAfterLoop);
    }

    static void checkedEvaluation() throws IOException {
        Box(var value) = checkedBox();
        check(12, value);
        int sum = 0;
        for (Box(var element) : checkedRows()) { sum += element; }
        check(12, sum);
    }

    static Box<Integer> checkedBox() throws IOException {
        return new Box<>(12);
    }

    static Iterable<Box<Integer>> checkedRows() throws IOException {
        return List.of(new Box<>(12));
    }

    static void check(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
