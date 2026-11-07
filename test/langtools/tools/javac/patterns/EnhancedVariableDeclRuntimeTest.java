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
 * @test id=normal
 * @bug 8372170
 * @summary Check execution of enhanced local variable declarations and enhanced for loops
 * @enablePreview
 * @run main EnhancedVariableDeclRuntimeTest
 */

/*
 * @test id=analyzer
 * @bug 8372170
 * @summary Check ELVD execution after speculative record pattern analysis
 * @enablePreview
 * @compile -XDfind=recordpattern EnhancedVariableDeclRuntimeTest.java
 * @run main EnhancedVariableDeclRuntimeTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class EnhancedVariableDeclRuntimeTest {
    record Box<T>(T value) {}
    record Pair<A, B>(A first, B second) {}
    record Token<T>(T value) {}
    interface Marker {}
    record Point(int x, int y) implements Marker {}
    sealed interface Carrier<T> permits Rec {}
    record Rec<T>(T value) implements Carrier<T> {}
    sealed interface S permits R {}
    record R<T>(T value) implements S {}
    record NullablePoint(Integer x, Integer y) {}
    record WithPrimitives(int x, double y) {}
    record ThrowingPoint(Integer x, Integer y) {
        @Override
        public Integer x() { throw new AccessorFailure("accessor failure"); }
    }
    record Frog(Integer x, Integer y) {}
    enum Color { RED, GREEN, BLUE }
    record ColoredPoint(NullablePoint point, Color color) {}
    record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}
    static class AccessorFailure extends AssertionError {
        AccessorFailure(String message) { super(message); }
    }
    private static int x = -1;
    private static int calls;

    public static void main(String[] args) {
        statementBindings();
        statementScopeAndShadowing();
        statementNullAndAccessorExceptions();
        iterableAndArrayLoops();
        loopNullAndAccessorExceptions();
        loopControlFlow();
        arrayComponents();
        outerTargets();
        nestedPatterns();
        enhancedFor();
        recordBoundsAndCaptures();
        nestedSealedSources();
    }

    private static void statementBindings() {
        NullablePoint(Integer a, Integer b) = new NullablePoint(1, 2);
        check(3, a + b);
        NullablePoint(var c, var d) = new NullablePoint(1, null);
        check(null, d);
        WithPrimitives(int e, double f) = new WithPrimitives(1, 2);
        check(1, e);
        check(2.0d, f);
    }

    private static void statementScopeAndShadowing() {
        NullablePoint point = new NullablePoint(10, 20);
        int sum;
        {
            NullablePoint(Integer x, Integer y) = point;
            check(10, x);
            sum = x + y;
        }
        check(30, sum);
        check(-1, EnhancedVariableDeclRuntimeTest.x);
        {
            NullablePoint(Integer x, Integer y) = new NullablePoint(1, 2);
            check(3, x + y);
        }
    }

    private static void statementNullAndAccessorExceptions() {
        expectException(NullPointerException.class, () -> {
            NullablePoint(Integer x, Integer y) = null;
        });
        MatchException exception = expectException(MatchException.class, () -> {
            ThrowingPoint(Integer x, Integer y) = new ThrowingPoint(1, 2);
        });
        check(AccessorFailure.class, exception.getCause().getClass());
    }

    private static void iterableAndArrayLoops() {
        List<NullablePoint> points = List.of(new NullablePoint(1, 2), new NullablePoint(2, 3));
        int sum = 0;
        for (NullablePoint(Integer a, Integer b) : points) {
            sum += a + b;
        }
        check(8, sum);
        sum = 0;
        for (NullablePoint(Integer a, Integer b) : points.toArray(NullablePoint[]::new)) {
            sum += a + b;
        }
        check(8, sum);
        sum = 0;
        for (NullablePoint(Integer a, var b) : points) {
            sum += a + b;
        }
        check(8, sum);
        sum = 0;
        for (WithPrimitives(int a, double b) :
                List.of(new WithPrimitives(1, 2), new WithPrimitives(2, 3))) {
            sum += a + (int) b;
        }
        check(8, sum);

        Rectangle[] rectangles = {new Rectangle(
                new ColoredPoint(new NullablePoint(1, 2), Color.RED),
                new ColoredPoint(new NullablePoint(3, 4), Color.GREEN))};
        List<Color> colors = new ArrayList<>();
        for (Rectangle(ColoredPoint(NullablePoint point, Color color), ColoredPoint lower) : rectangles) {
            colors.add(color);
        }
        check(List.of(Color.RED), colors);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void loopNullAndAccessorExceptions() {
        List<NullablePoint> points = List.of(new NullablePoint(1, 2), new NullablePoint(2, 3));
        check(8, sumPoints(points));
        expectException(NullPointerException.class, () -> sumPoints(null));
        List<NullablePoint> nullElement = new ArrayList<>(points);
        nullElement.add(null);
        expectException(NullPointerException.class, () -> sumPoints(nullElement));
        List<NullablePoint> nullComponent = List.of(new NullablePoint(1, null), new NullablePoint(2, 3));
        expectException(NullPointerException.class, () -> sumPoints(nullComponent));
        check(2, countPoints(points));
        check(2, countPoints(nullComponent));
        expectException(NullPointerException.class, () -> countPoints(nullElement));
        MatchException exception = expectException(MatchException.class, () -> {
            for (ThrowingPoint(var a, var b) : List.of(new ThrowingPoint(1, 2))) {
                check(3, a + b);
            }
        });
        check(AccessorFailure.class, exception.getCause().getClass());
        List polluted = List.of(new NullablePoint(1, 2), new Frog(3, 4));
        expectException(ClassCastException.class, () -> sumPoints((List<NullablePoint>) polluted));
    }

    private static int sumPoints(List<NullablePoint> points) {
        int sum = 0;
        for (NullablePoint(var a, var b) : points) {
            sum += a + b;
        }
        return sum;
    }

    private static int countPoints(List<NullablePoint> points) {
        int count = 0;
        for (NullablePoint(var a, var b) : points) {
            count++;
        }
        return count;
    }

    private static void loopControlFlow() {
        List<NullablePoint> points = List.of(new NullablePoint(1, 2), new NullablePoint(2, 3));
        check(3, returnFromLoop(points));
        int sum = 0;
        for (NullablePoint(var a, var b) : points) {
            if (a == 1) break;
            sum += a + b;
        }
        check(0, sum);
    }

    private static int returnFromLoop(List<NullablePoint> points) {
        for (NullablePoint(var a, var b) : points) {
            return a + b;
        }
        return -1;
    }

    private static void arrayComponents() {
        Box<String[]>(var strings) = new Box<>(new String[] {"a", "bc"});
        check("bc", strings[1]);
        Box<int[]>(int[] ints) = new Box<>(new int[] {3, 5});
        check(8, ints[0] + ints[1]);
        Box<Box<String[]>>(Box<String[]>(var nested)) = new Box<>(new Box<>(new String[] {"nested"}));
        check("nested", nested[0]);
        Box<? extends String[]>(var upper) = new Box<>(new String[] {"extends"});
        check("extends", upper[0]);
        Box<? super String[]>(Object lower) = new Box<Object>(new String[] {"super"});
        check("super", ((String[]) lower)[0]);
        int before = calls;
        Box<String[]>(_) = read(new Token<>(box(new String[] {"unnamed"})));
        Box<int[]>(var _) = read(new Token<>(box(new int[] {11})));
        Box<String[]>(String[] _) = read(new Token<>(box(new String[] {"typed unnamed"})));
        check(before + 3, calls);

        List<Box<String[]>> rows = List.of(box(new String[] {"a"}), box(new String[] {"bc"}));
        int length = 0;
        for (Box<String[]>(String[] values) : rows) {
            length += values[0].length();
        }
        check(3, length);
        Box<int[]>[] boxes = array(box(new int[] {2}), box(new int[] {3}));
        int sum = 0;
        for (Box<int[]>(var values) : boxes) {
            sum += values[0];
        }
        check(5, sum);
        int nestedLength = 0;
        for (Box<Box<String[]>>(Box<String[]>(var values)) : array(box(box(new String[] {"nested loop"})))) {
            nestedLength += values[0].length();
        }
        check(11, nestedLength);
        int count = 0;
        for (Box<String[]>(_) : rows) { count++; }
        for (Box<int[]>(var _) : boxes) { count++; }
        for (Box<String[]>(String[] _) : rows) { count++; }
        check(6, count);
    }

    private static <T extends RuntimeException> T expectException(Class<T> expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (exception.getClass() != expected) {
                throw new AssertionError("Expected " + expected + ", got " + exception, exception);
            }
            return expected.cast(exception);
        }
        throw new AssertionError("Expected " + expected);
    }

    private static <T> Box<T> empty() {
        calls++;
        return new Box<>(null);
    }

    private static <T> Box<T> box(T value) {
        return new Box<>(value);
    }

    private static <T> Box<Box<T>> nestedEmpty() {
        return box(empty());
    }

    private static <T> T read(Token<T> token) {
        calls++;
        return token.value();
    }

    private static String pick(String value) { return "String"; }
    private static String pick(Object value) { return "Object"; }

    private static void outerTargets() {
        int before = calls;
        Box<String>(var targeted) = empty();
        check("String", pick(targeted));
        check(null, targeted);
        check(before + 1, calls);

        Box(var standalone) = empty();
        check("Object", pick(standalone));

        Token<Box<String>> token = new Token<>(box("hello"));
        before = calls;
        Box(var precise) = read(token);
        check("String", pick(precise));
        check("hello", precise);
        check(before + 1, calls);

        Box<?>(var wildcard) = read(token);
        check("Object", pick(wildcard));
        check("hello", wildcard);

        Box<Function<String, String>>(var lambda) = new Box<>(s -> s.trim());
        check("hello", lambda.apply(" hello "));
        Box<Function<String, String>>(var reference) = new Box<>(String::trim);
        check("hello", reference.apply(" hello "));
    }

    private static void nestedPatterns() {
        Box<Box<String>> value = box(box("hello"));
        Box(Box(var inferred)) = value;
        check("String", pick(inferred));
        check("hello", inferred);

        Box(Box(String typed)) = value;
        check("hello", typed);
        Box(Box(_)) = value;
        Box(Box(var _)) = value;
        Box(Box(String _)) = value;
        Box(_) = value;

        int before = calls;
        Box<Box<String>>(Box(var fromTarget)) = nestedEmpty();
        check("String", pick(fromTarget));
        check(null, fromTarget);
        check(before + 1, calls);

        Token<Box<Box<String>>> token = new Token<>(value);
        Box<Box<String>>(Box(_)) = read(token);
        Box<Box<String>>(Box(var _)) = read(token);
        Box<Box<String>>(Box(String _)) = read(token);

        Box(Box(Box(var deep))) = box(value);
        check("String", pick(deep));
        check("hello", deep);

        Pair<Box<String>, Box<Integer>> pair = new Pair<>(box("hello"), box(7));
        Pair(Box(var text), Box(_)) = pair;
        check("String", pick(text));
        check("hello", text);
        Pair(_, Box(var number)) = pair;
        check(7, number);
    }

    private static void enhancedFor() {
        List<Box<Box<String>>> rows = List.of(box(box("a")), box(box("bc")));
        int length = 0;
        for (Box(Box(var text)) : rows) {
            check("String", pick(text));
            length += text.length();
        }
        check(3, length);

        int count = 0;
        for (Box(Box(_)) : rows) { count++; }
        for (Box(Box(var _)) : rows) { count++; }
        for (Box(Box(String _)) : rows) { count++; }
        for (Box(_) : rows) { count++; }
        check(8, count);

        Box<Box<String>>[] array = array(rows.get(0), rows.get(1));
        length = 0;
        for (Box<Box<String>>(Box(String text)) : array) {
            length += text.length();
        }
        check(3, length);
        count = 0;
        for (Box<Box<String>>(Box(_)) : array) { count++; }
        for (Box<Box<String>>(Box(var _)) : array) { count++; }
        for (Box<Box<String>>(Box(String _)) : array) { count++; }
        check(6, count);

        List<Pair<Box<String>, Box<Integer>>> pairs =
                List.of(new Pair<>(box("a"), box(1)), new Pair<>(box("bc"), box(2)));
        int sum = 0;
        for (Pair(_, Box(var number)) : pairs) { sum += number; }
        check(3, sum);
    }

    private static void recordBoundsAndCaptures() {
        Box<String> value = box("hello");
        boundedSources(value, List.of(value));

        List<? extends Box<String>> rows = List.of(value);
        Box(var captured) = rows.getFirst();
        check("String", pick(captured));
        check("hello", captured);
        Box<?>(var wildcard) = rows.getFirst();
        check("Object", pick(wildcard));
        check("hello", wildcard);

        Token<? extends Box<String>> token = new Token<>(value);
        int before = calls;
        Box(var fromToken) = read(token);
        check("String", pick(fromToken));
        check("hello", fromToken);
        check(before + 1, calls);

        before = calls;
        int count = 0;
        for (Box(var text) : capturedRows(rows)) {
            check("String", pick(text));
            check("hello", text);
            count++;
        }
        check(1, count);
        check(before + 1, calls);
        for (Box<String>(var text) : rows) {
            check("String", pick(text));
            check("hello", text);
        }

        Point valuePoint = new Point(1, 2);
        Point(var x, var y) = (Point & Marker) valuePoint;
        check(3, x + y);

        List<? extends Box<Box<String>>> nested = List.of(box(value));
        Box(Box(var nestedText)) = nested.getFirst();
        check("String", pick(nestedText));
        check("hello", nestedText);
        count = 0;
        for (Box(Box(_)) : nested) { count++; }
        for (Box(Box(var _)) : nested) { count++; }
        for (Box<Box<String>>(Box(String _)) : nested) { count++; }
        check(3, count);
    }

    private static <T extends Box<String>> void boundedSources(T value, Iterable<T> rows) {
        Box(var text) = value;
        check("String", pick(text));
        check("hello", text);
        for (Box(var element) : rows) {
            check("String", pick(element));
            check("hello", element);
        }
        for (Box<String>(var element) : rows) {
            check("String", pick(element));
            check("hello", element);
        }
    }

    private static List<? extends Box<String>> capturedRows(List<? extends Box<String>> rows) {
        calls++;
        return rows;
    }

    private static void nestedSealedSources() {
        Box<Carrier<String>> typed = box(new Rec<>("precise"));
        Box(Rec(var text)) = typed;
        check("String", pick(text));
        check("precise", text);
        for (Box(Rec(var element)) : List.of(typed)) {
            check("String", pick(element));
            check("precise", element);
        }
        Box(Rec(_)) = typed;
        Box(Rec(var _)) = typed;
        Box(Rec(String _)) = typed;

        Box<S> untyped = box(new R<>("unknown"));
        Box(R(var inferred)) = untyped;
        check("Object", pick(inferred));
        check("unknown", inferred);
        Box(R<?>(var unknown)) = untyped;
        check("Object", pick(unknown));
        check("unknown", unknown);
        for (Box(R(var element)) : List.of(untyped)) {
            check("Object", pick(element));
            check("unknown", element);
        }
        for (Box(R<?>(var element)) : List.of(untyped)) {
            check("Object", pick(element));
            check("unknown", element);
        }
    }

    @SafeVarargs
    private static <T> Box<T>[] array(Box<T>... values) {
        return values;
    }

    private static void check(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
