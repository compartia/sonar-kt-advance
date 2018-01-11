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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.model.GoodForCache;

import com.google.common.collect.ImmutableMap;
import com.kt.advance.xml.model.PodFile.PpoTypeRef;
import com.kt.advance.xml.model.PodFile.PpoTypeRefKey;

@XmlRootElement(name = "c-analysis")
public class PpoFile extends AnalysisXml {
    public static class ArgElement {
        @XmlElement(name = "msg")
        public MsgElement message;

    }

    public static class DElement {

        @XmlElementWrapper(name = "amsgs")
        @XmlElement(name = "arg")
        public List<ArgElement> args = new ArrayList<>();
    }

    public static class EElement {
        @XmlAttribute(name = "txt")
        public String text;

    }

    public static class MsgElement {
        @XmlAttribute(name = "t")
        public String text;

    }

    /**
     * 'cs': lambda x:PP.CallsiteSPOType(*x), 'rs': lambda
     * x:PP.ReturnsiteSPOType(*x)
     */
    public enum PoType {

        cs("Callsite"), rs("Returnsite"), p("PPO"), pl("PPOLib");
        //        public String label;
        //
        //        SpoType(String label) {
        //            this.label = label;
        //        }

        public String label;

        PoType(String label) {
            this.label = label;
        }

    }

    /**
     * PPO XML c-analysis/function
     *
     * @author artem
     *
     */
    public static class PpoFunction {

        @XmlAttribute(name = "fname")
        public String name;

        @XmlElementWrapper(name = "ppos")
        @XmlElement(name = "ppo")
        public List<PrimaryProofObligation> proofObligations = new ArrayList<>();

        @Override
        public String toString() {
            return "Function [name=" + name + "] ppos=" + proofObligations;
        }

    }

    /**
     * c-analysis/function/primary-proof-obligations/proof-obligation/ location
     *
     * @author artem
     *
     */
    public static class PpoLocation implements GoodForCache {

        private static final long serialVersionUID = 7420557529782621945L;

        @XmlAttribute(name = "byte")
        public int byteNo;

        @XmlAttribute
        public String file;

        @XmlAttribute
        public int line;

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PpoLocation other = (PpoLocation) obj;
            if (byteNo != other.byteNo) {
                return false;
            }
            if (file == null) {
                if (other.file != null) {
                    return false;
                }
            } else if (!file.equals(other.file)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + byteNo;
            result = prime * result + ((file == null) ? 0 : file.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "" + file + ":" + line;
        }

    }

    /**
     * po_status = { 'g': 'safe', 'o': 'open', 'r': 'violation', 'x':
     * 'dead-code' }
     *
     * -- refer CFunctionPPOs.py
     */

    public enum PPOStatus {
        g("safe"), o("open"), r("violation"), x("dead-code"), unknown("unknown");

        public final String label;

        PPOStatus(String label) {
            this.label = label;
        }

    }

    /**
     *
     * @author artem <ppo deps="f" id="15" invs="50" ippo="15" s="g" ts=
     *         "2018-01-02 20:23:19">
     */
    public static class PrimaryProofObligation extends ProofObligationBase {
        @XmlAttribute(name = "deps", required = true)
        public String deps;

        @XmlAttribute(name = "domain")
        public String domain;

        @XmlTransient
        public PpoTypeRef type;

        @XmlAttribute(name = "invs")
        public String invsString;

        /**
         * po_status = { 'g': 'safe', 'o': 'open', 'r': 'violation', 'x':
         * 'dead-code' }
         *
         * -- refer CFunctionPPOs.py
         */
        @XmlAttribute(name = "s", required = true)
        public String status;

        @XmlAttribute(name = "ippo", required = true)
        public Integer ippo;;

        @XmlAttribute(name = "ids")
        public Integer ids;

        @XmlElement(name = "e")
        public EElement evaluation;

        @XmlElement(name = "d")
        public DElement d;

        public Integer[] getInvariants() {
            return splitStringIntoIntegers(this.invsString);
        }

        @XmlTransient
        public PPOStatus getStatusCode() {
            if (this.status != null) {
                return PPOStatus.valueOf(this.status);
            } else {
                return PPOStatus.unknown;
            }
        }

        @Override
        public String toString() {
            return "PrimaryProofObligation [deps=" + deps + ", invs=" + invsString + ", ippo=" + ippo + ", id=" + id
                    + ", timeStamp=" + timeStamp + "]";
        }

        //
        //        @XmlAttribute(name = "c-complexity", required = true)
        //        public Integer complexityC;
        //
        //        @XmlAttribute(name = "p-complexity", required = true)
        //        public Integer complexityP;
        //
        //        @XmlAttribute(name = "fname")
        //        public String fname;
        //
        //        @XmlElement(name = "location", nillable = false, required = true)
        //        public PpoLocation location;
        //
        //        @XmlAttribute(name = "name")
        //        public String name;
        //
        //        @XmlAttribute(name = "origin")
        //        public String origin;
        //
        //        @Override
        //        public String toString() {
        //            return "PrimaryProofObligation [name=" + name + ", fname=" + fname + ", location=" + location + ", origin="
        //                    + origin + ", id=" + id + "]";
        //        }

    }

    public static class ProofObligationBase {
        @XmlAttribute(name = "id", required = true)
        public Integer id;

        @XmlAttribute(name = "ts", required = true)
        public String timeStamp;

        public PpoTypeRefKey getPpoTypeRefKey(AnalysisXml xml) {
            return new PpoTypeRefKey(xml.getBaseDir(), xml, id);
        }

    }

    //    <ppo deps="f" id="15" invs="50" ippo="15" s="g" ts="2018-01-02 20:23:19">

    public static class Statistics {
        @XmlAttribute(name = "size")
        public int size;

    }

    public static class Symbol implements Serializable {
        /**
         *
         */
        private static final long serialVersionUID = -6317397648718840694L;
        public SymbolType type = SymbolType.ID;
        public String value;

        public Symbol() {
        }

        public Symbol(SymbolType type, String value) {
            super();
            this.type = type;
            this.value = value;
        }

    }

    public enum SymbolType {
        ID, CONST;
    }

    /** <var vid="4743" vname="fptr1"/> **/

    public static class Var {
        @XmlAttribute
        public Integer vid;
        @XmlAttribute
        public String vname;

        @Override
        public String toString() {
            return vname;
        }
    }

    private static final Logger LOG = Loggers.get(PpoFile.class.getName());

    /**
     * https://people.eecs.berkeley.edu/~necula/cil/api/type_Cil.html
     *
     * <code>
    	| PlusA
    	| PlusPI
    	| IndexPI
    	| MinusA
    	| MinusPI
    	| MinusPP
    	| Mult
    	| Div
    	| Mod
    	| Shiftlt
    	| Shiftrt
    	| Lt
    	| Gt
    	| Le
    	| Ge
    	| Eq
    	| Ne
    	| BAnd
    	| BXor
    	| BOr
    	| LAnd
    	| LOr
    </code>
     */
    static final Map<String, String> OP_MAP = new ImmutableMap.Builder<String, String>()
            .put("div", "%s / %s")
            .put("plusa", "%s + %s")
            .put("pluspi", "%s + %s")
            .put("mult", "%s * %s")
            .put("minusa", "%s - %s")
            .put("minuspi", "%s - %s")
            .put("minuspp", "%s - %s")
            .put("mod", "%s % %s")
            .put("lt", "%s < %s")
            .put("gt", "%s > %s")
            .put("le", "%s <= %s")
            .put("ge", "%s >= %s")
            .put("indexpi", "%s[%s]")
            .build();

    @XmlElement(name = "function")
    public PpoFunction function;

    public String functionId() {
        return this.header.application.file + "/" + this.function.name + "/";
    }

    public Map<String, PrimaryProofObligation> getPPOsAsMap() {
        final Map<String, PrimaryProofObligation> ret = new HashMap<>();
        final String functionId = functionId();
        for (final PrimaryProofObligation po : function.proofObligations) {

            final String key = functionId + po.id;
            if (ret.containsKey(key)) {
                LOG.warn("duplicated PPO ID: file(" +
                //                        this.getOrigin().getParentFile().getName() + "/" +
                        this.getOrigin().getAbsolutePath() + "), PPO key: " + key
                        + "; file PPO id: " + po.id);
            }
            ret.put(key, po);
        }

        return ret;
    }

    @Override
    public String toString() {
        return "PPO [function=" + function + "]";
    }

}
