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
 * @summary Check suggestions to infer record pattern type arguments without changing types
 * @enablePreview
 * @compile -Werror -XDrawDiagnostics -XDshould-stop.ifNoError=FLOW -XDshould-stop.ifError=FLOW RecordPatternAnalyzerTest.java
 * @compile/ref=RecordPatternAnalyzerTest.out -XDrawDiagnostics -XDfind=recordpattern -XDshould-stop.ifNoError=FLOW -XDshould-stop.ifError=FLOW RecordPatternAnalyzerTest.java
 * @compile/fail/ref=RecordPatternAnalyzerTestWerror.out -Werror -XDrawDiagnostics -XDfind=recordpattern -XDshould-stop.ifNoError=FLOW -XDshould-stop.ifError=FLOW RecordPatternAnalyzerTest.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

public class RecordPatternAnalyzerTest {
    record Box<T>(T value) {}

    @Target(ElementType.LOCAL_VARIABLE)
    @interface Local {}

    @Target(ElementType.TYPE_USE)
    @interface Use {}

    static <T> Box<T> box(T value) { return new Box<>(value); }
    static <T> Box<T> empty() { return new Box<>(null); }
    static <T> Box<Box<T>> nestedEmpty() { return new Box<>(empty()); }
    static <T> T read() { return null; }

    void declarationAnnotation() {
        @Local Box<String>(var value) = box("hi");
    }

    void typeArgumentAnnotation() {
        Box<@Use String>(var value) = box("hi");
    }

    void typePreservingDeclarations() {
        {
            Box<String>(var x) = box("hi");
            String result = x;
        }
        {
            Box<String>(String x) = box("hi");
            String result = x;
        }
        {
            Box<String>(_) = box("hi");
        }
        {
            Box<String>(var _) = box("hi");
        }
        {
            Box<String>(String _) = box("hi");
        }
    }

    void qualifiedRecordType(Box<String> value) {
        RecordPatternAnalyzerTest.Box<String>(var x) = value;
        String result = x;
    }

    <T> void genericRecordType(Box<T> value) {
        Box<T>(var x) = value;
        T result = x;
    }

    void essentialInitializerTargets() {
        {
            Box<String>(var x) = empty();
        }
        {
            Box<String>(var x) = read();
        }
        {
            Box<String>(String x) = empty();
        }
        {
            Box<String>(String x) = read();
        }
        {
            Box<String>(_) = empty();
        }
        {
            Box<String>(_) = read();
        }
        {
            Box<String>(var _) = empty();
        }
        {
            Box<String>(var _) = read();
        }
        {
            Box<String>(String _) = empty();
        }
        {
            Box<String>(String _) = read();
        }
    }

    void essentialNestedTarget() {
        Box<Box<String>>(Box(var x)) = nestedEmpty();
        String result = x;
    }

    void wildcardTargetChangesComponentType() {
        Box<?>(var x) = box("hi");
        Object result = x;
        Box<?>(_) = box("hi");
    }

    void explicitComponentChangesRecordType() {
        Box<Object>(Object x) = box("hi");
    }

    void wildcardSource(Box<?> value) {
        Box<?>(var x) = value;
        Object result = x;
        Box<?>(_) = value;
    }

    void upperBoundSource(Box<? extends CharSequence> value) {
        Box<? extends CharSequence>(var x) = value;
        int length = x.length();
    }

    void lowerBoundSource(Box<? super String> value) {
        Box<? super String>(var x) = value;
    }

    void nestedPatterns(Box<Box<String>> value) {
        {
            Box<Box<String>>(Box<String>(var x)) = value;
        }
        {
            Box<Box<String>>(Box<String>(String x)) = value;
        }
        {
            Box<Box<String>>(Box<String>(_)) = value;
        }
        {
            Box<Box<String>>(Box<String>(var _)) = value;
        }
        {
            Box<Box<String>>(Box<String>(String _)) = value;
        }
    }

    void nestedInitializerTargets() {
        {
            Box<Box<String>>(Box<String>(var x)) = nestedEmpty();
        }
        {
            Box<Box<String>>(Box<String>(String x)) = nestedEmpty();
        }
        {
            Box<Box<String>>(Box<String>(_)) = nestedEmpty();
        }
        {
            Box<Box<String>>(Box<String>(var _)) = nestedEmpty();
        }
        {
            Box<Box<String>>(Box<String>(String _)) = nestedEmpty();
        }
    }

    void nestedWildcardSource(Box<Box<?>> value) {
        Box<Box<?>>(Box<?>(var x)) = value;
        Object result = x;
    }

    void deeplyNestedSource(Box<Box<Box<String>>> value) {
        Box<Box<Box<String>>>(Box<Box<String>>(Box<String>(var x))) = value;
        String result = x;
    }

    void iterablePatterns(List<Box<String>> rows) {
        for (Box<String>(var x) : rows) {}
        for (Box<String>(String x) : rows) {}
        for (Box<String>(_) : rows) {}
        for (Box<String>(var _) : rows) {}
        for (Box<String>(String _) : rows) {}
    }

    void arrayPatterns(Box<Box<String>>[] rows) {
        for (Box<Box<String>>(Box<String>(var x)) : rows) {}
        for (Box<Box<String>>(Box<String>(String x)) : rows) {}
        for (Box<Box<String>>(Box<String>(_)) : rows) {}
        for (Box<Box<String>>(Box<String>(var _)) : rows) {}
        for (Box<Box<String>>(Box<String>(String _)) : rows) {}
    }

    void loopWildcardChangesComponentType(List<Box<String>> rows) {
        for (Box<?>(var x) : rows) {}
    }

    boolean instanceOfPattern(Box<String> value) {
        return value instanceof Box<String>(var x);
    }

    void switchStatementPattern(Box<String> value) {
        switch (value) {
            case Box<String>(var x) -> { String result = x; }
        }
    }

    String switchExpressionPattern(Box<String> value) {
        return switch (value) {
            case Box<String>(var x) -> x;
        };
    }

    void nestedSwitchPattern(Box<Box<String>> value) {
        switch (value) {
            case Box<Box<String>>(Box<String>(var x)) -> {}
        }
    }

    void instanceOfInsideInitializer(Box<String> value) {
        Box<Boolean>(var matched) = box(value instanceof Box<String>(var x));
    }

    void switchInsideInitializer(Box<String> value) {
        Box<String>(var text) = box(switch (value) {
            case Box<String>(var x) -> x;
        });
    }

    void instanceOfInsideIterable(Box<String> value, List<Box<String>> rows) {
        for (Box<String>(var x) :
                (value instanceof Box<String>(var ignored) ? rows : rows)) {}
    }

    void multipleDeclarations() {
        Box<String>(var first) = box("a");
        Box<String>(var second) = box(first);
        Box<Box<String>>(Box<String>(var third)) = box(box(second));
        String result = first + second + third;
    }

    void declarationInsidePatternScope(Box<String> value) {
        if (value instanceof Box<String>(var x) && !x.isEmpty()) {
            Box<String>(var y) = box(x);
            String result = y;
        }
    }

    void analyzerMode() {
        Box<String>(var x) = box("hi");
    }

    void implicitlyInferredPattern() {
        Box(var x) = box("hi");
    }

    void ordinaryVariable() {
        Box<String> value = box("hi");
    }
}
