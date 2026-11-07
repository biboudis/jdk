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
 * @summary Check ELVD initializer inference, assignment compatibility and pattern coverage
 * @enablePreview
 * @compile EnhancedVariableDeclInferenceTest.java
 */

import java.util.List;
import java.util.function.Function;

class EnhancedVariableDeclInferenceTest {
    record Box<T>(T value) {}
    record Pair<A, B>(A first, B second) {}
    interface Marker {}
    sealed interface IPoint permits Point {}
    record Point(int x, int y) implements Marker, IPoint {}
    record Numeric<N extends Number>(N value) {}
    record Token<T>(T value) {}
    sealed interface Carrier<T> permits Rec {}
    record Rec<T>(T value) implements Carrier<T>, Marker {}
    sealed interface S permits R {}
    record R<T>(T value) implements S {}

    static <T> Box<T> box(T value) { return new Box<>(value); }
    static <T> Box<T> empty() { return new Box<>(null); }
    static <T> Box<Box<T>> nestedEmpty() { return new Box<>(empty()); }
    static <T extends Number> Numeric<T> numeric() { return null; }
    static <T> T read() { return null; }
    static <T> T read(Token<T> token) { return token.value(); }
    static <T> T id(T value) { return value; }
    static <A, B> Pair<A, B> pair(A a, B b) { return new Pair<>(a, b); }
    static <T> List<Box<T>> rows() { return List.of(); }
    static <T> List<Box<Box<T>>> nestedRows() { return List.of(); }

    void outerTargets() {
        Box(var fromArgument) = box("hi");
        String a = fromArgument;
        Box(var standalone) = empty();
        Object b = standalone;
        Box<String>(var targeted) = empty();
        String c = targeted;
        Box<?>(var wildcard) = read();
        Object d = wildcard;
        Box<String>(var unconstrained) = read();
        String e = unconstrained;
        Point(var x, var y) = read();
        int sum = x + y;
        Box(_) = empty();
        Box(var _) = empty();
        Box<String>(_) = read();
        Box<String>(var _) = empty();
        Box<String>(String _) = empty();
    }

    <T> void genericOuterTarget() {
        Box<T>(var value) = empty();
        T result = value;
    }

    void tokenAndWildcardPrecision(Token<Box<String>> token, Box<? extends CharSequence> bounded) {
        Box(var precise) = read(token);
        String a = precise;
        Box<?>(var erasedByPattern) = read(token);
        Object b = erasedByPattern;
        Box(var upperBound) = bounded;
        int length = upperBound.length();
        Box<? extends CharSequence>(var targetedBound) = read();
        int otherLength = targetedBound.length();
    }

    void partialArguments() {
        Pair(var text, var unknown) = pair("hi", null);
        String a = text;
        Object b = unknown;
        Pair<String, Integer>(var targetedText, var targetedNumber) = pair("hi", null);
        String c = targetedText;
        Integer d = targetedNumber;
        Pair<String, Integer>(String typedText, Integer typedNumber) = pair("hi", null);
        Pair(_, _) = pair("hi", null);
    }

    void declaredBounds() {
        Numeric(var inferredBound) = numeric();
        Number a = inferredBound;
        Numeric<Integer>(var explicitBound) = numeric();
        Integer b = explicitBound;
    }

    void polyExpressionTargets(boolean flag, int selector) {
        Box<String>(var direct) = empty();
        String a = direct;
        Box<String>(var parens) = (empty());
        String b = parens;
        Box<String>(var nestedCall) = id(empty());
        String c = nestedCall;
        Box<String>(var conditional) = flag ? empty() : empty();
        String d = conditional;
        Box<String>(var switched) = switch (selector) {
            case 0 -> empty();
            default -> empty();
        };
        String e = switched;
        Box<String>(var diamond) = new Box<>(null);
        String f = diamond;
        Box<Function<String, String>>(var lambda) = new Box<>(s -> s.trim());
        String g = lambda.apply("hi");
        Box<Function<String, String>>(var reference) = new Box<>(String::trim);
        String h = reference.apply("hi");
        Box(var bareConditional) = flag ? box("a") : box("b");
        String i = bareConditional;
        Box(var bareSwitch) = switch (selector) {
            case 0 -> box("a");
            default -> box("b");
        };
        String j = bareSwitch;
    }

    void nestedPatternLeaves() {
        Box(Box(var fromArgument)) = box(box("hi"));
        String fromArgumentResult = fromArgument;
        Box<Box<String>>(Box(var fromFactory)) = nestedEmpty();
        String fromFactoryResult = fromFactory;
        Box<Box<String>>(Box(var fromRead)) = read();
        String fromReadResult = fromRead;
        Box(Box(String typedFromArgument)) = box(box("hi"));
        String typedFromArgumentResult = typedFromArgument;
        Box<Box<String>>(Box(String typedFromFactory)) = nestedEmpty();
        String typedFromFactoryResult = typedFromFactory;
        Box<Box<String>>(Box(String typedFromRead)) = read();
        String typedFromReadResult = typedFromRead;
        Box(Box(_)) = box(box("hi"));
        Box<Box<String>>(Box(_)) = nestedEmpty();
        Box<Box<String>>(Box(_)) = read();
        Box(Box(var _)) = box(box("hi"));
        Box<Box<String>>(Box(var _)) = nestedEmpty();
        Box<Box<String>>(Box(var _)) = read();
        Box(Box(String _)) = box(box("hi"));
        Box<Box<String>>(Box(String _)) = nestedEmpty();
        Box<Box<String>>(Box(String _)) = read();
    }

    void nestedPatternContexts() {
        Box(Box(Box(var deep))) = box(box(box("hi")));
        String a = deep;
        Pair(Box(var left), Box(_)) = pair(box("hi"), box(1));
        String b = left;
        Pair(Box(_), Box(var right)) = pair(box("hi"), box(1));
        Integer c = right;
        Pair(_, Box(var kept)) = pair(box("hi"), box(1));
        Integer d = kept;
        Box(Box(var standalone)) = nestedEmpty();
        Object e = standalone;
        Box(Box(_)) = nestedEmpty();
        Box<Box<? extends CharSequence>>(Box(var bounded)) = read();
        int length = bounded.length();
    }

    void enhancedForInference(List<Box<String>> boxes, Box<String>[] array) {
        for (Box(var x) : boxes) { String s = x; }
        for (Box<String>(var x) : boxes) { String s = x; }
        for (Box<?>(var x) : boxes) { Object o = x; }
        for (Box(_) : boxes) {}
        for (Box(var _) : array) {}
        for (Box(String _) : array) {}
        for (Box(var x) : array) { String s = x; }
        for (Box(var x) : rows()) { Object o = x; }
    }

    void nestedEnhancedForLeaves(List<Box<Box<String>>> rows, Box<Box<String>>[] array) {
        for (Box(Box(var x)) : rows) { String result = x; }
        for (Box<Box<String>>(Box(var x)) : array) { String result = x; }
        for (Box(Box(String x)) : rows) { String result = x; }
        for (Box<Box<String>>(Box(String x)) : array) { String result = x; }
        for (Box(Box(_)) : rows) {}
        for (Box<Box<String>>(Box(_)) : array) {}
        for (Box(Box(var _)) : rows) {}
        for (Box<Box<String>>(Box(var _)) : array) {}
        for (Box(Box(String _)) : rows) {}
        for (Box<Box<String>>(Box(String _)) : array) {}
    }

    void nestedEnhancedForContexts(List<Pair<Box<String>, Box<Integer>>> pairs) {
        for (Pair(Box(var text), Box(_)) : pairs) { String s = text; }
        for (Pair(_, Box(var number)) : pairs) { Integer n = number; }
        for (Box(Box(var x)) : nestedRows()) { Object o = x; }
        for (Box(Box(_)) : nestedRows()) {}
    }

    void testingContextsRemainSelectorFirst(Token<Box<String>> token) {
        if (read(token) instanceof Box(var x)) { String s = x; }
        switch (read(token)) { case Box(var x) -> { String s = x; } }
    }

    <T extends Box<String>> void recordBound(T value) {
        Box(var inferred) = value;
        String a = inferred;
        Box<String>(var explicit) = value;
        String b = explicit;
        Box<?>(var wildcard) = value;
        Object c = wildcard;
    }

    void capturedRecords(List<? extends Box<String>> values) {
        Box(var inferred) = values.getFirst();
        String a = inferred;
        Box<String>(var explicit) = values.getFirst();
        String b = explicit;
    }

    <T extends Point> void pointBound(T value) {
        Point(var x, var y) = value;
        int sum = x + y;
    }

    void capturedPoints(List<? extends Point> values) {
        Point(var x, var y) = values.getFirst();
        int sum = x + y;
    }

    void intersection(Point value) {
        Point(var x, var y) = (Point & Marker) value;
        int sum = x + y;
    }

    <T extends Box<String>> void enhancedForRecordBound(Iterable<T> rows, T[] array) {
        for (Box(var x) : rows) { String s = x; }
        for (Box<String>(var x) : rows) { String s = x; }
        for (Box(var x) : array) { String s = x; }
        for (Box<String>(var x) : array) { String s = x; }
    }

    void enhancedForCapturedRecords(List<? extends Box<String>> rows,
                                    Iterable<? extends Box<String>> iterable) {
        for (Box(var x) : rows) { String s = x; }
        for (Box<String>(var x) : rows) { String s = x; }
        for (Box<?>(var x) : rows) { Object o = x; }
        for (Box(var x) : iterable) { String s = x; }
    }

    <T extends Point & Marker> void enhancedForIntersectionBound(List<T> rows) {
        for (Point(var x, var y) : rows) { int sum = x + y; }
    }

    void nestedCapturedLeaves(List<? extends Box<Box<String>>> values) {
        Box(Box(var inferred)) = values.getFirst();
        String inferredResult = inferred;
        Box<Box<String>>(Box(var targeted)) = values.getFirst();
        String targetedResult = targeted;
        for (Box(Box(var x)) : values) { String result = x; }
        for (Box<Box<String>>(Box(var x)) : values) { String result = x; }
        Box(Box(String typed)) = values.getFirst();
        String typedResult = typed;
        Box<Box<String>>(Box(String typedTargeted)) = values.getFirst();
        String typedTargetedResult = typedTargeted;
        for (Box(Box(String x)) : values) { String result = x; }
        for (Box<Box<String>>(Box(String x)) : values) { String result = x; }
        Box(Box(_)) = values.getFirst();
        Box<Box<String>>(Box(_)) = values.getFirst();
        for (Box(Box(_)) : values) {}
        for (Box<Box<String>>(Box(_)) : values) {}
        Box(Box(var _)) = values.getFirst();
        Box<Box<String>>(Box(var _)) = values.getFirst();
        for (Box(Box(var _)) : values) {}
        for (Box<Box<String>>(Box(var _)) : values) {}
        Box(Box(String _)) = values.getFirst();
        Box<Box<String>>(Box(String _)) = values.getFirst();
        for (Box(Box(String _)) : values) {}
        for (Box<Box<String>>(Box(String _)) : values) {}
    }

    void nestedSealedPatterns(Box<S> value, List<Box<S>> rows) {
        Box(R(var inferred)) = value;
        Object inferredResult = inferred;
        Box(R<?>(var nested)) = value;
        Object o = nested;
        for (Box(R(var x)) : rows) { Object result = x; }
        for (Box(R<?>(var x)) : rows) { Object other = x; }
    }

    void nestedParameterizedSealedPatterns(Box<Carrier<String>> value,
                                           List<Box<Carrier<String>>> rows) {
        Box(Rec(var nested)) = value;
        String text = nested;
        for (Box(Rec(var x)) : rows) { String other = x; }
        Box(Rec(_)) = value;
        Box(Rec(var _)) = value;
        Box(Rec(String _)) = value;
        for (Box(Rec(_)) : rows) {}
        for (Box(Rec(var _)) : rows) {}
        for (Box(Rec(String _)) : rows) {}
    }

    void ordinaryUncheckedAssignments(Box value, List<Box> rows) {
        Box<String> ordinary = value;
        for (Box<String> other : rows) {}
    }

    void nullInitializers() {
        Point(var x, var y) = null;
        Box(var inferred) = null;
        Object a = inferred;
        Box<?>(var wildcard) = null;
        Object b = wildcard;
        Box<String>(var explicit) = null;
        String c = explicit;
    }

    void successiveDeclarations() {
        Box<String>(var first) = empty();
        String a = first;
        Box(var second) = box("hi");
        String b = second;
        Box<?>(var third) = read();
        Object c = third;
        Box<Box<String>>(Box(var fourth)) = nestedEmpty();
        String d = fourth;
    }

    void deprecatedDeclaration() {
        @Deprecated Box<String>(var x) = empty();
    }

    void explicitTarget() {
        Box<String>(var x) = empty();
        String s = x;
    }

    void arrayTypeArguments() {
        Box<String[]>(var strings) = new Box<>(new String[] {"a"});
        String text = strings[0];
        Box<int[]>(int[] ints) = new Box<>(new int[] {1});
        int number = ints[0];
        Box<String[][]>(var matrix) = new Box<>(new String[][] {{"matrix"}});
        String cell = matrix[0][0];
        Pair<int[][], String[]>(var numbers, String[] names) =
                new Pair<>(new int[][] {{7}}, new String[] {"pair"});
        int firstNumber = numbers[0][0];
        Box<java.lang.String[]>(java.lang.String[] qualified) = new Box<>(new String[] {"qualified"});
        Box<String[]>(_) = new Box<>(new String[] {"unnamed"});
        Box<int[]>(var _) = new Box<>(new int[] {11});
        Box<String[]>(String[] _) = new Box<>(new String[] {"typed unnamed"});
    }

    void nestedArrayTypeArguments() {
        Box<Box<String[]>>(Box<String[]>(var two)) = new Box<>(new Box<>(new String[] {"two"}));
        String twoResult = two[0];
        Box<Box<Box<String[]>>>(Box<Box<String[]>>(Box(String[] three))) =
                new Box<>(new Box<>(new Box<>(new String[] {"three"})));
        String threeResult = three[0];
        Box<Box<Box<Box<int[][]>>>>(Box(Box(Box(var four)))) =
                new Box<>(new Box<>(new Box<>(new Box<>(new int[][] {{13}}))));
        int fourResult = four[0][0];
        Pair<Box<String[]>, Box<int[]>>(Box(String[] text), Box<int[]>(var values)) =
                new Pair<>(new Box<>(new String[] {"nested"}), new Box<>(new int[] {17}));
        int value = values[0];
        Box(Box<String[]>(var _)) = new Box<>(new Box<>(new String[] {"nested unnamed"}));
    }

    void wildcardArrayTypeArguments() {
        Box<? extends String[]>(var upper) = new Box<>(new String[] {"extends"});
        String text = upper[0];
        Box<? super String[]>(Object lower) = new Box<Object>(new String[] {"super"});
        Pair<? extends int[], ? super String[][]>(var ints, var strings) =
                new Pair<int[], Object>(new int[] {19}, new String[][] {{"wildcard"}});
        int number = ints[0];
        Object result = strings;
    }

    void arrayTypeArgumentsInLoops(List<Box<String[]>> rows, Box<int[]>[] boxes,
                                   Box<Box<String[]>>[] nested) {
        for (Box<String[]>(String[] row) : rows) { String text = row[0]; }
        for (Box<int[]>(var values) : boxes) { int number = values[0]; }
        for (Box<Box<String[]>>(Box<String[]>(var values)) : nested) { String text = values[0]; }
        for (Box<String[]>(_) : rows) {}
        for (Box<int[]>(var _) : boxes) {}
        for (Box<String[]>(String[] _) : rows) {}
    }
}
