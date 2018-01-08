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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "c-analysis")
public class PodFile extends AnalysisXml {
    public static class Assumption {
    }

    /**
     * PPO XML c-analysis/function
     *
     * @author artem
     *
     */
    public static class PodFunction {

        @XmlAttribute(name = "fname")
        public String name;

        @XmlElementWrapper(name = "assumption-table")
        @XmlElement(name = "n")
        public List<Assumption> assumptions = new ArrayList<>();

        @XmlElementWrapper(name = "ppo-type-table")
        @XmlElement(name = "n")
        public List<PpoType> ppoTypes = new ArrayList<>();

        @XmlElementWrapper(name = "spo-type-table")
        @XmlElement(name = "n")
        public List<PpoType> spoTypes = new ArrayList<>();

    }

    /**
     * a="19,37,58" ix="33" t="p"
     *
     * @author artem
     *
     */
    public static class PoType {
        @XmlAttribute(name = "a")
        public String a;
        @XmlAttribute(name = "ix")
        public String ix;
        @XmlAttribute(name = "t")
        public String type;
    }

    public static class PpoType extends PoType {
    }

    public static class SpoType extends PoType {
    }
}
