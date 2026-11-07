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
 * @summary Check that JShell preserves annotations when parsing enhanced local variable declarations
 * @enablePreview
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jshell/jdk.jshell:open
 * @run junit EnhancedVariableDeclParserAnnotationsTest
 */

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.net.URI;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnhancedVariableDeclParserAnnotationsTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "@A(7) @B Box<String>(var value) = rhs;",
        "@A(7) @B Box(var value) = rhs;",
        "@A(7) @B Box<String>(_) = rhs;",
        "@A(7) @B Box<String>(var _) = rhs;",
        "@A(7) @B Box<String>(String value) = rhs;",
        "@A(7) @B Box<String> value = rhs;",
        "for (@A(7) @B Box<String>(var value) : rows) {}"
    })
    public void preservesDeclarationAnnotations(String source) throws Exception {
        var annotations = parseModifiers(source).getAnnotations();
        assertEquals(List.of("A", "B"), annotations.stream()
                .map(annotation -> annotation.getAnnotationType().toString())
                .toList());
        assertEquals(1, annotations.getFirst().getArguments().size());
        assertEquals("7", annotations.getFirst().getArguments().getFirst().toString());
        assertEquals(0, annotations.getLast().getArguments().size());
    }

    private JCModifiers parseModifiers(String source) throws Exception {
        var context = new Context();
        try (var fileManager = new JavacFileManager(context, true, null)) {
            var file = new SimpleJavaFileObject(URI.create("string:///Snippet.java"),
                    JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            var log = Log.instance(context);
            log.useSource(file);
            var options = Options.instance(context);
            options.put("--source", Integer.toString(Runtime.version().feature()));
            options.put("--enable-preview", "true");
            var register = Class.forName("jdk.jshell.ReplParserFactory")
                    .getDeclaredMethod("preRegister", Context.class, boolean.class);
            register.setAccessible(true);
            register.invoke(null, context, false);
            var parser = ParserFactory.instance(context).newParser(source, false, true, false);
            var unit = parser.parseCompilationUnit();
            assertEquals(0, log.nerrors, source);
            assertEquals(1, unit.defs.size(), source);
            return switch (unit.defs.head) {
                case JCEnhancedForLoop loop -> loop.getVariable().getModifiers();
                case JCVariableDecl variable -> variable.getModifiers();
                default -> throw new AssertionError(unit.defs.head.getTag());
            };
        }
    }
}
