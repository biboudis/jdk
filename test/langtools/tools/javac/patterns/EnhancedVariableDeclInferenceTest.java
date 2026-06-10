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
 * @summary Check target typing and invocation result inference for enhanced variable declarations
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run junit EnhancedVariableDeclInferenceTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnhancedVariableDeclInferenceTest {

    private Path base;
    private final ToolBox tb = new ToolBox();

    @Test
    public void testTestingContextsRemainSelectorFirst() throws Exception {
        compile("""
                class InstanceofCase {
                    record Box<T>(T val) {}
                    static <T> Box<T> empty() { return new Box<T>(null); }
                    boolean test() {
                        return empty() instanceof Box<String>(String s);
                    }
                }
                """, Task.Expect.FAIL, false);

        compile("""
                class SwitchCase {
                    record Box<T>(T val) {}
                    static <T> Box<T> empty() { return new Box<T>(null); }
                    boolean test() {
                        return switch (empty()) {
                            case Box<String>(String s) -> true;
                        };
                    }
                }
                """, Task.Expect.FAIL, false);
    }

    @Test
    public void testTargetTyping() throws Exception {
        compile("""
                import java.util.function.Function;

                class TargetPropagation {
                    record Box<T>(T val) {}
                    record Holder<T>(T val) {}

                    static <T> Box<T> empty() { return new Box<T>(null); }
                    static <T> T id(T value) { return value; }

                    void test(boolean flag, int selector) {
                        Box<String>(String direct) = empty();
                        Box<String>(String parenthesized) = (empty());
                        Box<String>(String nested) = id(empty());
                        Box<String>(String conditional) = flag ? empty() : empty();
                        Box<String>(String switched) = switch (selector) {
                            case 0 -> empty();
                            default -> empty();
                        };
                        Box<String>(String diamond) = new Box<>(null);

                        Holder<Function<String, String>>(Function<String, String> lambda) =
                                new Holder<>(value -> value.trim());
                        Holder<Function<String, String>>(Function<String, String> reference) =
                                new Holder<>(String::trim);

                        Box(var standalone) = empty();
                    }
                }
                """, Task.Expect.SUCCESS, true);
    }

    @Test
    public void testDirectGenericReturn() throws Exception {
        compile("""
                class DirectGenericReturn {
                    sealed interface Carrier<T> permits Rec {}
                    record Rec<T>(T val) implements Carrier<T> {}

                    static <T> Carrier<T> carrier() { return new Rec<>(null); }
                    static Carrier<String> fixed() { return new Rec<>(null); }

                    void test() {
                        Rec<String> assignment = carrier();
                        Rec<String>(String pattern) = carrier();
                        Rec<String> explicit = DirectGenericReturn.<String>carrier();
                        Rec<String> fixedResult = fixed();
                    }

                    <T> void genericTarget() {
                        Rec<T> assignment = carrier();
                        Rec<T>(T pattern) = carrier();
                    }
                }
                """, Task.Expect.SUCCESS, true);
    }

    @Test
    public void testNestedTypeArguments() throws Exception {
        compile("""
                import java.util.List;

                class NestedArgument {
                    sealed interface Carrier<T> permits Rec {}
                    record Rec(String val) implements Carrier<List<String>> {}

                    static <T> Carrier<List<T>> carrier() { return null; }

                    void test() {
                        Rec assignment = carrier();
                        Rec(String pattern) = carrier();
                    }
                }
                """, Task.Expect.SUCCESS, true);
    }

    @Test
    public void testLooseInvocation() throws Exception {
        List<String> errors = compile("""
                class LooseInvocation {
                    sealed interface Carrier<T> permits Rec {}
                    record Rec<T>(T val) implements Carrier<T> {}

                    static <T> Carrier<T> carrier() { return new Rec<>(null); }
                    static void consume(Rec<String> value) {}

                    void test() {
                        consume(carrier());
                    }
                }
                """, Task.Expect.FAIL, true, "-XDrawDiagnostics");

        assertTrue(errors.stream().anyMatch(line ->
                line.contains("LooseInvocation.Carrier<java.lang.String>")), errors.toString());
    }

    @Test
    public void testNestedSafeNarrowing() throws Exception {
        compile("""
                class NestedSafeNarrowing {
                    sealed interface Carrier<T> permits Rec {}
                    record Rec<T>(T val) implements Carrier<T> {}

                    static <T> Carrier<T> carrier() { return new Rec<>(null); }
                    static <T> T id(T value) { return value; }

                    void test() {
                        Rec<String> assignment = id(carrier());
                        Rec<String>(String pattern) = id(carrier());
                    }
                }
                """, Task.Expect.FAIL, true);
    }

    @Test
    public void testTypeVariableReturn() throws Exception {
        compile("""
                import java.util.List;

                class ExactReturn {
                    sealed interface Sup permits Sub {}
                    static final class Sub implements Sup {}

                    static <T> T from(List<T> values) { return null; }

                    void test(List<Sup> values) {
                        Sub result = from(values);
                    }
                }
                """, Task.Expect.FAIL, true);

        compile("""
                class OrdinaryTypeVariableTarget {
                    record Box<T>(T val) {}
                    static <T> T make() { return null; }

                    void test() {
                        Box<String>(String value) = make();
                    }
                }
                """, Task.Expect.SUCCESS, true);
    }

    @Test
    public void testUnrelatedGenericReturn() throws Exception {
        compile("""
                class UnrelatedReturn {
                    record Rec<T>(T val) {}
                    record Other<T>(T val) {}

                    static <T> Other<T> other() { return null; }

                    void test() {
                        Rec<String> assignment = other();
                        Rec<String>(String pattern) = other();
                    }
                }
                """, Task.Expect.FAIL, true);
    }

    @Test
    public void testRawSupertype() throws Exception {
        List<String> errors = compile("""
                class RawSupertype {
                    sealed interface Carrier<T> permits Raw {}
                    record Raw() implements Carrier {}

                    static <T> Carrier<T> carrier() { return null; }

                    void test() {
                        Raw assignment = carrier();
                        Raw() = carrier();
                    }
                }
                """, Task.Expect.FAIL, true, "-XDrawDiagnostics");

        assertTrue(errors.stream().anyMatch(line ->
                line.contains("RawSupertype.Carrier<java.lang.Object>")), errors.toString());
    }

    @Test
    public void testNonUniqueNarrowing() throws Exception {
        List<String> errors = compile("""
                class AssignmentParity {
                    sealed interface Carrier<T> permits StringRec, IntegerRec {}
                    record StringRec(String val) implements Carrier<String> {}
                    record IntegerRec(Integer val) implements Carrier<Integer> {}

                    static <T> Carrier<T> carrier() { return null; }

                    void test() {
                        StringRec assignment = carrier();
                        StringRec(String inferred) = carrier();
                        StringRec(String explicit) = AssignmentParity.<String>carrier();
                    }
                }
                """, Task.Expect.FAIL, true, "-XDrawDiagnostics");

        assertTrue(errors.stream().anyMatch(line ->
                line.contains("AssignmentParity.Carrier<java.lang.String>")), errors.toString());
    }

    @Test
    public void testPreviewDisabled() throws Exception {
        List<String> errors = compile("""
                class PreviewDisabled {
                    sealed interface Carrier<T> permits Rec {}
                    record Rec<T>(T val) implements Carrier<T> {}

                    static <T> Carrier<T> carrier() { return null; }

                    void test() {
                        Rec<String> assignment = carrier();
                    }
                }
                """, Task.Expect.FAIL, false,
                "--source", System.getProperty("java.specification.version"), "-XDrawDiagnostics");

        assertTrue(errors.stream().anyMatch(line ->
                line.contains("PreviewDisabled.Carrier<java.lang.String>")), errors.toString());
    }

    @Test
    public void testUnresolvedEnclosingTarget() throws Exception {
        compile("""
                class NestedTargetRegression {
                    static class Statement {}
                    static class Variable extends Statement {}
                    static class Buffer<T> {}

                    static <T extends Buffer<? super Variable>> T declarations(T definitions) {
                        return definitions;
                    }

                    void test() {
                        Buffer<Statement> statements = declarations(new Buffer<>());
                    }
                }
                """, Task.Expect.SUCCESS, false);
    }

    @Test
    public void testListDoubleRegression() throws Exception {
        compile("""
                import java.util.List;

                class ListRegression {
                    void test() {
                        List<Double> values = List.of(1, 2);
                    }
                }
                """, Task.Expect.FAIL, false, "-XDrawDiagnostics");
    }

    private List<String> compile(String source, Task.Expect expect, boolean preview,
            String... additionalOptions) {
        List<String> options = new ArrayList<>();
        options.add("-XDshould-stop.at=FLOW");
        options.addAll(List.of(additionalOptions));
        if (preview) {
            options.add("--enable-preview");
            options.add("--source");
            options.add(System.getProperty("java.specification.version"));
        }
        return new JavacTask(tb)
                .options(options.toArray(String[]::new))
                .sources(source)
                .outdir(base)
                .run(expect)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
    }

    @BeforeEach
    public void setUp(TestInfo info) throws IOException {
        base = Path.of(info.getTestMethod().orElseThrow().getName());
        Files.createDirectories(base);
    }
}
