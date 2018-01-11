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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.sonar.plugins.kt.advance.model.HasOriginFile;

public abstract class AnalysisXml implements HasOriginFile {
    public static class HeaderApp {
        @XmlAttribute
        public String file;
    }

    /**
     * a="19,37,58" ix="33" t="p"
     *
     * @author artem
     *
     */
    public static class IndexedTableNode {
        @XmlAttribute(name = "a")
        public String arguments;

        @XmlAttribute(name = "ix")
        public Integer index;

        @XmlAttribute(name = "t")
        public String tags;

        public static <T extends IndexedTableNode> Map<Integer, T> asMapByIndex(List<T> list) {
            final Map<Integer, T> map = new HashMap<>();
            for (final T x : list) {
                map.put(x.index, x);
            }
            return map;
        }

        @Deprecated
        public static <T extends IndexedTableNode> Map<Integer, IndexedTableNodeRep> asRepMapByIndex(List<T> list) {
            final Map<Integer, IndexedTableNodeRep> map = new HashMap<>();
            for (final T x : list) {
                map.put(x.index, x.asIndexedTableNodeRep());
            }
            return map;
        }

        @Deprecated
        public IndexedTableNodeRep asIndexedTableNodeRep() {
            return new IndexedTableNodeRep(this);
        }

        /**
         *
         *
         * @return
         */

        public Integer[] getArguments() {
            return splitStringIntoIntegers(this.arguments);
        }

    }

    public static class IndexedTableNodeRep {
        public Integer[] args;
        public String[] tags;
        public Integer index;

        public IndexedTableNodeRep(IndexedTableNode node) {
            this.args = splitStringIntoIntegers(node.arguments);
            this.tags = splitString(node.tags);
            this.index = node.index;
        }

        @Deprecated
        public Integer getFirstArg() {
            if (args != null && args.length > 0) {
                return args[0];
            }
            return null;
        }

        @Deprecated
        public String getFirstTag() {
            if (tags != null && tags.length > 0) {
                return tags[0];
            }
            return null;
        }
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

    private File baseDir;

    @XmlElement(name = "header")
    public PoHeader header;

    @XmlTransient
    private File origin;

    public static String[] splitString(String str) {
        if (str == null) {
            return new String[0];
        }
        final String[] split = str.split(",");
        return split;
    }

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
    public File getBaseDir() {
        return baseDir;
    }

    @Override
    public File getOrigin() {
        return origin;
    }

    public File getOriginAnalysisDir() {
        return getOrigin().getParentFile().getParentFile();
    }

    public String getSourceFilename() {
        if (this.header.application == null) {
            throw new IllegalStateException(origin + " file has no header/applicatoin tag");
        }
        return this.header.application.file;
    }

    @Override
    public String getTime() {
        return header.time;
    }

    @Override
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void setOrigin(File origin) {
        this.origin = origin;
    }

}
