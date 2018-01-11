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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.sonar.plugins.kt.advance.batch.FsAbstraction.HasPredicateKey;

import com.kt.advance.xml.model.PpoFile.PoType;
import com.kt.advance.xml.model.PrdFile.Predicate;
import com.kt.advance.xml.model.PrdFile.PredicateKey;

@XmlRootElement(name = "c-analysis")
public class PodFile extends AnalysisXml {

    public static class Assumption extends IndexedTableNode {
    }

    /**
     * XML c-analysis/function
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
        public List<PpoTypeNode> ppoTypes = new ArrayList<>();

        @XmlElementWrapper(name = "spo-type-table")
        @XmlElement(name = "n")
        public List<SpoTypeNode> spoTypes = new ArrayList<>();

        @Deprecated
        public Map<Integer, Assumption> getAssumptionsAsMap() {
            return IndexedTableNode.asMapByIndex(assumptions);
        }

        @Deprecated
        public Map<Integer, PpoTypeNode> getPpoTypesAsMap() {
            return IndexedTableNode.asMapByIndex(ppoTypes);
        }

        public Map<Integer, SpoTypeNode> getSpoTypesAsMap() {
            return IndexedTableNode.asMapByIndex(spoTypes);
        }

    }

    @Deprecated
    public static class PpoTypeNode extends IndexedTableNode {

        private PpoTypeRef ppoTypeRefCache;

        public PpoTypeRef asPpoTypeRef(AnalysisXml origin, File baseDir) {
            if (ppoTypeRefCache == null) {
                ppoTypeRefCache = new PpoTypeRef(this, origin, baseDir);
            }
            return ppoTypeRefCache;
        }

    }

    /**
     *
     * See PPOType.py
     *
     *
     *
     * def get_location(self): return
     * self.cdecls.get_location(int(self.args[0]))
     *
     * def get_context(self): return
     * self.contexts.get_program_context(int(self.args[1]))
     *
     * def get_predicate(self): try: return
     * self.pd.get_predicate(int(self.args[2])) except IndexedTableError as e:
     * print(str(e)) raise
     *
     *
     *
     * @author artem
     *
     */
    public static class PpoTypeRef implements HasPredicateKey {
        public Integer locationIndex;
        public Integer contexIndex;
        public PredicateKey predicateIndex;

        //        private String[] tags;
        public PoType proofObligationType;
        final Integer[] args;

        public Predicate predicate;

        public PpoTypeRef(IndexedTableNode node, AnalysisXml origin, File baseDir) {
            this.args = node.getArguments();

            this.locationIndex = args[0];
            this.contexIndex = args[1];
            this.predicateIndex = PrdFile.makePredicateKey(origin, args[2], baseDir);

            this.proofObligationType = PoType.valueOf(splitString(node.tags)[0]);
        }

        public Predicate getPredicate() {
            return this.predicate;
        }

        @Override
        public PredicateKey getPredicateKey() {
            return predicateIndex;
        }

        @Override
        public void setPredicate(Predicate predicate) {
            this.predicate = predicate;

        }

    }

    public static class PpoTypeRefKey extends ObjectKey {
        public PpoTypeRefKey(File base, AnalysisXml file, Integer id) {
            super(base, file, id);
        }

    }

    /**
     * def get_location(self): return
     * self.cdecls.get_location(int(self.args[0]))
     *
     * def get_context(self): return
     * self.contexts.get_program_context(int(self.args[1]))
     *
     * def get_predicate(self): return self.pd.get_predicate(int(self.args[2]))
     *
     * def get_postcondition(self): return
     * self.id.get_postcondition(int(self.args[3]))
     *
     * def get_external_id(self): return int(self.args[3])
     */
    @Deprecated
    public static class SpoTypeNode extends IndexedTableNode {
        private SpoTypeRef spoTypeRefCache;

        public SpoTypeRef asSpoTypeRef(AnalysisXml origin, File baseDir) {
            if (spoTypeRefCache == null) {
                spoTypeRefCache = new SpoTypeRef(this, origin, baseDir);
            }
            return spoTypeRefCache;
        }
    }

    public static class SpoTypeRef extends PpoTypeRef {
        //        public Integer locationIndex;
        //        public Integer contexIndex;
        //        public Integer predicateIndex;

        public Integer postconditionIndex;

        //        public PoType proofObligationType;

        //        private String[] tags;

        public SpoTypeRef(IndexedTableNode node, AnalysisXml origin, File baseDir) {
            super(node, origin, baseDir);
            //            final Integer[] args = node.getArguments();
            //            this.locationIndex = args[0];
            //            this.contexIndex = args[1];
            //            this.predicateIndex = args[2];
            //XXX: might be it is external_id at index 3, see POType.py/ReturnsiteSPOType
            this.postconditionIndex = args[3];

            //            this.proofObligationType = PoType.valueOf(splitString(node.tags)[0]);
        }

    }

    @XmlElement(name = "function")
    public PodFunction function;

    public Map<PpoTypeRefKey, PpoTypeRef> getPpoTypeRefAsMap(File baseDir) {

        final Map<PpoTypeRefKey, PpoTypeRef> map = new HashMap<>();
        for (final PpoTypeNode x : this.function.ppoTypes) {
            final PpoTypeRefKey key = new PpoTypeRefKey(baseDir, this, x.index);
            map.put(key, x.asPpoTypeRef(this, baseDir));
        }
        return map;

    }

}
