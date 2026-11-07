/*
 * Copyright (c) 2021, 2026, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.tree.CaseLabelTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

public class DisambiguatePatterns {

    public static void main(String... args) throws Throwable {
        DisambiguatePatterns test = new DisambiguatePatterns();
        test.disambiguationTest("String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("String s when s.isEmpty()",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("@Ann String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("(String) s",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("((String) s)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("((0x1))",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a > b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a >> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a >>> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a < b | a > b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a << b | a >> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a << b || a < b | a >>> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a < c.d > b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? extends c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("@Ann a<? extends c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? extends @Ann c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? super c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? super @Ann c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c.d>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<@Ann c.d>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c<d>>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[] b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[][] b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("int i",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("int[] i",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[a]",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a[b][c]",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a & b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("R r when (x > 0)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("R(int x) when (x > 0)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("java.util.List<?>[] p",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("java.util.List<?>[][] p",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c>>[] d",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("java.util.List<?>[] p when true",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("java.util.List<?> @Ann [] p",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c>> @Ann [] d",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("(java.util.List<?>[]) o",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("String[].class",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("new int[1][]",
                                ExpressionType.EXPRESSION);
        test.disambiguationTest("new java.util.List<?>[1][]",
                                ExpressionType.EXPRESSION);

        // Declarations without a record pattern and expressions in for headers.
        test.forDisambiguationTest("T[] a",
                                 ForType.ENHANCED_FOR_WITH_LVDS);
        test.forDisambiguationTest("@Annot(field = \"test\") Point p",
                                 ForType.ENHANCED_FOR_WITH_LVDS);
        test.forDisambiguationTest("Box<String[]> box",
                                 ForType.ENHANCED_FOR_WITH_LVDS);
        test.forDisambiguationTest("List<List<String[]>> rows",
                                 ForType.ENHANCED_FOR_WITH_LVDS);
        test.forDisambiguationTest("String @Ann [] @Ann [] xs",
                                 ForType.ENHANCED_FOR_WITH_LVDS);
        test.forDisambiguationTest("T[].class.getName()",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("method()",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("method(), method()",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("method2((Integer a) -> 42)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("m(cond ? b() : i)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("m((GPoint<?>)null, cond ? b() : i)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("Box<String[]> box = rhs",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("List<List<String[]>> rows = rhs",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("String @Ann [] @Ann [] xs = rhs",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(A.<byte[]>m())",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(A.<B, byte[]>m(b, bytes))",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(A.<B[]>m())",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(b.<byte[]>m(c), d)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(A.<byte[], short[][]>m())",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(A.<List<String[]>>m())",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(A.<List<List<String[]>>>m())",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(A<String[], short[][]>::method)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(ArrayList<String[]>::new)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(String[][].class)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(int[][][].class)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(String[][]::new)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("String[][].class.getName()",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(x < y)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(x < y, String[][].class)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(x < y, A.<String[]>m())",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(x > y)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(x >> 1)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(x >>> 1)",
                                 ForType.TRADITIONAL_FOR);
        test.forDisambiguationTest("a.m(x < y ? left() : right())",
                                 ForType.TRADITIONAL_FOR);

        // Declarations with a record pattern in statements and enhanced for headers.
        test.recordDeclarationDisambiguationTest("Empty()");
        test.recordDeclarationDisambiguationTest("Point(int x, int y)");
        test.recordDeclarationDisambiguationTest("Point(Integer a, Integer b)");
        test.recordDeclarationDisambiguationTest("Point(var a, var b)");
        test.recordDeclarationDisambiguationTest("Point(_, var y)");
        test.recordDeclarationDisambiguationTest("R(T[] a)");
        test.recordDeclarationDisambiguationTest("ForEachPatterns.Point(Integer a, Integer b)");
        test.recordDeclarationDisambiguationTest("GPoint<Integer>(Integer a, Integer b)");
        test.recordDeclarationDisambiguationTest("GPoint<Point>(Point(Integer a, Integer b), Point c)");
        test.recordDeclarationDisambiguationTest("GPoint<Point>(Point(var a, Integer b), Point c)");
        test.recordDeclarationDisambiguationTest("GPoint<Point>(Point(@Ann Integer a, @Ann Integer b), @Ann Point c)");
        test.recordDeclarationDisambiguationTest("GPoint<VoidPoint>(VoidPoint(), VoidPoint())");
        test.recordDeclarationDisambiguationTest("RecordOfLists(List<Integer> lr)");
        test.recordDeclarationDisambiguationTest("RecordOfLists2(List<List<Integer>> lr)");
        test.recordDeclarationDisambiguationTest("Box<String[]>(var xs)");
        test.recordDeclarationDisambiguationTest("Box<String[]>(String[] xs)");
        test.recordDeclarationDisambiguationTest("Box<String[]>(_)");
        test.recordDeclarationDisambiguationTest("Box<String[]>(var _)");
        test.recordDeclarationDisambiguationTest("Box<String[]>(String[] _)");
        test.recordDeclarationDisambiguationTest("Pair<int[], String[][]>(var a, var b)");
        test.recordDeclarationDisambiguationTest("Box<List<String>[]>(var values)");
        test.recordDeclarationDisambiguationTest("Box<List<String[]>>(var xs)");
        test.recordDeclarationDisambiguationTest("Box<List<List<String[]>>>(var xs)");
        test.recordDeclarationDisambiguationTest("Box<List<List<List<String[]>>>>(var xs)");
        test.recordDeclarationDisambiguationTest("Outer(Pair<String[], Integer>(var a, var b))");
        test.recordDeclarationDisambiguationTest("Outer(Box<String[]>(var xs))");
        test.recordDeclarationDisambiguationTest("Box<? extends String[][]>(var xs)");
        test.recordDeclarationDisambiguationTest("Box<? super String[]>(var value)");
        test.recordDeclarationDisambiguationTest("pkg.Box<java.util.List<? extends java.lang.String[][]>>(var xs)");
        test.recordDeclarationDisambiguationTest("pkg.@Ann Box<String>(var value)");
        test.recordDeclarationDisambiguationTest("@Ann Box<String[]>(var xs)");
        test.recordDeclarationDisambiguationTest("@Ann(value = {1, 2}) @Other Box<?>(var value)");
        test.recordDeclarationDisambiguationTest("Box<@Ann String @Ann [] @Ann []>(var xs)");
        test.recordDeclarationDisambiguationTest("Box<@Ann(value = {1, 2}) String @Ann []>(var xs)");
        test.recordDeclarationDisambiguationTest("Pair<String[], @Ann ? extends List<int[][]>>(var a, var b)");

        // Declarations without a record pattern and expression statements.
        test.statementDisambiguationTest("Point p = p0",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("@Ann Point p = p0",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("GPoint<Integer> gp = g",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("T[] a = arr",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("int i = 0",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("var v = method()",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("Box<String[]> box = rhs",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("List<List<String[]>> rows = rhs",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("String @Ann [] @Ann [] xs = rhs",
                                 BlockStatementType.LOCAL_VARIABLE_DECLARATION);
        test.statementDisambiguationTest("a.m(A.<byte[]>m())",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(A.<B, byte[]>m(b, bytes))",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(A.<B[]>m())",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(b.<byte[]>m(c), d)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(A.<byte[], short[][]>m())",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(A.<List<String[]>>m())",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(A.<List<List<String[]>>>m())",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(A<String[], short[][]>::method)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(ArrayList<String[]>::new)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(String[][].class)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(int[][][].class)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(String[][]::new)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("String[][].class.getName()",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(x < y)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(x < y, String[][].class)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(x < y, A.<String[]>m())",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(x > y)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(x >> 1)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(x >>> 1)",
                                 BlockStatementType.EXPRESSION_STATEMENT);
        test.statementDisambiguationTest("a.m(x < y ? left() : right())",
                                 BlockStatementType.EXPRESSION_STATEMENT);
    }

    private final ParserFactory factory;

    public DisambiguatePatterns() throws URISyntaxException {
        Context context = new Context();
        JavacFileManager jfm = new JavacFileManager(context, true, Charset.defaultCharset());
        Options.instance(context).put(Option.PREVIEW, "");
        SimpleJavaFileObject source =
                new SimpleJavaFileObject(new URI("mem://Test.java"), JavaFileObject.Kind.SOURCE) {};
        Log.instance(context).useSource(source);
        factory = ParserFactory.instance(context);
    }

    void disambiguationTest(String snippet, ExpressionType expectedType) {
        String code = """
                      public class Test {
                          private void test() {
                              switch (null) {
                                  case SNIPPET -> {}
                              }
                          }
                      }
                      """.replace("SNIPPET", snippet);
        JavacParser parser = factory.newParser(code, false, false, false);
        CompilationUnitTree result = parser.parseCompilationUnit();
        ClassTree clazz = (ClassTree) result.getTypeDecls().get(0);
        MethodTree method = (MethodTree) clazz.getMembers().get(0);
        SwitchTree st = (SwitchTree) method.getBody().getStatements().get(0);
        CaseLabelTree label = st.getCases().get(0).getLabels().get(0);
        ExpressionType actualType = switch (label) {
            case ConstantCaseLabelTree et -> ExpressionType.EXPRESSION;
            case PatternCaseLabelTree pt -> ExpressionType.PATTERN;
            default -> throw new AssertionError("Unexpected result: " + result);
        };
        if (expectedType != actualType) {
            throw new AssertionError("Expected: " + expectedType + ", actual: " + actualType +
                                      ", for: " + code + ", parsed: " + result);
        }
    }

    void recordDeclarationDisambiguationTest(String pattern) {
        forDisambiguationTest(pattern, ForType.ENHANCED_FOR_WITH_PATTERNS_WITH_ELVDS);
        statementDisambiguationTest(pattern + " = rhs", BlockStatementType.ENHANCED_LOCAL_VARIABLE_DECLARATION);
    }

    void forDisambiguationTest(String snippet, ForType forType) {
        String codeTemplate = switch (forType) {
            case TRADITIONAL_FOR ->
                    """
                    public class Test {
                        private void test() {
                            for (SNIPPET; ;) {
                            }
                        }
                    }
                    """;
            case ENHANCED_FOR_WITH_LVDS, ENHANCED_FOR_WITH_PATTERNS_WITH_ELVDS ->
                    """
                    public class Test {
                        private void test() {
                            for (SNIPPET : collection) {
                            }
                        }
                    }
                    """;
        };

        String code = codeTemplate.replace("SNIPPET", snippet);
        JavacParser parser = factory.newParser(code, false, false, false);
        CompilationUnitTree result = parser.parseCompilationUnit();
        ClassTree clazz = (ClassTree) result.getTypeDecls().get(0);
        MethodTree method = (MethodTree) clazz.getMembers().get(0);
        StatementTree st = method.getBody().getStatements().get(0);
        if (forType == ForType.TRADITIONAL_FOR) {
            if (st.getKind() != Kind.FOR_LOOP) {
                throw new AssertionError("Unpected statement: " + st);
            }
        } else {
            EnhancedForLoopTree ef = (EnhancedForLoopTree) st;
            ForType actualType = ef.getVariable().getKind() == Kind.ENHANCED_VARIABLE
                    ? ForType.ENHANCED_FOR_WITH_PATTERNS_WITH_ELVDS
                    : ForType.ENHANCED_FOR_WITH_LVDS;
            if (forType != actualType) {
                throw new AssertionError("Expected: " + forType + ", actual: " + actualType +
                        ", for: " + code + ", parsed: " + result);
            }
        }
    }

    void statementDisambiguationTest(String snippet, BlockStatementType expectedType, String... expectedErrors) {
        String code = """
                      public class Test {
                          private void test() {
                              SNIPPET;
                          }
                      }
                      """.replace("SNIPPET", snippet);
        JavacParser parser = factory.newParser(code, false, false, false);
        CompilationUnitTree result = parser.parseCompilationUnit();
        ClassTree clazz = (ClassTree) result.getTypeDecls().get(0);
        MethodTree method = (MethodTree) clazz.getMembers().get(0);
        StatementTree st = method.getBody().getStatements().get(0);
        switch (expectedType) {
            case ENHANCED_LOCAL_VARIABLE_DECLARATION -> {
                if (!(st instanceof JCVariableDecl)) {
                    throw new AssertionError("Expected pattern-based JCVariableDecl, got: " + st.getClass() +
                            ", for: " + code + ", parsed: " + result);
                }
                if (st.getKind() != Kind.ENHANCED_VARIABLE) {
                    throw new AssertionError("Expected kind ENHANCED_VARIABLE, got: " + st.getKind() +
                            ", for: " + code + ", parsed: " + result);
                }
            }
            case LOCAL_VARIABLE_DECLARATION -> {
                if (!(st instanceof JCVariableDecl)) {
                    throw new AssertionError("Expected JCVariableDecl, got: " + st.getClass() +
                            ", for: " + code + ", parsed: " + result);
                }
                if (st.getKind() != Kind.VARIABLE) {
                    throw new AssertionError("Expected kind VARIABLE, got: " + st.getKind() +
                            ", for: " + code + ", parsed: " + result);
                }
            }
            case EXPRESSION_STATEMENT -> {
                if (st.getKind() != Kind.EXPRESSION_STATEMENT) {
                    throw new AssertionError("Expected expression statement, got: " + st.getKind() +
                            ", for: " + code + ", parsed: " + result);
                }
            }
        }
    }

    enum ForType {
        TRADITIONAL_FOR,
        ENHANCED_FOR_WITH_LVDS,
        ENHANCED_FOR_WITH_PATTERNS_WITH_ELVDS;
    }

    enum ExpressionType {
        PATTERN,
        EXPRESSION;
    }

    enum BlockStatementType {
        LOCAL_VARIABLE_DECLARATION,
        ENHANCED_LOCAL_VARIABLE_DECLARATION,
        EXPRESSION_STATEMENT;
    }
}
