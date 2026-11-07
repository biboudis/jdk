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
 * @summary Check pattern-based declarations through the existing variable tree API and visitors
 * @enablePreview
 * @run main EnhancedVariableDeclTreeApiTest
 */

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.DeconstructionPatternTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class EnhancedVariableDeclTreeApiTest {
    private static final String SOURCE = """
            import java.util.List;
            class Test {
                record Box<T>(T value) {}
                record Pair<A, B>(A first, B second) {}
                static <T> T make() { return null; }
                void test(List<Box<String>> rows, Box<String>[] array) {
                    Box<String> ordinary = new Box<>("ordinary");
                    var _ = new Box<>("unnamed");
                    Box(var inferred) = new Box<>("inferred");
                    Box<String>(var explicit) = make();
                    Box<?>(var wildcard) = new Box<>("wildcard");
                    Box(Box(var nested)) = new Box<>(new Box<>("nested"));
                    Box(_) = new Box<>("ignored");
                    Box(var _) = new Box<>("unnamed binding");
                    Pair(var left, _) = new Pair<>("left", 1);
                    for (Box<String> item : rows) {}
                    for (Box(var iterated) : rows) {}
                    for (Box(var arrayValue) : array) {}
                }
            }
            """;

    public static void main(String... args) throws Exception {
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var source = new SimpleJavaFileObject(URI.create("string:///Test.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return SOURCE;
            }
        };
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null, diagnostics,
                List.of("--enable-preview", "--source", Integer.toString(Runtime.version().feature()), "-proc:none",
                        "-d", Files.createTempDirectory(Path.of("."), "elvd-api").toString()),
                null, List.of(source));
        CompilationUnitTree unit = task.parse().iterator().next();
        Trees trees = Trees.instance(task);
        List<TreePath> declarations = new ArrayList<>();
        Map<String, String> sourceTypes = new HashMap<>();
        int[] loopHeaders = {0};
        int[] ordinaryLoopHeaders = {0};
        int[] variableVisits = {0};
        int[] patternVisits = {0};
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree tree, Void unused) {
                return tree.getSimpleName().contentEquals("Test") ? super.visitClass(tree, unused) : null;
            }

            @Override
            public Void visitMethod(MethodTree tree, Void unused) {
                return tree.getName().contentEquals("test") ? super.visitMethod(tree, unused) : null;
            }

            @Override
            public Void visitVariable(VariableTree tree, Void unused) {
                variableVisits[0]++;
                equal(VariableTree.class, tree.getKind().asInterface());
                boolean dispatched = tree.accept(new SimpleTreeVisitor<Boolean, Void>() {
                    @Override
                    public Boolean visitVariable(VariableTree variable, Void ignored) {
                        return variable == tree;
                    }
                }, null);
                equal(true, dispatched);
                if (tree.getKind() == Tree.Kind.ENHANCED_VARIABLE) {
                    declarations.add(getCurrentPath());
                    equal("", tree.getName().toString());
                    equal(null, tree.getNameExpression());
                    equal(null, tree.getType());
                    equal(true, tree.getModifiers().getFlags().isEmpty());
                    var pattern = (DeconstructionPatternTree) tree.getPattern();
                    if (getCurrentPath().getParentPath().getLeaf() instanceof EnhancedForLoopTree loop) {
                        equal(tree, loop.getVariable());
                        equal(null, tree.getInitializer());
                        loopHeaders[0]++;
                    } else {
                        equal(true, tree.getInitializer() != null);
                    }
                    sourceTypes.put(tree.toString(), pattern.getDeconstructor().toString());
                    long start = trees.getSourcePositions().getStartPosition(tree);
                    long end = trees.getSourcePositions().getEndPosition(tree);
                    equal(true, start >= 0 && end > start);
                    equal(true, tree.toString().contains("("));
                } else {
                    equal(Tree.Kind.VARIABLE, tree.getKind());
                    equal(null, tree.getPattern());
                    if (getCurrentPath().getParentPath().getLeaf() instanceof EnhancedForLoopTree loop) {
                        equal(tree, loop.getVariable());
                        equal(null, tree.getInitializer());
                        equal("Box<String>", tree.getType().toString());
                        ordinaryLoopHeaders[0]++;
                    }
                }
                return super.visitVariable(tree, unused);
            }

            @Override
            public Void visitDeconstructionPattern(DeconstructionPatternTree tree, Void unused) {
                patternVisits[0]++;
                return super.visitDeconstructionPattern(tree, unused);
            }
        }.scan(unit, null);
        equal(9, declarations.size());
        equal(2, loopHeaders[0]);
        equal(1, ordinaryLoopHeaders[0]);
        equal(10, patternVisits[0]);
        equal(22, variableVisits[0]);
        task.analyze();
        var errors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR).toList();
        equal(List.of(), errors);
        for (TreePath path : declarations) {
            var tree = (VariableTree) path.getLeaf();
            var element = (VariableElement) trees.getElement(path);
            equal(ElementKind.LOCAL_VARIABLE, element.getKind());
            equal(true, element.isUnnamed());
            equal(true, element.getSimpleName().isEmpty());
            equal(Elements.Origin.MANDATED, task.getElements().getOrigin(element));
            equal(tree, trees.getTree(element));
            equal(tree, trees.getPath(element).getLeaf());
            var patternPath = new TreePath(path, tree.getPattern());
            equal(element, trees.getElement(patternPath));
            equal(true, task.getTypes().isSameType(element.asType(), trees.getTypeMirror(path)));
            equal(true, task.getTypes().isSameType(element.asType(), trees.getTypeMirror(patternPath)));
            equal(null, tree.getType());
            var pattern = (DeconstructionPatternTree) tree.getPattern();
            equal(sourceTypes.get(tree.toString()), pattern.getDeconstructor().toString());
        }
        Map<String, String> bindings = new HashMap<>();
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree tree, Void unused) {
                var element = trees.getElement(getCurrentPath());
                if (element instanceof VariableElement variable && variable.getKind() == ElementKind.BINDING_VARIABLE) {
                    bindings.put(tree.getName().toString(), variable.asType().toString());
                }
                return super.visitVariable(tree, unused);
            }
        }.scan(unit, null);
        for (String name : List.of("inferred", "explicit", "nested", "left", "iterated", "arrayValue")) {
            equal("java.lang.String", bindings.get(name));
        }
        equal("java.lang.Object", bindings.get("wildcard"));
        task.generate();
    }

    private static void equal(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
