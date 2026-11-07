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
 * @summary Check retained type annotations on ELVDs and ordinary local declarations
 * @enablePreview
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run junit EnhancedVariableDeclTypeAnnotationsTest
 */

import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toolbox.JavacTask;
import toolbox.ToolBox;

import static org.junit.jupiter.api.Assertions.*;

public class EnhancedVariableDeclTypeAnnotationsTest {
    private final ToolBox tb = new ToolBox();
    private static final String SOURCE_PREFIX = """
            import java.lang.annotation.*;
            import java.util.*;
            class Test {
                @Target(ElementType.LOCAL_VARIABLE) @Retention(RetentionPolicy.RUNTIME)
                @interface Local { int value() default 0; }
                @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface Use {}
                @Target({ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
                @Retention(RetentionPolicy.RUNTIME) @interface Both {}
                @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.CLASS) @interface Invisible {}
                @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.SOURCE) @interface Source {}
                record Box<T>(T value) {}
            """;

    private Path compile(String body, String debug) throws Exception {
        Path out = Files.createTempDirectory(Path.of("."), "annotations");
        new JavacTask(tb).options("--enable-preview", "--source",
                        Integer.toString(Runtime.version().feature()), debug)
                .outdir(out).sources(SOURCE_PREFIX + body + "}").run();
        return out;
    }

    @Test
    public void metadata() throws Exception {
        for (String debug : List.of("-g", "-g:none")) {
            Path out = compile("""
                    void declaration() { @Local Box<String>(_) = new Box<>("d"); }
                    void typed() { @Local @Use @Both Box<@Use String>(var x) = new Box<>("t"); }
                    void named() { @Local @Use @Both Box<@Use String> value = new Box<>("t"); }
                    void unnamed() { @Local @Use @Both Box<@Use String> _ = new Box<>("t"); }
                    void wildcard() { @Local Box<? extends @Use CharSequence>(_) = new Box<String>("w"); }
                    void namedWildcard() { @Local Box<? extends @Use CharSequence> value = new Box<String>("w"); }
                    void unnamedWildcard() { @Local Box<? extends @Use CharSequence> _ = new Box<String>("w"); }
                    void bare() { @Use Box(var x) = new Box<>("b"); }
                    void component() { Box<String>(@Use String value) = new Box<>("c"); }
                    void nestedComponent() { Box<Box<String>>(Box<String>(@Use String value)) = new Box<>(new Box<>("n")); }
                    void componentLoop(List<Box<String>> rows) { for (Box<String>(@Use String value) : rows) { value.length(); } }
                    void loop(List<Box<String>> rows) { for (@Use Box<String>(var x) : rows) {} }
                    void array(Box<String>[] rows) { for (@Use Box<String>(_) : rows) {} }
                    void retention() { @Source @Invisible Box<String>(_) = new Box<>("r"); }
                    """, debug);
            var model = ClassFile.of().parse(out.resolve("Test.class"));
            Map<String, List<TypeAnnotation>> retained = new java.util.HashMap<>();
            for (var method : model.methods()) {
                String name = method.methodName().stringValue();
                if (!List.of("declaration", "typed", "named", "unnamed", "wildcard", "namedWildcard",
                        "unnamedWildcard", "bare", "component", "nestedComponent", "componentLoop",
                        "loop", "array", "retention").contains(name)) continue;
                boolean typed = List.of("typed", "named", "unnamed").contains(name);
                boolean componentBinding = List.of("component", "nestedComponent", "componentLoop").contains(name);
                var code = method.code().orElseThrow();
                List<TypeAnnotation> annotations = new ArrayList<>();
                List<TypeAnnotation> invisible = new ArrayList<>();
                for (var attribute : code.attributes()) {
                    if (attribute instanceof RuntimeVisibleTypeAnnotationsAttribute types) {
                        annotations.addAll(types.annotations());
                    } else if (attribute instanceof RuntimeInvisibleTypeAnnotationsAttribute types) {
                        invisible.addAll(types.annotations());
                    }
                }
                assertEquals(name.equals("declaration") || name.equals("retention") ? 0 : typed ? 3 : 1,
                        annotations.size(), name);
                assertEquals(name.equals("retention") ? 1 : 0, invisible.size(), name);
                if (name.equals("retention")) {
                    assertEquals("LTest$Invisible;", invisible.getFirst().annotation().className().stringValue());
                }
                annotations.addAll(invisible);
                for (var annotation : annotations) {
                    assertNotEquals("LTest$Local;", annotation.annotation().className().stringValue());
                    assertEquals(TypeAnnotation.TargetType.LOCAL_VARIABLE,
                            annotation.targetInfo().targetType());
                    var target = (TypeAnnotation.LocalVarTarget) annotation.targetInfo();
                    assertFalse(target.table().isEmpty(), name);
                    assertEquals(componentBinding ? target.table().size() : 1, target.table().stream().map(range ->
                            List.of(((CodeAttribute) code).labelToBci(range.startLabel()),
                                    ((CodeAttribute) code).labelToBci(range.endLabel()), range.index())).distinct().count(), name);
                    for (var range : target.table()) {
                        assertTrue(((CodeAttribute) code).labelToBci(range.endLabel()) > ((CodeAttribute) code).labelToBci(range.startLabel()));
                    }
                }
                if (typed) {
                    assertEquals(1, annotations.stream().filter(a -> !a.targetPath().isEmpty()).count());
                }
                retained.put(name, annotations);
            }
            for (var names : List.of(List.of("typed", "named", "unnamed"),
                    List.of("wildcard", "namedWildcard", "unnamedWildcard"))) {
                var expected = retained.get(names.getFirst());
                for (String name : names) {
                    var actual = retained.get(name);
                    assertEquals(expected.stream().map(a ->
                                    a.annotation().className().stringValue() + ":" + a.targetPath()).sorted().toList(),
                            actual.stream().map(a ->
                                    a.annotation().className().stringValue() + ":" + a.targetPath()).sorted().toList(), name);
                }
            }
        }
    }
}
