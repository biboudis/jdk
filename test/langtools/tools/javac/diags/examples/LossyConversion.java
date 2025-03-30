/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

// key: compiler.warn.possible.loss.of.precision
// key: compiler.warn.possible.loss.of.precision.assignment
// key: compiler.warn.possible.loss.of.precision.parameter
// key: compiler.warn.definite.loss.of.precision
// key: compiler.warn.definite.loss.of.precision.assignment
// key: compiler.warn.definite.loss.of.precision.parameter
// options: -Xlint:lossy-conversions

class LossyConversion {
    void definite(float f) {
        f += 16_777_217;            // compound
        float b = 16_777_217;       // assignment
        m3(16_777_217);          // parameter
    }
    void possible(int i) {
        float f = 1.0f;
        int i2 = 0x10000001;
        i += f;                      // compound
        float b = i2;                // assignment
        m3(i2);                      // parameter
    }
    void m3(float f) {
    }
}
