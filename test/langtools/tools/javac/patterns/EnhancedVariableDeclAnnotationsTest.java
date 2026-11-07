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
 * @summary Check annotations on enhanced local declarations and component variables in testing contexts
 * @enablePreview
 * @compile -Xlint:rawtypes -Werror EnhancedVariableDeclAnnotationsTest.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.util.List;

public class EnhancedVariableDeclAnnotationsTest {
    @Target(ElementType.LOCAL_VARIABLE)
    @interface Local {}
    @Target(ElementType.TYPE_USE)
    @interface Use {}
    @Target({ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @interface Both {}
    @Target(ElementType.LOCAL_VARIABLE) @Repeatable(Locals.class)
    @interface Repeat {}
    @Target(ElementType.LOCAL_VARIABLE)
    @interface Locals { Repeat[] value(); }

    record Box<T>(T value) {}
    static <T> Box<T> empty() { return new Box<>(null); }
    static <T> T read() { return null; }

    static void declarations() {
        @Local Box(var x) = new Box<>("x");
        String a = x;
        @Use Box(var y) = new Box<>("y");
        String b = y;
        @Local Box<String>(var z) = empty();
        String c = z;
        @Both Box<?>(var w) = read();
        Object d = w;
        @Local Box<Box<String>>(Box(@Local var nested)) = new Box<>(new Box<>("n"));
        String e = nested;
        @Repeat @Repeat Box<String>(_) = new Box<>("r");
        @Local Box(@Local var _) = new Box<>("u");
        @Local @Use Box<String>(var combined) = new Box<>("both");
    }

    static void enhancedFor(List<Box<String>> rows, Box<String>[] array) {
        for (@Use Box<String>(var value) : rows) {
            String s = value;
        }
        for (@Local Box<String>(_) : array) {}
    }

    static void arrayTypeArguments() {
        @Use Box<@Use String @Use []>(var annotated) = new Box<>(new String[] {"annotated"});
        String text = annotated[0];
        @Use Box<@Use int @Use [] @Use []>(var dimensions) = new Box<>(new int[][] {{23}});
        int number = dimensions[0][0];
        Box<java.lang.@Use String @Use []>(var qualified) = new Box<>(new String[] {"qualified"});
        String name = qualified[0];
    }

    static void instanceOf(Object o) {
        if (o instanceof Box<?>(@Local var x)) {}
        if (o instanceof Box<?>(@Use Object x)) {}
        if (o instanceof Box<?>(@Local @Use Object x)) {}
        if (o instanceof Box<?>(Box<?>(@Local var x))) {}
    }

    static void switchStatement(Object o) {
        switch (o) {
            case Box<?>(@Local var x) -> {}
            default -> {}
        }
        switch (o) {
            case Box<?>(@Use Object x) -> {}
            default -> {}
        }
        switch (o) {
            case Box<?>(@Local @Use Object x) -> {}
            default -> {}
        }
        switch (o) {
            case Box<?>(Box<?>(@Local var x)) -> {}
            default -> {}
        }
    }

    static void switchExpression(Object o) {
        int local = switch (o) {
            case Box<?>(@Local var x) -> 1;
            default -> 0;
        };
        int typeUse = switch (o) {
            case Box<?>(@Use Object x) -> 1;
            default -> 0;
        };
        int both = switch (o) {
            case Box<?>(@Local @Use Object x) -> 1;
            default -> 0;
        };
        int nested = switch (o) {
            case Box<?>(Box<?>(@Local var x)) -> 1;
            default -> 0;
        };
    }
}
