/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.runtime;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Objects;

import jdk.internal.javac.PreviewFeature;

/**
 * This class implements specialized {@link StringTemplate StringTemplates} produced by
 * string template bootstrap method callsites generated by the compiler. Instances of this
 * class are produced by {@link StringTemplateImplFactory}.
 * <p>
 * Values are stored by subclassing {@link Carriers.CarrierObject}. This allows specializations
 * and sharing of value shapes without creating a new class for each shape.
 * <p>
 * {@link StringTemplate} fragments are shared via binding to the
 * {@link java.lang.invoke.CallSite CallSite's} {@link MethodHandle}.
 * <p>
 * The {@link StringTemplateImpl} instance also carries
 * specialized {@link MethodHandle MethodHandles} for producing the values list and interpolation.
 * These {@link MethodHandle MethodHandles} are  also shared by binding to the
 * {@link java.lang.invoke.CallSite CallSite}.
 *
 * @since 21
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
final class StringTemplateImpl extends Carriers.CarrierObject implements StringTemplate {
    /**
     * List of string fragments for the string template. This value of this list is shared by
     * all instances created at the {@link java.lang.invoke.CallSite CallSite}.
     */
    private final List<String> fragments;

    /**
     * Specialized {@link MethodHandle} used to implement the {@link StringTemplate StringTemplate's}
     * {@code values} method. This {@link MethodHandle} is shared by all instances created at the
     * {@link java.lang.invoke.CallSite CallSite}.
     */
    private final MethodHandle valuesMH;

    /**
     * Specialized {@link MethodHandle} used to implement the {@link StringTemplate StringTemplate's}
     * {@code interpolate} method. This {@link MethodHandle} is shared by all instances created at the
     * {@link java.lang.invoke.CallSite CallSite}.
     */
    private final MethodHandle interpolateMH;

    /**
     * Constructor.
     *
     * @param primitiveCount  number of primitive slots required (bound at callsite)
     * @param objectCount     number of object slots required (bound at callsite)
     * @param fragments       list of string fragments (bound in (bound at callsite)
     * @param valuesMH        {@link MethodHandle} to produce list of values (bound at callsite)
     * @param interpolateMH   {@link MethodHandle} to produce interpolation (bound at callsite)
     */
    StringTemplateImpl(int primitiveCount, int objectCount,
                       List<String> fragments, MethodHandle valuesMH, MethodHandle interpolateMH) {
        super(primitiveCount, objectCount);
        this.fragments = fragments;
        this.valuesMH = valuesMH;
        this.interpolateMH = interpolateMH;
    }

    @Override
    public List<String> fragments() {
        return fragments;
    }

    @Override
    public List<Object> values() {
        try {
            return (List<Object>)valuesMH.invokeExact(this);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException("string template values failure", ex);
        }
    }

    @Override
    public String interpolate() {
        try {
            return (String)interpolateMH.invokeExact(this);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException("string template interpolate failure", ex);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StringTemplate st &&
                Objects.equals(fragments(), st.fragments()) &&
                Objects.equals(values(), st.values());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fragments(), values());
    }

    @Override
    public String toString() {
        return StringTemplate.toString(this);
    }
}
