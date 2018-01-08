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

package com.kt.advance.xml.model;

import java.io.File;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.sonar.plugins.kt.advance.model.HasOriginFile;

public abstract class AnalysisXml implements HasOriginFile {
    public static class HeaderApp {
        @XmlAttribute
        public String file;
    }

    public static class PoHeader {
        @XmlAttribute
        public String time;

        @XmlElement(name = "name")
        public String name;

        @XmlElement(name = "info")
        public String info;

        @XmlElement(name = "application")
        public HeaderApp application;

    }

    @XmlElement(name = "header")
    public PoHeader header;

    @XmlTransient
    private File origin;

    public static Integer[] splitStringIntoIntegers(String str) {
        if (str == null) {
            return new Integer[0];
        }
        final String[] split = str.split(",");
        final Integer[] ret = new Integer[split.length];
        for (int x = 0; x < split.length; x++) {
            ret[x] = Integer.parseInt(split[x]);
        }
        return ret;
    }

    @Override
    public File getOrigin() {
        return origin;
    }

    @Override
    public String getTime() {
        return header.time;
    }

    @Override
    public void setOrigin(File origin) {
        this.origin = origin;
    }

}
