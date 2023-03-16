/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.io.IOException;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Matcher_attribute extends Attribute {
    Matcher_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);
        pattern_flags = cr.readUnsignedShort();
        pattern_name_index = cr.readUnsignedShort();
        pattern_descriptor = new Descriptor(cr);
        attributes = new Attributes(cr);
    }

    public Matcher_attribute(int name_index, int pattern_flags, int pattern_name_index, Descriptor pattern_descriptor, Attributes attributes) {
        super(name_index, 4);
        this.pattern_flags = pattern_flags;
        this.pattern_name_index = pattern_name_index;
        this.pattern_descriptor = pattern_descriptor;
        this.attributes = attributes;
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitMatcher(this, data);
    }

    public final int pattern_flags;
    public final int pattern_name_index;
    public final Descriptor pattern_descriptor;
    public final Attributes attributes;
}
