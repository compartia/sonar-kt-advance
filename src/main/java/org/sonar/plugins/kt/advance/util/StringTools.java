/*
 * KT Advance
 * Copyright (c) 2016 Kestrel Technology LLC
 * http://www.kestreltechnology.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.kt.advance.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTools {
    private static final String Q = "\"";

    private StringTools() {
    }

    /**
     * finds start of a var name in the C code line.
     *
     * @param var
     * @param line
     * @return
     */
    public static int findVarLocation(String var, String line) {

        final Pattern p = Pattern.compile("(^|\\W)(" + Pattern.quote(var) + ")(\\W|$)");
        final Matcher m = p.matcher(line);
        if (m.find()) {
            return m.start(2);
        } else {
            return -1;
        }
    }

    public static String quote(String str) {
        return Q + str + Q;
    }

}
