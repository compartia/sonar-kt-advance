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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "c-analysis")
public class PrdFile extends AnalysisXml {
    /**
     *
     *
     *
     * po_predicate_constructors = {
     *
     * }
     *
     *
     * @author artem
     *
     */
    public static class Predicate {
        final String[] tags;
        final Integer[] args;
        public final PredicateType type;
        public final Integer index;

        public Predicate(IndexedTableNode node) {
            this.args = node.getArguments();

            this.index = node.index;
            this.tags = splitString(node.tags);

            type = PredicateType.valueOf("_" + tags[0]);
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return type + " - " + index + "-" + tags;
        }
    }

    public static class PredicateKey extends ObjectKey {

        public PredicateKey(File base, AnalysisXml file, Integer id) {
            super(base, file, id, NO_FUNCTION);
        }
    }

    public static class PredicatesDictionary {
        @XmlElementWrapper(name = "po-predicate-table")
        @XmlElement(name = "n")
        public List<IndexedTableNode> predicates = new ArrayList<>();

    }

    public enum PredicateType {
        //      'nn': lambda(x):PO.CPONotNull(*x),
        //      'null': lambda(x):PO.CPONull(*x),
        //      'vm': lambda(x):PO.CPOValidMem(*x),
        //      'gm': lambda(x):PO.CPOGlobalMem(*x),
        //      'ab': lambda(x):PO.CPOAllocationBase(*x),
        //      'tao': lambda(x):PO.CPOTypeAtOffset(*x),
        //      'lb': lambda(x):PO.CPOLowerBound(*x),
        //      'ub': lambda(x):PO.CPOUpperBound(*x),
        //      'ilb': lambda(x):PO.CPOIndexLowerBound(*x),
        //      'iub': lambda(x):PO.CPOIndexUpperBound(*x),
        //      'i': lambda(x):PO.CPOInitialized(*x),
        //      'ir': lambda(x):PO.CPOInitializedRange(*x),
        //      'c': lambda(x):PO.CPOCast(*x),
        //      'pc': lambda(x):PO.CPOPointerCast(*x),
        //      'csu': lambda(x):PO.CPOSignedToUnsignedCast(*x),
        //      'cus': lambda(x):PO.CPOUnsignedToSignedCast(*x),
        //      'z': lambda(x):PO.CPONotZero(*x),
        //      'nt': lambda(x):PO.CPONullTerminated(*x),
        //      'nneg': lambda(x):PO.CPONonNegative(*x),
        //      'iu': lambda(x):PO.CPOIntUnderflow(*x),
        //      'io': lambda(x):PO.CPOIntOverflow(*x),
        //      'w': lambda(x):PO.CPOWidthOverflow(*x),
        //      'plb': lambda(x):PO.CPOPtrLowerBound(*x),
        //      'pub': lambda(x):PO.CPOPtrUpperBound(*x),
        //      'pubd': lambda(x):PO.CPOPtrUpperBoundDeref(*x),
        //      'cb': lambda(x):PO.CPOCommonBase(*x),
        //      'cbt': lambda x:PO.CPOCommonBaseType(*x),
        //      'ft': lambda(x):PO.CPOFormatString(*x),
        //      'no': lambda(x):PO.CPONoOverlap(*x),
        //      'vc': lambda(x):PO.CPOValueConstraint(*x),
        //      'pre': lambda(x):PO.CPOPredicate(*x)

        _nn("Not Null"),//
        _null("Null"),//
        _vm("Valid Mem"), //
        _gm("Global Mem"),//
        _ab("Allocation Base"),//
        _tao("Type At Offset"),//
        _lb("Lower Bound"),//
        _ub("Upper Bound"), //
        _ilb("Index Lower Bound"),//
        _iub("Index Upper Bound"),//
        _i("Initialized"), //
        _ir("Initialized Range"),//
        _c("Cast"),//
        _pc("Pointer Cast"),//
        _csu("Signed To Unsigned Cast"),//
        _cus("Unsigned To Signed Cast"),//
        _z("Not Zero"),//
        _nt("Null Terminated"),//
        _nneg("Non Negative"),//
        _iu("Int Underflow"),//
        _io("Int Overflow"),//
        _w("Width Overflow"),//
        _plb("Ptr Lower Bound"),//
        _pub("Ptr Upper Bound"),//
        _pubd("Ptr Upper Bound Deref"),//
        _cb("Common Base"),//
        _cbt("Common Base Type"),//
        _ft("Format String"),//
        _no("No Overlap"),//
        _vc("Value Constraint"),//
        _pre("Predicate");

        public String label;

        PredicateType(String label) {
            this.label = label;
        }
    }

    public final static String NO_FUNCTION = "*";

    @XmlElement(name = "po-dictionary")
    public PredicatesDictionary predicatesDictionary;

    public static PredicateKey makePredicateKey(AnalysisXml file, Integer index, File base) {

        return new PredicateKey(base, file, index);
    }

    @Override
    public String getFunctionName() {
        return NO_FUNCTION;
    }

    @Override
    public File getOriginAnalysisDir() {
        return getOrigin().getParentFile();
    }

    public Map<PredicateKey, Predicate> getPredicatesAsMap(File base) {
        final Map<PredicateKey, Predicate> map = new HashMap<>();
        for (final IndexedTableNode node : this.predicatesDictionary.predicates) {
            final PredicateKey pk = makePredicateKey(node, base);
            if (map.containsKey(pk)) {
                throw new IllegalArgumentException(pk + " is already in the map " + this.header.application.file);
            }
            map.put(pk, new Predicate(node));
        }

        return map;
    }

    public PredicateKey makePredicateKey(IndexedTableNode node, File base) {
        return makePredicateKey(this, node.index, base);
    }

}
