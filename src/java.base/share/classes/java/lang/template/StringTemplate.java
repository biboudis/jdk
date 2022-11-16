/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.template;

import java.util.List;
import java.util.Objects;

import jdk.internal.javac.PreviewFeature;

/**
 * {@link StringTemplate StringTemplates} is the run-time representation of a
 * string template or text block template in a template expression.
 * <p>
 * In the source code of a Java program, a string template or text block template
 * contains an interleaved succession of <em>fragment literals</em> and <em>embedded
 * expressions</em>. The {@link StringTemplate#fragments()} method returns the
 * fragment literals, and the {@link StringTemplate#values()} method returns the
 * results of evaluating the embedded expressions. {@link StringTemplate} does not
 * provide access to the source code of the embedded expressions themselves; it is
 * not a compile-time representation of a string template or text block template.
 * <p>
 * {@link StringTemplate} is primarily used in conjunction with a template processor
 * to produce a string or other meaningful value. Evaluation of a template expression
 * first produces an instance of {@link StringTemplate}, representing the template
 * of the template expression, and then passes the instance to the template processor
 * given by the template expression.
 * <p>
 * For example, the following code contains a template expression that uses the template
 * processor {@code RAW}, which simply yields the {@link StringTemplate} passed to it:
 * {@snippet :
 * int x = 10;
 * int y = 20;
 * StringTemplate st = RAW."\{x} + \{y} = \{x + y}";
 * List<String> fragments = st.fragments();
 * List<Object> values = st.values();
 * }
 * {@code fragments} will be equivalent to {@code List.of("", " + ", " = ", "")}
 * and {@code values} will be the equivalent of {@code List.of(10, 20, 30)}.
 * <p>
 * The following code contains a template expression with the same template but a
 * different template processor:
 * {@snippet :
 * int x = 10;
 * int y = 20;
 * String s = STR."\{x} + \{y} = \{x + y}";
 * }
 * When the template expression is evaluated, an instance of {@link StringTemplate} is
 * produced that returns the same lists from {@link StringTemplate#fragments()} and
 * {@link StringTemplate#values()} as shown above. The {@code STR} template processor
 * uses these lists to yield an interpolated string. {@code s} will be equivalent to
 * {@code "10 + 20 = 30"}.
 * <p>
 * The {@code interpolate()} method provides a direct way to perform string interpolation
 * of a {@link StringTemplate}. Template processors can use the following code pattern:
 * {@snippet :
 * List<String> fragments = st.fragments();
 * List<Object> values    = st.values();
 * ... check or manipulate the fragments and/or values ...
 * String result = StringTemplate.interpolate(fragments, values);
 * }
 * The {@link StringTemplate#process(ValidatingProcessor)} method provides an
 * alternative to using string template expressions.
 * {@snippet :
 * StringTemplate st = RAW."\{x} + \{y} = \{x + y}";
 * String result = st.process(STR);
 * }
 * The factory methods {@link StringTemplate#of(String)} and
 * {@link StringTemplate#of(List, List)} can be used to construct a {@link StringTemplate}.
 *
 * @jls 15.8.6
 *
 * @see ValidatingProcessor
 * @see TemplateProcessor
 * @see StringProcessor
 * @see java.util.FormatProcessor
 *
 * @implNote Libraries may produce {@link StringTemplate} instances as long as they
 * conform to the requirements of this interface.
 * Implementations of {@link StringTemplate} must minimally implement the
 * methods {@link StringTemplate#fragments()} and {@link StringTemplate#values()}.
 * Instances of {@link StringTemplate} are considered immutable. To preserve the
 * semantics of string templates and text block templates, the list returned by
 * {@link StringTemplate#fragments()} must be one element larger than the list returned
 * by {@link StringTemplate#values()}.
 *
 * @since 20
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
public interface StringTemplate {
    /**
     * Returns this {@link StringTemplate StringTemplate's} immutable list of string
     * fragments consisting of the string of characters preceding each of the embedded
     * expressions plus the string of characters following the last embedded expression.
     * In the example: {@snippet :
     * String student = "Mary";
     * String teacher = "Johnson";
     * StringTemplate st = RAW."The student \{student} is in \{teacher}'s class room.";
     * List<String> fragments = st.fragments(); // @highlight substring="fragments()"
     * }
     * {@code fragments} will be equivalent to
     * {@code List.of("The student ", " is in ", "'s class room.")}
     *
     * @return list of string fragments
     *
     * @implSpec The list returned is immutable.
     */
    List<String> fragments();

    /**
     * Returns this {@link StringTemplate StringTemplate's} immutable list of embedded
     * expression results. In the example:
     * {@snippet :
     * String student = "Mary";
     * String teacher = "Johnson";
     * StringTemplate st = RAW."The student \{student} is in \{teacher}'s class room.";
     * List<Object> values = st.values(); // @highlight substring="values()"
     * }
     * {@code values} will be equivalent to {@code List.of(student, teacher)}
     *
     * @return list of expression values
     *
     * @implSpec The list returned is immutable.
     */
    List<Object> values();

    /**
     * Returns the interpolation of this {@link StringTemplate StringTemplate's}.
     * fragments and values.
     *
     * {@snippet :
     * String student = "Mary";
     * String teacher = "Johnson";
     * StringTemplate st = RAW."The student \{student} is in \{teacher}'s class room.";
     * String string = st.interpolation(); // @highlight substring="interpolation()"
     * }
     * {@code string} will be equivalent to {@code "The student Mary is in Johnson's class room."}
     *
     * @return interpolation of this {@link StringTemplate}
     */
    default String interpolate() {
        return StringTemplate.interpolate(fragments(), values());
    }

    /**
     * Returns the result of applying the specified processor to this {@link StringTemplate}.
     * This method can be used as an alternative to string template expressions. For example,
     * {@snippet :
     * String student = "Mary";
     * String teacher = "Johnson";
     * String result1 = STR."The student \{student} is in \{teacher}'s class room.";
     * String result2 = RAW."The student \{student} is in \{teacher}'s class room.".process(STR); // @highlight substring="process"
     * }
     * produces an equivalent result for both {@code result1} and {@code result2}.
     *
     * @param processor the {@link ValidatingProcessor} instance to process
     *
     * @param <R>  Processor's process result type.
     * @param <E>  Exception thrown type.
     *
     * @return constructed object of type {@code R}
     *
     * @throws E exception thrown by the template processor when validation fails
     * @throws NullPointerException if processor is null
     *
     * @implNote The default implementation invokes the processor's process
     * method {@code processor.process(this)}.
     */
    default <R, E extends Throwable> R
    process(ValidatingProcessor<? extends R, ? extends E> processor) throws E {
        Objects.requireNonNull(processor, "processor should not be null");

        return processor.process(this);
    }

    /**
     * Produces a diagnostic string representing the supplied
     * {@link StringTemplate}.
     *
     * @param stringTemplate  the {@link StringTemplate} to represent
     *
     * @return diagnostic string representing the supplied templated string
     *
     * @throws NullPointerException if stringTemplate is null
     */
    static String toString(StringTemplate stringTemplate) {
        Objects.requireNonNull(stringTemplate, "stringTemplate should not be null");
        return "StringTemplate{ fragments = [ \"" +
                String.join("\", \"", stringTemplate.fragments()) +
                "\" ], values = " +
                stringTemplate.values() +
                " }";
    }

    /**
     * Returns a StringTemplate as if constructed by invoking
     * {@code StringTemplate.of(List.of(string), List.of())}.
     *
     * @param string  single string fragment
     *
     * @return StringTemplate composed from string
     *
     * @throws NullPointerException if string is null
     */
    static StringTemplate of(String string) {
        Objects.requireNonNull(string, "string must not be null");
        return TemplateSupport.of(List.of(string), List.of());
    }

    /**
     * Returns a StringTemplate with the given fragments and values.
     *
     * @implSpec The {@code fragments} list size must be one more that the
     * {@code values} list size.
     *
     * @param fragments list of string fragments
     * @param values    list of expression values
     *
     * @return StringTemplate composed from string
     *
     * @throws IllegalArgumentException if fragments list size is not one more
     *         than values list size
     * @throws NullPointerException if fragments is null or values is null or if any fragment is null.
     *
     * @implNote Contents of both lists are copied to construct immutable lists.
     */
    static StringTemplate of(List<String> fragments, List<?> values) {
        Objects.requireNonNull(fragments, "fragments must not be null");
        Objects.requireNonNull(values, "values must not be null");
        if (values.size() + 1 != fragments.size()) {
            throw new IllegalArgumentException(
                    "fragments list size is not one more than values list size");
        }
        return TemplateSupport.of(fragments, values);
    }

    /**
     * Creates a string that interleaves the elements of values between the
     * elements of fragments.
     *
     * @param fragments  list of String fragments
     * @param values     list of expression values
     *
     * @return String interpolation of fragments and values
     *
     * @throws IllegalArgumentException if fragments list size is not one more
     *         than values list size
     * @throws NullPointerException fragments or values is null or if any of the fragments is null
     */
    static String interpolate(List<String> fragments, List<?> values) {
        Objects.requireNonNull(fragments, "fragments must not be null");
        Objects.requireNonNull(values, "values must not be null");
        int fragmentsSize = fragments.size();
        int valuesSize = values.size();
        if (fragmentsSize != valuesSize + 1) {
            throw new IllegalArgumentException("fragments must have one more element than values");
        }
        return TemplateSupport.interpolate(fragments, values);
    }

    /**
     * Combine one or more {@link StringTemplate StringTemplates} into a single {@link StringTemplate}.
     * {@snippet :
     * StringTemplate st = StringTemplate.combine(RAW."\{a}", RAW."\{b}", RAW."\{c}");
     * assert st.interpolate().equals(RAW."\{a}\{b}\{c}");
     * }
     * Fragment lists from each {@link StringTemplate} are merged such that the last fragment
     * from the previous {@link StringTemplate} is concatenated with the first fragment of the next
     * {@link StringTemplate}. Values lists are simply concatenated to produce a single values list.
     *
     * @param sts  one or more {@link StringTemplate}
     *
     * @return combined {@link StringTemplate}
     *
     * @throws NullPointerException if sts is null or if any of the elements are null
     * @throws RuntimeException if sts has zero elements
     */
    static StringTemplate combine(StringTemplate... sts) {
        return TemplateSupport.combine(sts);
    }

    /**
     * Interpolation template processor instance.
     * Example: {@snippet :
     * int x = 10;
     * int y = 20;
     * String result = STR."\{x} + \{y} = \{x + y}"; // @highlight substring="STR"
     * }
     * @implNote The result of interpolation is not interned.
     */
    static final StringProcessor STR = StringTemplate::interpolate;

    /**
     * No-op template processor. Used to highlight that non-processing of the StringTemplate
     * was intentional.
     * {@snippet :
     * // The string template before interpolation
     * System.out.println(RAW."\{x} = \{y} = \{x + y}"); // @highlight substring="RAW"
     * // The string template after interpolation
     * System.out.println(STR."\{x} = \{y} = \{x + y}"); // @highlight substring="STR"
     * }
     */
    static final TemplateProcessor<StringTemplate> RAW = st -> st;

}
