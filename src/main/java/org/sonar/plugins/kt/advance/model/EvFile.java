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
package org.sonar.plugins.kt.advance.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.commons.lang.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class EvFile implements HasOriginFile {

    public static class Evidence implements GoodForCache {

        private static final long serialVersionUID = 3792035626380612971L;
        @XmlAttribute
        public String comment;
    }

    public static class Function {
        @XmlElementWrapper(name = "proof-obligations-discharged")
        @XmlElement(name = "discharged")
        public List<PO> dischargedProofObligations = new ArrayList<>();

        @XmlAttribute
        public String name;

        @XmlElementWrapper(name = "open-proof-obligations")
        @XmlElement(name = "open")
        public List<PO> openProofObligations = new ArrayList<>();

        @XmlElement(name = "statistics")
        public Statistics statistics;

        public Map<String, PO> getDischargedPOsAsMap() {
            final Map<String, PO> ret = new HashMap<>();

            for (final PO po : dischargedProofObligations) {
                ret.put(po.id, po);
            }

            return ret;
        }

    }

    public static class Header {
        public static class Application {
            @XmlAttribute
            public String file;

            public String getFileNormalized() {
                return this.file.replace("//", "/");
            }
        }

        @XmlElement(name = "application")
        public Application application;

    }

    public static class Lifting implements GoodForCache {

        private static final long serialVersionUID = -5886596927509452158L;

        @XmlAttribute(name = "a-type")
        public String type;

        @XmlAttribute(name = "a-id")
        public String apiId;
    }

    public static class PO implements GoodForCache {

        /**
         *
         */
        private static final long serialVersionUID = 1180976511685537595L;

        @XmlElement(name = "evidence")
        public Evidence evidence;

        @JsonIgnore
        @XmlAttribute(required = true)
        public String id;

        @XmlAttribute
        public String method;

        @XmlAttribute
        public String domain;

        @JsonIgnore
        @XmlAttribute(name = "predicate")
        public String predicate;

        @JsonIgnore
        @XmlAttribute(name = "type")
        public String predicateTag;

        @XmlAttribute
        public String time;

        @JsonIgnore
        @XmlAttribute(name = "violation")
        public Boolean violation;

        @XmlElementWrapper(name = "assumptions")
        @XmlElement(name = "uses")
        public List<Lifting> assumptions = new ArrayList<>();

        @JsonIgnore
        public Lifting getDefaultUsedAssumption() {
            if (assumptions != null && !assumptions.isEmpty()) {
                return assumptions.get(0);
            }
            return null;
        }

        public boolean isViolated() {
            return BooleanUtils.toBoolean(violation);
        }
    }

    public static class Statistics {

        @XmlAttribute
        public int checkvalid;

        @XmlAttribute
        public int invariant;

        @XmlAttribute(name = "invariant_with_api")
        public int invariantWithApi;

        @XmlAttribute
        public int total;

        @XmlAttribute(name = "total-proven")
        public int totalProven;

    }

    @XmlElement(name = "header")
    public Header header;

    private File origin;

    public Function function;

    public Map<String, PO> getDischargedPOsAsMap() {
        return function.getDischargedPOsAsMap();
    }

    public Map<String, PO> getOpenPOsAsMap() {
        final Map<String, PO> ret = new HashMap<>();

        for (final PO po : function.openProofObligations) {
            ret.put(po.id, po);
        }

        return ret;
    }

    @Override
    public File getOrigin() {
        return origin;
    }

    @Override
    public String getTime() {
        return null;
    }

    @Override
    public void setOrigin(File originatingFile) {
        this.origin = originatingFile;

    }

}
