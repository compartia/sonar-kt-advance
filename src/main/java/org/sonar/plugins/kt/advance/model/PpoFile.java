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

        @XmlElement(name = "exp2")
        Expression exp2;

        @XmlElement(name = "constant")
        Constant constant;

        //rv-assumption - relevant fields

        @XmlAttribute
        public String file;

        @XmlAttribute
        public int line;

        public Symbol getVarName() {
            Symbol v = null;
            if ("const".equals(etag)) {
                if (constant != null && "cstr".equals(constant.ctag)) {
                    v = new Symbol(SymbolType.CONST, constant.strValue);
                } else {
                    v = new Symbol(SymbolType.CONST, xstr);
                }

            } else if ("lval".equals(etag) || "startof".equals(etag) || "addrof".equals(etag)) {
                if (lval != null) {
                    v = lval.getVarName();
                }
            } else if (exp != null) {
                /**
                 * unary
                 */
                v = exp.getVarName();

            } else if (exp1 != null) {
                /**
                 * binary
                 */
                v = exp1.getVarName();
                if (v == null && exp2 != null) {
                    v = exp2.getVarName();
                }
            }

            if (v == null) {
                v = new Symbol(SymbolType.CONST, xstr);
            }
            return v;
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

        public Symbol getVarName() {
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

        public Symbol getVarName() {
            if (mem != null) {
                return mem.getVarName();
            } else {
                return new Symbol(SymbolType.ID, var.vname);
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

        public Symbol getVarName() {
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

        public Symbol getVarName() {
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
                    + origin + ", id=" + getId() + "]";
        }

    }

    public static class ProofObligation {

        private String id;

        @XmlElement(name = "predicate")
        public PoPredicate predicate;

        public String getDescription() {
            return predicate.toString();
        }

        @XmlAttribute(name = "id", required = true)
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

    }

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
    public Function function = new Function();

    @XmlElement(name = "header")
    public PpoHeader header = new PpoHeader();

    @XmlTransient
    private File origin;

    @Override
    public File getOrigin() {
        return origin;
    }

    public Map<String, PrimaryProofObligation> getPPOsAsMap() {
        final Map<String, PrimaryProofObligation> ret = new HashMap<>();

        for (final PrimaryProofObligation po : function.proofObligations) {
            ret.put(po.getId(), po);
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
