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
package com.kt.advance.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.FsAbstraction;
import org.sonar.plugins.kt.advance.util.MapCounterInt;

import com.kt.advance.xml.XmlReadingTests.ErrorsBundle;
import com.kt.advance.xml.model.AnalysisXml;
import com.kt.advance.xml.model.PodFile;
import com.kt.advance.xml.model.PodFile.PpoTypeRef;
import com.kt.advance.xml.model.PodFile.PpoTypeRefKey;
import com.kt.advance.xml.model.PodFile.SpoTypeNode;
import com.kt.advance.xml.model.PodFile.SpoTypeRef;
import com.kt.advance.xml.model.PpoFile;
import com.kt.advance.xml.model.PpoFile.PPOStatus;
import com.kt.advance.xml.model.PpoFile.PoType;
import com.kt.advance.xml.model.PpoFile.PrimaryProofObligation;
import com.kt.advance.xml.model.PrdFile;
import com.kt.advance.xml.model.PrdFile.Predicate;
import com.kt.advance.xml.model.PrdFile.PredicateKey;
import com.kt.advance.xml.model.SpoFile.ApiCondition;
import com.kt.advance.xml.model.SpoFile.SPOCall;

public class XmlValidator {

    private static final Logger LOG = Loggers.get(XmlValidator.class.getName());

    final static String LINE = "-------------------------------------------------------------------";

    public static FsAbstraction fs;

    public static void main(String[] args) {
        final String basedir = args[0];

        XmlValidator.fs = new FsAbstraction(new File(basedir));

        final JUnitCore junit = new JUnitCore();
        final Result result = junit.run(XmlValidator.class);
    }

    public static void validateHeader(AnalysisXml xml) {
        final String message = "Invalid or missing header in file " + xml.getRelativeOrigin();
        assertNotNull(message, xml.header);
        assertNotNull("no <application> tag: " + message, xml.header.application);
        assertNotNull("no file attr in <application> tag: " + message, xml.header.application.file);
    }

    //    @Ignore
    @Test
    public void testAllPod2PpoCorrespondence() throws JAXBException {
        line();
        LOG.info("reading " + fs.getBaseDir());
        line();

        final ErrorsBundle ee = new ErrorsBundle();

        //////////////////////////////////////////
        final Collection<File> pods = FileUtils.listFiles(fs.getBaseDir(),
            FsAbstraction.podFileFilter,
            TrueFileFilter.INSTANCE);

        ////////////////
        final Collection<File> predicatesFiles = FileUtils.listFiles(fs.getBaseDir(),
            FsAbstraction.prdFileFilter,
            TrueFileFilter.INSTANCE);

        final Map<PredicateKey, Predicate> allPredicatesMap = fs.readAllPredicateXmls(predicatesFiles);
        ///////
        line();
        LOG.info("total number of UNIQUE predicates keys in " + predicatesFiles.size() + " files is "
                + allPredicatesMap.size());
        line();

        ///////////////////////

        int validFiles = 0;
        final MapCounterInt<String> stats = makeStatsTable();
        //        final MapCounter<String> statsByStatus = new MapCounter<>(1);

        for (final File pod : pods) {
            try {

                final File podFile = pod;
                final File ppoFile = XmlNamesUtils.replaceSuffix(podFile, FsAbstraction.POD_SUFFIX,
                    FsAbstraction.PPO_SUFFIX);

                /*************/
                final PodFile dict = fs.readPodXml(podFile);
                final PpoFile ppos = fs.readPpoXml(ppoFile);

                validateHeader(dict);
                validateHeader(ppos);

                final int proofObligationsCount = ppos.function.proofObligations.size();
                final Map<PpoTypeRefKey, PpoTypeRef> ppoPpoTypeRefAsMap = dict.getPpoTypeRefAsMap();
                final int ppoTypesCount = ppoPpoTypeRefAsMap.size();

                assertEquals(
                    "The number of <ppo> tags in file does not correspond the number of nodes of ppo-type-table in ["
                            + dict.getRelativeOrigin()
                            + "]",

                    proofObligationsCount, ppoTypesCount);

                //XXX: remove?? this cal
                //                testBindPod2Ppo(dict, ppos, fs, false, ee);
                FsAbstraction.bindPod2Ppo(ppos, dict);

                ////////////////

                for (final PrimaryProofObligation ppo : ppos.function.proofObligations) {
                    final PPOStatus statusCode = ppo.getStatusCode();
                    //                    statsByStatus.inc(statusCode.label, 0, 1);
                    final Predicate predicate = allPredicatesMap.get(ppo.type.predicateIndex);

                    stats.inc(predicate.type.label, statusCode.label, 1);
                    stats.inc(predicate.type.label, ppo.type.proofObligationType.label, 1);

                    stats.inc("-=total=-", ppo.type.proofObligationType.label, 1);
                    stats.inc("-=total=-", statusCode.label, 1);

                    //                    ppo.type.setPredicate(predicate);
                }

                //                for (final PpoTypeRef ppoTypeRef : ppoPpoTypeRefAsMap.values()) {
                //
                //                    try {
                //                        //                        final PpoTypeRef ppoTypeRef = pt.asPpoTypeRef(dict, base);
                //                        final Predicate predicate = allPredicatesMap.get(ppoTypeRef.predicateIndex);
                //                        assertNotNull("no predicate with key " + ppoTypeRef.predicateIndex, predicate);
                //                        ppoTypeRef.setPredicate(predicate);
                //
                //                        stats.inc(predicate.type.label, ppoTypeRef.proofObligationType.label, 1);
                //                        //                        stats.inc(predicate.type.label, ppoTypeRef.proofObligationType.ordinal(), 1);
                //                        stats.inc("=total", 0, 1);
                //
                //                    } catch (final Throwable e) {
                //                        ee.addError(fs.getRelativeFile(pod), e.getLocalizedMessage());
                //
                //                    }
                //
                //                }

                ////////////////

                validFiles++;

            } catch (final Throwable e) {
                ee.addError(fs.getRelativeFile(pod), e.getLocalizedMessage());
            }

        }
        line();
        ee.print();
        line();

        LOG.info("errors:" + ee.getErrorsCount());
        LOG.info("valid POD/PPO pairs:" + validFiles);
        LOG.info("invalid POD/PPO pairs:" + ee.getErrorsKeysCount());
        line();

        line();
        LOG.info("\n\n Primary Proof Obligations in " + fs.getBaseDir());
        LOG.info("\n" + stats.toSv(",\t"));
        //        line();
        //        LOG.info("\n" + statsByStatus.toStringTable());
        line();

        assertEquals(0, ee.getErrorsCount());
    }

    @Ignore
    @Test
    public void testReadAllPrd() throws JAXBException {

        final Collection<File> predicatesFiles = FileUtils.listFiles(fs.getBaseDir(),
            FsAbstraction.prdFileFilter,
            TrueFileFilter.INSTANCE);

        int count = 0;
        for (final File f : predicatesFiles) {
            final PrdFile prdXml = fs.readPrdXml(f);
            validateHeader(prdXml);
            count += prdXml.predicatesDictionary.predicates.size();
        }
        line();
        LOG.info("total number of predicates in " + predicatesFiles.size() + " files is " + count);
        line();
    }

    //    @Ignore
    //    @Test
    //    public void testBindAllPod2Prd() throws JAXBException {
    //
    //        ///
    //        final MapCounter<String> stats = new MapCounter<>(3);
    //
    //        final Collection<File> predicatesFiles = FileUtils.listFiles(fs.getBaseDir(),
    //            FsAbstraction.prdFileFilter,
    //            TrueFileFilter.INSTANCE);
    //
    //        final Map<PredicateKey, Predicate> allPredicatesMap = fs.readAllPredicateXmls(predicatesFiles);
    //        ///////
    //        line();
    //        LOG.info("total number of UNIQUE predicates keys in " + predicatesFiles.size() + " files is "
    //                + allPredicatesMap.size());
    //        line();
    //
    //        ///////
    //        int bound = 0;
    //        int exceptions = 0;
    //        int errors = 0;
    //
    //        ///////
    //        final Collection<File> pods = FileUtils.listFiles(fs.getBaseDir(),
    //            FsAbstraction.podFileFilter,
    //            TrueFileFilter.INSTANCE);
    //
    //        for (final File podFile : pods) {
    //
    //            try {
    //                final PodFile dict = fs.readPodXml(podFile);
    //                validateHeader(dict);
    //
    //                //                                final Map<Integer, PpoTypeNode> ppoTypesAsMap = dict.function.getPpoTypesAsMap();
    //                final Map<PpoTypeRefKey, PpoTypeRef> ppoPpoTypeRefAsMap = dict.getPpoTypeRefAsMap();
    //
    //                for (final PpoTypeRef ppoTypeRef : ppoPpoTypeRefAsMap.values()) {
    //
    //                    try {
    //                        //                        final PpoTypeRef ppoTypeRef = pt.asPpoTypeRef(dict, base);
    //                        final Predicate predicate = allPredicatesMap.get(ppoTypeRef.predicateIndex);
    //                        assertNotNull("no predicate with key " + ppoTypeRef.predicateIndex, predicate);
    //                        ppoTypeRef.setPredicate(predicate);
    //
    //                        stats.inc(predicate.type.label, 0, 1);
    //                        stats.inc(predicate.type.label, ppoTypeRef.proofObligationType.ordinal(), 1);
    //                        stats.inc("=total", 0, 1);
    //
    //                        bound++;
    //                    } catch (final Error e) {
    //                        errors++;
    //                        LOG.error(e.getMessage());
    //                    }
    //
    //                }
    //
    //            } catch (final Error e) {
    //                exceptions++;
    //                LOG.error(e.getLocalizedMessage());
    //            }
    //
    //        }
    //
    //        line();
    //        LOG.info("\n\n Primary Proof Obligations in " + fs.getBaseDir());
    //        LOG.info("\n" + stats.toStringTable());
    //        line();
    //        LOG.info("total PPOs with valid predicates:" + bound);
    //        LOG.info("errors (PPOs with no  predicates):" + errors);
    //        LOG.info("exceptions :" + exceptions);
    //        line();
    //
    //        assertEquals(0, exceptions);
    //        assertEquals(0, errors);
    //
    //    }

    private void testBindPod2Ppo(PodFile dict, PpoFile ppos, FsAbstraction fs, boolean printStats, ErrorsBundle ee)
            throws JAXBException {

        //        final File spoFile = XmlNamesUtils.replaceSuffix(dict.getOrigin(), FsAbstraction.POD_SUFFIX,
        //            FsAbstraction.SPO_SUFFIX);
        //
        //        /*************/
        //        final SpoFile spos = fs.readSpoXml(spoFile);
        //        /*************/
        //
        //        //        validateHeader(dict);
        //        //        validateHeader(ppos);
        //        validateHeader(spos);
        //
        //        final Map<Integer, SpoTypeNode> spoTypesAsMap = dict.function.getSpoTypesAsMap();
        //
        //        //        assertEquals("The number of ppo tags in file [" + ppoFile.getAbsolutePath()
        //        //                + "] does not correspond the number of nodes of ppo-type-table in [" + pod.getAbsolutePath() + "]",
        //        //            ppos.function.proofObligations.size(), ppoTypeRefAsMap.size());
        //
        //        FsAbstraction.bindPod2Ppo(ppos, dict);
        //
        //        final MapCounter<String> stats = new MapCounter<>(1);
        //        int errors = 0;
        //        for (final PrimaryProofObligation ppo : ppos.function.proofObligations) {
        //
        //            try {
        //
        //                assertNotNull("PPO with key " + ppo.getPpoTypeRefKey(ppos) + " has NO corresponding key in POD file "
        //                        + dict.getRelativeOrigin(),
        //                    ppo.type);
        //                stats.inc(ppo.type.proofObligationType.label, 0, 1);
        //
        //            } catch (final Throwable e) {
        //                errors++;
        //                ee.addError(ppos.getRelativeOrigin(), e.getLocalizedMessage());
        //                //                LOG.error(errors + "\t" + e.getLocalizedMessage() + " when parsing " + podFile);
        //            }
        //
        //        }
        //
        //        if (printStats) {
        //            line();
        //            LOG.info(stats.toStringTable());
        //            line();
        //        }
        //
        //        //        List<SPOCall> spoCalls = spos.getCallsites().directCalls;
        //
        //        bindSpoCallsApiConditions(spoTypesAsMap, spos.getCallsites().directCalls, dict);
        //        bindSpoCallsApiConditions(spoTypesAsMap, spos.getCallsites().indirectCalls, dict);
        //        bindSpoCallsApiConditions(spoTypesAsMap, spos.getCallsites().returnSites, dict);
        //
        //        //        ee.print();
        //        assertEquals("there were errors on binding " + dict.getOrigin(), 0, errors);
    }

    void bindSpoCallsApiConditions(final Map<Integer, SpoTypeNode> spoTypesAsMap, List<SPOCall> spoCalls,
            AnalysisXml origin) {

        for (final SPOCall spoCall : spoCalls) {

            for (final ApiCondition apiCondition : spoCall.apiConditions) {
                final Integer id = apiCondition.proofObligation.id;

                final SpoTypeRef spoTypeRef = spoTypesAsMap.get(id).asSpoTypeRef(origin, origin.getBaseDir());

                assertNotNull(spoTypeRef.contexIndex);
                assertNotNull(spoTypeRef.locationIndex);
                assertNotNull(spoTypeRef.postconditionIndex);
                assertNotNull(spoTypeRef.predicateIndex);

                apiCondition.proofObligation.type = spoTypeRef;
                assertNotNull(apiCondition.proofObligation.type);

            }

        }
    }

    void line() {
        LOG.info(LINE);
    }

    MapCounterInt<String> makeStatsTable() {
        final MapCounterInt<String> stats = new MapCounterInt<>(PPOStatus.values().length + PoType.values().length);
        {

            int c = 0;
            for (final PPOStatus p : PPOStatus.values()) {
                stats.setColumnName(c, p.label);
                c++;
            }
            for (final PoType p : PoType.values()) {
                stats.setColumnName(c, p.label);
                c++;
            }
        }
        return stats;
    }

    void verifyPPO(final PpoFile ppos, String filename) {
        assertNotNull(ppos);
        validateHeader(ppos);
        assertNotNull(filename + "-" + ppos.toString() + " has no function", ppos.function);
        assertNotNull(ppos.function.name);
        for (final PrimaryProofObligation po : ppos.function.proofObligations) {
            assertNotNull(po.id);
            assertNotNull(po.ippo);

            if (po.status != null) {
                assertNotNull(filename + "-" + po.toString() + " has no e sub-element", po.evaluation);
                assertNotNull(po.evaluation.text);
            }
        }

    }

}
