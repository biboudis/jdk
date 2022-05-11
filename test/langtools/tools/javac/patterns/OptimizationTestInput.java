/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Use for optimization (should reduce the number of checks)
 * @compile --enable-preview -source ${jdk.version} OptimizationTestInput.java
 */
public class OptimizationTestInput {

    record Outer(R r1, R r2) { };
    sealed interface I { };
    final class A implements I { };
    final class B implements I { };
    sealed interface R { };
    record Foo(I i) implements R { }
    record Bar(I i) implements R { }

    void test(Outer o) {
        switch (o) {
            case Outer(Foo(A x1), Foo(A y1)) -> { }
            case Outer(Foo(A x3), Foo(B y3)) -> { }
            case Outer(Foo(A x5), Bar(A y5)) -> { }
            case Outer(Foo(A x7), Bar(B y7)) -> { }

            case Outer(Foo(B x2), Foo(B y2)) -> { }
            case Outer(Foo(B x4), Foo(A y4)) -> { }
            case Outer(Foo(B x6), Bar(B y6)) -> { }
            case Outer(Foo(B x8), Bar(A y8)) -> { }

            case Outer(Bar(A xx1), Bar(A yy1)) -> { }
            case Outer(Bar(B xx2), Bar(B yy2)) -> { }
            case Outer(Bar(A xx3), Bar(B yy3)) -> { }
            case Outer(Bar(B xx4), Bar(A yy4)) -> { }

            case Outer(Bar(A xx5), Foo(A yy5)) -> { }
            case Outer(Bar(B xx6), Foo(B yy6)) -> { }
            case Outer(Bar(A xx7), Foo(B yy7)) -> { }
            case Outer(Bar(B xx8), Foo(A yy8)) -> { }
        }
    }
}
