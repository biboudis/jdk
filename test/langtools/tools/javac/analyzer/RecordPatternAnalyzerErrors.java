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
 * @summary Check record pattern analyzer modes and recovery after declaration errors
 * @enablePreview
 * @compile/fail/ref=RecordPatternAnalyzerErrors.out -XDrawDiagnostics -XDfind=recordpattern -XDshould-stop.ifNoError=FLOW -XDshould-stop.ifError=FLOW RecordPatternAnalyzerErrors.java
 * @compile/fail/ref=RecordPatternAnalyzerErrors.out -XDrawDiagnostics -XDfind=all -XDshould-stop.ifNoError=FLOW -XDshould-stop.ifError=FLOW RecordPatternAnalyzerErrors.java
 * @compile/fail/ref=RecordPatternAnalyzerErrorsDisabled.out -XDrawDiagnostics -XDfind=all,-recordpattern -XDshould-stop.ifNoError=FLOW -XDshould-stop.ifError=FLOW RecordPatternAnalyzerErrors.java
 */

public class RecordPatternAnalyzerErrors {
    record Box<T>(T value) {}
    record R(Object value) {}
    sealed interface Carrier<T> permits Rec {}
    record Rec<T>(T value) implements Carrier<T> {}

    static <T> Box<T> box(T value) { return new Box<>(value); }
    static <T> Box<T> empty() { return new Box<>(null); }

    void incompatibleComponent() {
        Box<Integer>(String bad) = empty();
        Box<String>(var good) = box("hi");
    }

    void incompatibleInferredRecord(Carrier<String> source) {
        Rec(var bad) = source;
        Box<String>(var good) = box("hi");
    }

    void validDeclarationAfterErrors() {
        Box<String>(var x) = box("hi");
    }

    void nongenericLoop(Iterable<R> rows) {
        for (R(Object value) : rows) {}
    }
}
