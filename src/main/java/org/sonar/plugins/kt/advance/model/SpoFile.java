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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.sonar.plugins.kt.advance.model.PpoFile.PpoHeader;
import org.sonar.plugins.kt.advance.model.PpoFile.PpoLocation;
import org.sonar.plugins.kt.advance.model.PpoFile.ProofObligation;

@XmlRootElement(name = "c-analysis")
public class SpoFile implements HasOriginFile {

    public static class CallSiteObligation {
        @XmlAttribute(name = "fvid", required = true)
        public Integer fvid;

        @XmlAttribute(name = "fname")
        public String fname;

        @XmlElement(name = "location", nillable = false, required = true)
        public PpoLocation location;

        @XmlElementWrapper(name = "obligations")
        @XmlElement(name = "obligation")
        public List<SecondaryProofObligation> proofObligations = new ArrayList<>();

        public Map<Integer, SecondaryProofObligation> getSPOsAsMap() {
            final Map<Integer, SecondaryProofObligation> ret = new HashMap<>();

            for (final SecondaryProofObligation spo : proofObligations) {
                ret.put(spo.id, spo);
            }

            return ret;
        }

        @Override
        public String toString() {
            return "callsite-obligation [fvid=" + fvid + "] fvid=" + fvid;
        }
    }

    /**
     * <code>
     * SPO//c-analysis/function/secondary-proof-obligations/callsite-obligations/callsite-obligation/obligations/obligation
     * </code>
     *
     */
    public static class SecondaryProofObligation extends ProofObligation {
        @XmlAttribute(name = "api-id")
        public int apiId;

        @XmlAttribute(name = "g-complexity")
        public Integer complexityG;
    }

    public static class SpoFunction {
        @XmlElement(name = "secondary-proof-obligations")
        public SpoWrapper spoWrapper;
    }

    /**
     * SPO XML c-analysis/function
     *
     * @author artem
     *
     */
    public static class SpoWrapper {
        @XmlAttribute
        public String name;

        @XmlElementWrapper(name = "callsite-obligations")
        @XmlElement(name = "callsite-obligation")
        public List<CallSiteObligation> proofObligations = new ArrayList<>();

        @Override
        public String toString() {
            return "Function [name=" + name + "] SPOs=" + proofObligations;
        }
    }

    @XmlElement(name = "header")
    public PpoHeader header;

    @XmlTransient
    private File origin;

    @XmlElement(name = "function")
    public SpoFunction function;

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

    @Override
    public String toString() {
        return "SPO [function=" + function + "]";
    }

}
