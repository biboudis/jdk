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
 * @summary Check ELVD annotation elements, source mappings and annotation processing
 * @enablePreview
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run junit EnhancedVariableDeclAnnotationModelTest
 */

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import toolbox.JavacTask;
import toolbox.ToolBox;

import static org.junit.jupiter.api.Assertions.*;

public class EnhancedVariableDeclAnnotationModelTest {
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
                record Box<T>(T value) {}
            """;

    @Test
    public void languageModel() throws Exception {
        Path base = Files.createTempDirectory(Path.of("."), "model");
        Path file = base.resolve("Test.java");
        Files.writeString(file, SOURCE_PREFIX + """
                Test() {
                    @Local Box<String> _ = new Box<>("constructor");
                    @Local Box<String>(_) = new Box<>("constructor");
                }
                void test(List<Box<String>> rows, List<Box<Box<String>>> nestedRows) {
                    @Local Box<@Use String> named = new Box<>("named");
                    @Local Box<@Use String> _ = new Box<>("unnamed");
                    @Local(7) @Use @Both Box<String>(@Local var x) = new Box<>("x");
                    @Local Box(var y) = new Box<>("y");
                    Box(_) = new Box<>("z");
                    for (@Local @Use Box<String>(var z) : rows) {}
                    @Use Box<?>(var w) = new Box<>("w");
                    @Local Box<Box<String>>(Box(@Local var nested)) = new Box<>(new Box<>("nested"));
                    @Local Box<Box<String>>(Box(_)) = new Box<>(new Box<>("nested"));
                    @Local Box<Box<String>>(Box(@Local var _)) = new Box<>(new Box<>("nested"));
                    for (@Local @Use Box<Box<String>>(Box(@Local var item)) : nestedRows) {}
                    Runnable action = () -> {
                        @Local Box<String> _ = new Box<>("lambda");
                        @Local Box<String>(_) = new Box<>("lambda");
                    };
                }
                static {
                    @Local Box<String> _ = new Box<>("static");
                    @Local Box<String>(_) = new Box<>("static");
                }
                {
                    @Local Box<String> _ = new Box<>("instance");
                    @Local Box<String>(_) = new Box<>("instance");
                }
                Runnable r = () -> {
                    @Local Box<String> _ = new Box<>("field lambda");
                    @Local Box<String>(_) = new Box<>("field lambda");
                };
                }
                """);
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            var task = (com.sun.source.util.JavacTask) compiler.getTask(null, manager,
                    null, List.of("--enable-preview", "--source", Integer.toString(Runtime.version().feature()), "-proc:none"),
                    null, manager.getJavaFileObjects(file));
            var units = task.parse();
            task.analyze();
            Trees trees = Trees.instance(task);
            AtomicInteger owners = new AtomicInteger();
            AtomicInteger ordinaryLocals = new AtomicInteger();
            Map<Tree, Element> ordinaryOwners = new IdentityHashMap<>();
            Map<AnnotationTree, Integer> annotationVisits = new IdentityHashMap<>();
            new TreePathScanner<Void, Void>() {
                void check(TreePath patternPath, TreePath declarationPath,
                           int declarations, int typeAnnotations) {
                    var owner = (VariableElement) trees.getElement(patternPath);
                    assertNotNull(owner);
                    assertEquals(ElementKind.LOCAL_VARIABLE, owner.getKind());
                    assertSame(owner, trees.getElement(declarationPath));
                    TreePath block = declarationPath;
                    while (block.getLeaf().getKind() != Tree.Kind.BLOCK) {
                        block = block.getParentPath();
                    }
                    assertSame(ordinaryOwners.get(block.getLeaf()), owner.getEnclosingElement());
                    var argument = ((DeclaredType) owner.asType()).getTypeArguments().getFirst();
                    if (declarations == 0 && typeAnnotations == 1) {
                        assertEquals(TypeKind.WILDCARD, argument.getKind());
                    } else if (argument instanceof DeclaredType declared &&
                            declared.asElement().getSimpleName().contentEquals("Box")) {
                        assertEquals("Test.Box<java.lang.String>", argument.toString());
                    } else {
                        assertEquals("java.lang.String", argument.toString());
                    }
                    assertEquals(declarations, owner.getAnnotationMirrors().size());
                    assertEquals(typeAnnotations, owner.asType().getAnnotationMirrors().size());
                    assertTrue(owner.getModifiers().isEmpty());
                    assertTrue(owner.getEnclosedElements().isEmpty());
                    assertNull(owner.getConstantValue());
                    for (var annotation : owner.getAnnotationMirrors()) {
                        assertEquals(Elements.Origin.EXPLICIT, task.getElements().getOrigin(owner, annotation));
                        assertEquals(Tree.Kind.ANNOTATION, trees.getTree(owner, annotation).getKind());
                        assertEquals(Tree.Kind.ANNOTATION, trees.getPath(owner, annotation).getLeaf().getKind());
                        assertSame(trees.getTree(owner, annotation), trees.getPath(owner, annotation).getLeaf());
                        for (var value : annotation.getElementValues().values()) {
                            assertEquals(Tree.Kind.INT_LITERAL, trees.getTree(owner, annotation, value).getKind());
                            assertEquals(Tree.Kind.INT_LITERAL, trees.getPath(owner, annotation, value).getLeaf().getKind());
                            assertSame(trees.getTree(owner, annotation, value),
                                    trees.getPath(owner, annotation, value).getLeaf());
                        }
                    }
                    owners.incrementAndGet();
                }

                @Override
                public Void visitVariable(VariableTree tree, Void unused) {
                    if (tree.getKind() == Tree.Kind.ENHANCED_VARIABLE) {
                        TreePath path = new TreePath(getCurrentPath(), tree.getPattern());
                        int count = tree.getModifiers().getAnnotations().size();
                        boolean hasTypeUse = tree.getModifiers().getAnnotations().stream()
                                .anyMatch(annotation -> annotation.getAnnotationType().toString().equals("Use"));
                        check(path, getCurrentPath(), count == 3 ? 2 : hasTypeUse ? count - 1 : count,
                                count == 3 ? 2 : hasTypeUse ? 1 : 0);
                        return super.visitVariable(tree, unused);
                    }
                    var element = trees.getElement(getCurrentPath());
                    if (element instanceof VariableElement variable &&
                            variable.getKind() == ElementKind.LOCAL_VARIABLE &&
                            !tree.getName().contentEquals("action")) {
                        assertEquals(tree.getName().isEmpty(), variable.isUnnamed());
                        assertEquals(1, variable.getAnnotationMirrors().size());
                        assertEquals(Tree.Kind.ANNOTATION,
                                trees.getTree(variable, variable.getAnnotationMirrors().getFirst()).getKind());
                        Element previous = ordinaryOwners.put(getCurrentPath().getParentPath().getLeaf(),
                                variable.getEnclosingElement());
                        if (previous != null) {
                            assertSame(previous, variable.getEnclosingElement());
                        }
                        ordinaryLocals.incrementAndGet();
                    }
                    if (List.of("x", "nested", "item").contains(tree.getName().toString())) {
                        assertEquals(ElementKind.BINDING_VARIABLE, element.getKind());
                        assertEquals(1, element.getAnnotationMirrors().size());
                    }
                    return super.visitVariable(tree, unused);
                }

                @Override
                public Void visitAnnotation(AnnotationTree tree, Void unused) {
                    annotationVisits.merge(tree, 1, Integer::sum);
                    return super.visitAnnotation(tree, unused);
                }
            }.scan(units, null);
            assertEquals(14, owners.get());
            assertEquals(7, ordinaryLocals.get());
            assertFalse(annotationVisits.isEmpty());
            annotationVisits.forEach((tree, count) -> assertEquals(1, count, tree.toString()));
        }
    }

    @Test
    public void annotationProcessing() throws Exception {
        AtomicInteger declarations = new AtomicInteger();
        AtomicInteger ordinaryLocals = new AtomicInteger();
        var processor = new AbstractProcessor() {
            @Override
            public Set<String> getSupportedAnnotationTypes() {
                return Set.of("*");
            }

            @Override
            public SourceVersion getSupportedSourceVersion() {
                return SourceVersion.latestSupported();
            }

            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
                if (round.processingOver()) {
                    return false;
                }
                Trees trees = Trees.instance(processingEnv);
                for (Element root : round.getRootElements()) {
                    new TreePathScanner<Void, Void>() {
                        void check(TreePath path, Elements.Origin origin) {
                            var variable = (VariableElement) trees.getElement(path);
                            assertNotNull(variable);
                            assertEquals(ElementKind.LOCAL_VARIABLE, variable.getKind());
                            assertEquals(origin, processingEnv.getElementUtils().getOrigin(variable));
                            assertTrue(processingEnv.getTypeUtils().isSameType(variable.asType(),
                                    trees.getTypeMirror(path)));
                            assertSame(path.getLeaf(), trees.getTree(variable));
                            assertSame(path.getLeaf(), trees.getPath(variable).getLeaf());
                            assertEquals(1, variable.getAnnotationMirrors().size());
                            assertEquals(variable.getAnnotationMirrors(),
                                    processingEnv.getElementUtils().getAllAnnotationMirrors(variable));
                            var annotation = variable.getAnnotationMirrors().getFirst();
                            assertSame(trees.getTree(variable, annotation),
                                    trees.getPath(variable, annotation).getLeaf());
                            assertEquals(Tree.Kind.ANNOTATION, trees.getTree(variable, annotation).getKind());
                            for (var value : annotation.getElementValues().values()) {
                                assertEquals(Tree.Kind.INT_LITERAL,
                                        trees.getTree(variable, annotation, value).getKind());
                                assertSame(trees.getTree(variable, annotation, value),
                                        trees.getPath(variable, annotation, value).getLeaf());
                            }
                        }

                        @Override
                        public Void visitVariable(VariableTree tree, Void unused) {
                            if (tree.getKind() == Tree.Kind.ENHANCED_VARIABLE) {
                                check(getCurrentPath(), Elements.Origin.MANDATED);
                                assertSame(trees.getElement(getCurrentPath()),
                                        trees.getElement(new TreePath(getCurrentPath(), tree.getPattern())));
                                declarations.incrementAndGet();
                                return super.visitVariable(tree, unused);
                            }
                            Element element = trees.getElement(getCurrentPath());
                            if (element != null && element.getKind() == ElementKind.LOCAL_VARIABLE) {
                                check(getCurrentPath(), Elements.Origin.EXPLICIT);
                                ordinaryLocals.incrementAndGet();
                            }
                            return super.visitVariable(tree, unused);
                        }
                    }.scan(trees.getPath(root), null);
                }
                TypeElement local = processingEnv.getElementUtils().getTypeElement("Test.Local");
                assertTrue(round.getElementsAnnotatedWith(local).isEmpty());
                return false;
            }
        };
        Path out = Files.createTempDirectory(Path.of("."), "processing");
        new JavacTask(tb).options("--enable-preview", "--source", Integer.toString(Runtime.version().feature()),
                        "-XDrawDiagnostics")
                .outdir(out).processors(processor).sources(SOURCE_PREFIX + """
                        void test(List<Box<String>> rows) {
                            @Local(9) Box<String> named = new Box<>("named");
                            @Local(11) Box<String> _ = new Box<>("unnamed");
                            @Local(7) Box<String>(var value) = new Box<>("enhanced");
                            for (@Local(13) Box<String>(var item) : rows) {}
                        }
                        }
                        """).run();
        assertEquals(2, declarations.get());
        assertEquals(2, ordinaryLocals.get());
    }
}
