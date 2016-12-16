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

import com.google.common.collect.ImmutableMap;

@XmlRootElement(name = "c-analysis")
public class PpoFile implements HasOriginFile {

    public static class Constant {
        @XmlAttribute(name = "ctag")
        public String ctag;

        @XmlAttribute(name = "strValue")
        public String strValue;
    }

    public static class Expression {
        /**
         * could be div, add, etc..
         */
        @XmlAttribute
        public String xstr;

        @XmlAttribute(name = "byte")
        public int byteNo;

        @XmlElement(name = "lval")
        public LValue lval;

        @XmlAttribute(name = "etag")
        public String etag;

        @XmlElement(name = "exp")
        Expression exp;

        @XmlElement(name = "exp1")
        Expression exp1;

        @XmlElement(name = "constant")
        Constant constant;

        //rv-assumption - relevant fields

        @XmlAttribute
        public String file;

        @XmlAttribute
        public int line;

        public String getVarName() {
            if ("const".equals(etag)) {
                if (constant != null && "cstr".equals(constant.ctag)) {
                    return constant.strValue;
                }
                return xstr;

            } else if ("lval".equals(etag) || "startof".equals(etag) || "addrof".equals(etag)) {
                if (lval != null) {
                    return lval.getVarName();
                } else {
                    return null;
                }
            } else if (exp != null) {
                return exp.getVarName();
            } else if (exp1 != null) {
                return exp1.getVarName();
            }
            return xstr;
        }

        public PpoLocation location() {
            if (file == null) {
                return null;
            }
            final PpoLocation loc = new PpoLocation();
            loc.file = file;
            loc.byteNo = byteNo;
            loc.line = line;
            return loc;
        }

        @Override
        public String toString() {
            return xstr;
        }
    }

    public static class ExpressionHolder {
        @XmlAttribute
        public String xstr;

        @XmlElement(name = "exp")
        public Expression exp;

        public String getVarName() {
            if (exp != null) {
                return exp.getVarName();
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return xstr;
        }
    }

    /**
     * PPO XML c-analysis/function
     *
     * @author artem
     *
     */
    public static class Function {
        @XmlAttribute
        public String name;
        @XmlElementWrapper(name = "primary-proof-obligations")
        @XmlElement(name = "proof-obligation")
        public List<PrimaryProofObligation> proofObligations = new ArrayList<>();

        @XmlElement(name = "statistics")
        public Statistics statistics;

        @Override
        public String toString() {
            return "Function [name=" + name + "] ppos=" + proofObligations;
        }

    }

    public static class LHost {

        @XmlElement(name = "mem")
        public Expression mem;

        @XmlElement(name = "var")
        Var var;

        public String getVarName() {
            if (mem != null) {
                return mem.getVarName();
            } else {
                return var.vname;
            }
        }

        @Override
        public String toString() {
            if (mem != null) {
                return mem.toString();
            } else {
                return var.toString();
            }
        }

    }

    public static class LValue {
        @XmlElement(name = "lhost")
        public LHost lhost;

        public String getVarName() {
            return lhost.getVarName();
        }

        @Override
        public String toString() {
            return lhost.toString();
        }
    }

    public static class PoPredicate {
        @XmlElement(name = "base-exp")
        public ExpressionHolder baseExp;

        @XmlElement(name = "len-exp")
        public ExpressionHolder lenExp;

        @XmlElement(name = "exp")
        public Expression exp;

        @XmlElement(name = "exp1")
        public Expression exp1;
        @XmlElement(name = "exp2")
        public Expression exp2;

        @XmlElement(name = "lval")
        public LValue lval;

        /**
         * could be div, add, etc..
         */
        @XmlAttribute
        public String op;

        @XmlAttribute
        public String size;

        @XmlAttribute
        public String tag;

        public String getExpression() {
            String expStr = "";

            if (lenExp != null && baseExp != null) {
                expStr = String.format(" %s, %s ", baseExp, lenExp);//XXX: what to show in this case?
            }

            if (exp1 != null && exp2 != null) {
                /*
                 * binary operation
                 */
                if (OP_MAP.containsKey(op)) {
                    expStr = String.format(OP_MAP.get(op), exp1, exp2);
                } else {
                    if (op != null) {
                        expStr = String.format("%s: %s, %s", op, exp1, exp2);
                    } else {
                        expStr = String.format(" %s, %s ", exp1, exp2);//XXX: what to show in this case?
                    }
                }

            } else if (exp != null) {
                expStr = exp.toString();
            } else if (lval != null) {
                expStr = lval.toString();
            }

            return expStr.trim();
        }

        public String getVarName() {
            if (baseExp != null) {
                return baseExp.getVarName();
            } else if (exp != null) {
                return exp.getVarName();
            } else if (exp1 != null) {
                return exp1.getVarName();
            } else if (lval != null) {
                return lval.getVarName();
            }

            return null;

        }

        @Override
        public String toString() {
            final String expStr = getExpression();

            return tag + " (" + expStr + ")";
        }
    }

    public static class PpoHeader {
        @XmlAttribute
        public String time;

        @XmlElement(name = "application")
        public PpoHeaderApp app;
    }

    public static class PpoHeaderApp {
        @XmlAttribute
        public String file;

        @XmlAttribute()
        public String name;

    }

    /**
     * c-analysis/function/primary-proof-obligations/proof-obligation/ location
     *
     * @author artem
     *
     */
    public static class PpoLocation implements Serializable {

        private static final long serialVersionUID = 7420557529782621945L;

        @XmlAttribute(name = "byte")
        public int byteNo;

        @XmlAttribute
        public String file;

        @XmlAttribute
        public int line;

        @Override
        public String toString() {
            return "" + file + ":" + line;
        }

    }

    /**
     * PPO//c-analysis/function/primary-proof-obligations/proof-obligation
     *
     * @author artem
     *
     */
    public static class PrimaryProofObligation extends ProofObligation {

        @XmlAttribute(name = "c-complexity", required = true)
        public Integer complexityC;

        @XmlAttribute(name = "p-complexity", required = true)
        public Integer complexityP;

        @XmlAttribute(name = "fname")
        public String fname;

        @XmlElement(name = "location", nillable = false, required = true)
        public PpoLocation location;

        @XmlAttribute(name = "name")
        public String name;

        @XmlAttribute(name = "origin")
        public String origin;

        @Override
        public String toString() {
            return "PrimaryProofObligation [name=" + name + ", fname=" + fname + ", location=" + location + ", origin="
                    + origin + ", id=" + id + "]";
        }

    }

    public static class ProofObligation {

        @XmlAttribute(name = "id", required = true)
        public int id;

        @XmlElement(name = "predicate")
        public PoPredicate predicate;

        public String getDescription() {
            return predicate.toString();
        }

    }

    public static class Statistics {
        @XmlAttribute(name = "size")
        public int size;

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
    public Function function;

    @XmlElement(name = "header")
    public PpoHeader header = new PpoHeader();

    @XmlTransient
    private File origin;

    @Override
    public File getOrigin() {
        return origin;
    }

    public Map<Integer, PrimaryProofObligation> getPPOsAsMap() {
        final Map<Integer, PrimaryProofObligation> ret = new HashMap<>();

        for (final PrimaryProofObligation po : function.proofObligations) {
            ret.put(po.id, po);
        }

        return ret;
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
        return "PPO [function=" + function + "]";
    }

}
