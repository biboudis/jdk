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
 * @summary Check errors in ELVD inference, assignment compatibility and pattern coverage
 * @enablePreview
 * @compile/fail/ref=EnhancedVariableDeclInferenceErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW EnhancedVariableDeclInferenceErrors.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

public class EnhancedVariableDeclInferenceErrors {
    @Target(ElementType.TYPE_USE)
    @interface Use {}

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
    sealed interface Several permits One, Other {}
    record One<T>(T value) implements Several {}
    record Other(int value) implements Several {}

    static <T> Box<T> box(T value) { return new Box<>(value); }
    static <T> Box<T> empty() { return new Box<>(null); }
    static <T> Box<Box<T>> nestedEmpty() { return new Box<>(empty()); }
    static <T extends Number> Numeric<T> numeric() { return null; }
    static <T> T read() { return null; }
    static <T> T read(Token<T> token) { return token.value(); }
    static <A, B> Pair<A, B> pair(A a, B b) { return new Pair<>(a, b); }
    static <T> List<Box<T>> rows() { return List.of(); }
    static <T> List<Box<Box<T>>> nestedRows() { return List.of(); }
    static <T> Carrier<T> carrier() { return null; }

    void unconstrainedFactoryComponent() {
        Box(var x) = empty();
        String s = x;
    }

    void wildcardTargetComponent() {
        Box<?>(var x) = read();
        String s = x;
    }

    void annotationDoesNotAffectInferredComponent() {
        @Use Box(var value) = empty();
        String result = value;
    }

    void annotationDoesNotAffectWildcardComponent() {
        @Use Box<?>(var value) = new Box<>("wildcard");
        String result = value;
    }

    void unconstrainedGenericResult() {
        Box(var x) = read();
    }

    void unconstrainedGenericResultWithUnnamedPattern() {
        Box(_) = read();
    }

    void narrowedInferredComponent() {
        Box(String x) = empty();
    }

    void narrowedInferredUnnamedComponent() {
        Box(String _) = empty();
    }

    void wildcardTargetDiscardsTokenPrecision(Token<Box<String>> token) {
        Box<?>(var x) = read(token);
        String s = x;
    }

    void capturedUpperBoundComponent(Box<? extends CharSequence> bounded) {
        Box(var x) = bounded;
        String s = x;
    }

    void partiallyConstrainedFactoryArgument() {
        Pair(var text, var number) = pair("hi", null);
        Integer n = number;
    }

    void narrowedPartiallyConstrainedComponents() {
        Pair(String text, Integer number) = pair("hi", null);
    }

    void declaredNumericBoundComponent() {
        Numeric(var x) = numeric();
        Integer n = x;
    }

    void untargetedConditionalExpression(boolean flag) {
        Box(var x) = flag ? empty() : empty();
        String s = x;
    }

    void untargetedNestedFactoryComponent() {
        Box(Box(var x)) = nestedEmpty();
        String s = x;
    }

    void narrowedNestedComponent() {
        Box(Box(String x)) = nestedEmpty();
    }

    void narrowedNestedUnnamedComponent() {
        Box(Box(String _)) = nestedEmpty();
    }

    void nestedTypeDoesNotTargetInitializer() {
        Box(Box<String>(var x)) = nestedEmpty();
    }

    void nestedTypeWithUnnamedComponentDoesNotTargetInitializer() {
        Box(Box<String>(_)) = nestedEmpty();
    }

    void nestedPatternOnUnconstrainedGenericResult() {
        Box(Box(var x)) = read();
    }

    void nestedUnnamedPatternOnUnconstrainedGenericResult() {
        Box(Box(_)) = read();
    }

    void nestedPatternOnWildcardComponent() {
        Box<?>(Box(_)) = read();
    }

    void explicitLoopTypeDoesNotTargetIterable() {
        for (Box<String>(var x) : rows()) {}
    }

    void inferredLoopComponent() {
        for (Box(var x) : rows()) {
            String s = x;
        }
    }

    void wildcardLoopComponent(List<Box<String>> boxes) {
        for (Box<?>(var x) : boxes) {
            String s = x;
        }
    }

    void explicitNestedLoopTypeDoesNotTargetIterable() {
        for (Box<Box<String>>(Box(_)) : nestedRows()) {}
    }

    void inferredNestedLoopComponent() {
        for (Box(Box(var x)) : nestedRows()) {
            String s = x;
        }
    }

    void narrowedNestedLoopComponent() {
        for (Box(Box(String x)) : nestedRows()) {}
    }

    void narrowedNestedUnnamedLoopComponent() {
        for (Box(Box(String _)) : nestedRows()) {}
    }

    boolean instanceofDoesNotTargetSelector() {
        return empty() instanceof Box<String>(var x);
    }

    void switchDoesNotTargetSelector() {
        switch (empty()) {
            case Box<String>(var x) -> {}
        }
    }

    void inferredSwitchComponent() {
        switch (empty()) {
            case Box(var x) -> {
                String s = x;
            }
        }
    }

    <T extends Box<String>> void wildcardTargetDiscardsRecordBoundPrecision(T value) {
        Box<?>(var x) = value;
        String s = x;
    }

    void inferredPatternFromSealedSource(S value) {
        R(var x) = value;
    }

    void inferredPatternFromParameterizedSealedSource(Carrier<String> value) {
        Rec(var x) = value;
    }

    void inferredPatternFromSealedFactory() {
        Rec(var x) = carrier();
    }

    <T extends S> void inferredPatternFromSealedBound(T value) {
        R(var x) = value;
    }

    void inferredPatternFromCapturedSealedSource(List<? extends Carrier<String>> values) {
        Rec(var x) = values.getFirst();
    }

    void inferredUnnamedPatternsFromSealedSource(S value) {
        R(_) = value;
        R(var _) = value;
        R(Object _) = value;
    }

    void explicitWildcardPatternFromSealedSource(S value) {
        R<?>(var x) = value;
    }

    void explicitPatternFromSealedSource(S value) {
        R<String>(var x) = value;
    }

    void explicitWildcardVariableFromSealedSource(S value) {
        R<?> r = value;
    }

    void explicitPatternFromParameterizedSealedSource(Carrier<String> value) {
        Rec<String>(var x) = value;
    }

    void explicitPatternFromSealedFactory() {
        Rec<String>(var x) = carrier();
    }

    void explicitVariableFromParameterizedSealedSource(Carrier<String> value) {
        Rec<String> r = value;
    }

    void explicitVariableFromSealedFactory() {
        Rec<String> r = carrier();
    }

    void incompatibleRecordPattern(Box<?> value) {
        Point(var x, var y) = value;
    }

    void inferredLoopPatternFromSealedElements(List<S> rows) {
        for (R(var x) : rows) {}
    }

    void inferredLoopPatternFromParameterizedSealedElements(List<Carrier<String>> rows) {
        for (Rec(var x) : rows) {}
    }

    <T extends S> void inferredLoopPatternFromSealedBound(Iterable<T> rows) {
        for (R(var x) : rows) {}
    }

    void inferredLoopPatternFromCapturedSealedElements(List<? extends Carrier<String>> rows) {
        for (Rec(var x) : rows) {}
    }

    void inferredLoopPatternFromSealedArray(Carrier<String>[] rows) {
        for (Rec(var x) : rows) {}
    }

    void inferredUnnamedLoopPatternsFromSealedElements(List<S> rows, S[] array) {
        for (R(_) : rows) {}
        for (R(var _) : rows) {}
        for (R(Object _) : rows) {}
        for (R(_) : array) {}
        for (R(var _) : array) {}
        for (R(Object _) : array) {}
    }

    void explicitWildcardLoopPatternFromSealedElements(List<S> rows) {
        for (R<?>(var x) : rows) {}
    }

    void explicitLoopPatternFromSealedElements(List<S> rows) {
        for (R<String>(var x) : rows) {}
    }

    void explicitWildcardLoopVariableFromSealedElements(List<S> rows) {
        for (R<?> r : rows) {}
    }

    void explicitLoopPatternFromParameterizedSealedElements(List<Carrier<String>> rows) {
        for (Rec<String>(var x) : rows) {}
    }

    void explicitLoopVariableFromParameterizedSealedElements(List<Carrier<String>> rows) {
        for (Rec<String> r : rows) {}
    }

    void explicitNongenericLoopPatternFromSealedElements(List<IPoint> rows) {
        for (Point(var x, var y) : rows) {}
    }

    void explicitNongenericLoopPatternFromCapturedSealedElements(List<? extends IPoint> rows) {
        for (Point(var x, var y) : rows) {}
    }

    void incompatibleObjectSource(Object value) {
        Box(var x) = value;
    }

    void incompatibleRecordSuperclassSource(Record value) {
        Box(var x) = value;
    }

    void incompatibleOpenInterfaceSource(Marker value) {
        Rec(var x) = value;
    }

    void incompatibleSealedAlternative(Several value) {
        One(var x) = value;
    }

    void incompatibleObjectElements(List<Object> rows) {
        for (Box(_) : rows) {}
    }

    void incompatibleRecordSuperclassElements(List<Record> rows) {
        for (Box(_) : rows) {}
    }

    void incompatibleOpenInterfaceElements(List<Marker> rows) {
        for (Rec(_) : rows) {}
    }

    void incompatibleSealedElements(List<Several> rows) {
        for (One(_) : rows) {}
    }

    void incompatibleWildcardElements(List<?> rows) {
        for (Box(_) : rows) {}
    }

    void incompatibleRawElements(List rows) {
        for (Box(_) : rows) {}
    }

    void uncheckedPatternConversion(Box value) {
        Box<String>(var x) = value;
    }

    void uncheckedLoopPatternConversion(List<Box> rows) {
        for (Box<String>(var x) : rows) {}
    }

    void bindingNotInScopeInInitializer() {
        Box(var x) = box(x);
    }

    void errorRecoveryAfterUnknownRecordType() {
        Missing<String>(var bad) = read();
        Box<String>(var good) = empty();
        String s = good;
    }
}
