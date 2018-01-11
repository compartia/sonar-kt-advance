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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.FsAbstraction;
import org.sonar.plugins.kt.advance.util.MapCounter;

import com.kt.advance.xml.model.AnalysisXml;
import com.kt.advance.xml.model.PodFile;
import com.kt.advance.xml.model.PodFile.PpoTypeRef;
import com.kt.advance.xml.model.PodFile.PpoTypeRefKey;
import com.kt.advance.xml.model.PodFile.SpoTypeNode;
import com.kt.advance.xml.model.PodFile.SpoTypeRef;
import com.kt.advance.xml.model.PpoFile;
import com.kt.advance.xml.model.PpoFile.PrimaryProofObligation;
import com.kt.advance.xml.model.PrdFile;
import com.kt.advance.xml.model.PrdFile.Predicate;
import com.kt.advance.xml.model.PrdFile.PredicateKey;
import com.kt.advance.xml.model.SpoFile;
import com.kt.advance.xml.model.SpoFile.ApiCondition;
import com.kt.advance.xml.model.SpoFile.SPOCall;

public class XmlReadingTests {
    static String naim_APP_BASEDIR = "/Users/artem/work/KestrelTechnology/IN/naim-0.11.8.3.1";
    static String juliet_APP_BASEDIR = "/Users/artem/work/KestrelTechnology/IN/juliet_v1.3";
    static String nagios_APP_BASEDIR = "/Users/artem/work/KestrelTechnology/IN/nagios-2.10";

    //    static String MODULE_BASEDIR = "/Users/artem/work/KestrelTechnology/IN/juliet_v1.3/CWE121/s01/CWE129_large/semantics/ktadvance/std_thread";
    private static final Logger LOG = Loggers.get(XmlReadingTests.class.getName());

    final static String LINE = "****************";

    @Test
    public void testBindAllPod2Ppo() throws JAXBException {
        int errors = 0;
        final File dir = new File(naim_APP_BASEDIR);
        final Collection<File> pods = FileUtils.listFiles(dir,
            FsAbstraction.podFileFilter,
            TrueFileFilter.INSTANCE);

        for (final File pod : pods) {
            try {

                testBindPod2Ppo(pod, dir);

            } catch (final Error e) {
                errors++;
                LOG.error(errors + "\t " + e.getLocalizedMessage());
            } catch (final Exception e) {
                errors++;
                LOG.error(errors + "\t " + e.getLocalizedMessage());
            }

        }

        assertEquals(0, errors);

    }

    @Test
    public void testBindAllPod2Prd() throws JAXBException {

        ///
        final MapCounter<String> stats = new MapCounter<>(3);
        //
        final File base = new File(naim_APP_BASEDIR);

        final Collection<File> predicatesFiles = FileUtils.listFiles(base,
            FsAbstraction.prdFileFilter,
            TrueFileFilter.INSTANCE);

        final Map<PredicateKey, Predicate> allPredicatesMap = FsAbstraction.readAllPredicateXmls(predicatesFiles,
            base);
        ///////
        line();
        LOG.info("total number of UNIQUE predicates keys in " + predicatesFiles.size() + " files is "
                + allPredicatesMap.size());
        line();

        ///////
        int bound = 0;
        int exceptions = 0;
        int errors = 0;

        ///////
        final Collection<File> pods = FileUtils.listFiles(base,
            FsAbstraction.podFileFilter,
            TrueFileFilter.INSTANCE);

        for (final File podFile : pods) {

            try {
                final PodFile dict = FsAbstraction.readPodXml(podFile, base);
                validateHeader(dict);

                //                                final Map<Integer, PpoTypeNode> ppoTypesAsMap = dict.function.getPpoTypesAsMap();
                final Map<PpoTypeRefKey, PpoTypeRef> ppoPpoTypeRefAsMap = dict.getPpoTypeRefAsMap();

                for (final PpoTypeRef ppoTypeRef : ppoPpoTypeRefAsMap.values()) {

                    try {
                        //                        final PpoTypeRef ppoTypeRef = pt.asPpoTypeRef(dict, base);
                        final Predicate predicate = allPredicatesMap.get(ppoTypeRef.predicateIndex);
                        assertNotNull("no predicate with key " + ppoTypeRef.predicateIndex, predicate);
                        ppoTypeRef.setPredicate(predicate);

                        stats.inc(predicate.type.label, 0, 1);
                        stats.inc(predicate.type.label, ppoTypeRef.proofObligationType.ordinal(), 1);

                        stats.inc("=total", 0, 1);

                        bound++;
                    } catch (final Error e) {
                        errors++;
                        LOG.error(e.getMessage());
                    }

                }

            } catch (final Error e) {
                exceptions++;
                LOG.error(e.getLocalizedMessage());
            }

        }

        line();
        LOG.info("\n\n Primary Proof Obligations in " + base);
        LOG.info("\n" + stats.toStringTable());
        line();
        LOG.info("total PPOs with valid predicates:" + bound);
        LOG.info("errors (PPOs with no  predicates):" + errors);
        LOG.info("exceptions :" + exceptions);
        line();

        assertEquals(0, exceptions);
        assertEquals(0, errors);

    }

    @Test
    public void testBindAllPredicateXmlsApi() throws JAXBException {
        final File base = new File(nagios_APP_BASEDIR);

        final Collection<File> pods = FileUtils.listFiles(base,
            FsAbstraction.podFileFilter,
            TrueFileFilter.INSTANCE);

        final Collection<File> predicatesFiles = FileUtils.listFiles(base,
            FsAbstraction.prdFileFilter,
            TrueFileFilter.INSTANCE);

        final Map<PredicateKey, Predicate>//
        allPredicatesMap = FsAbstraction.readAllPredicateXmls(predicatesFiles, base);
        final Map<PpoTypeRefKey, PpoTypeRef> //
        allPodFilesMap = FsAbstraction.readAllPodFilesMap(pods, base);

        FsAbstraction.bindPredicates(allPredicatesMap, allPodFilesMap.values());
    }

    @Test
    public void testBindPodPpoSpoXml() throws JAXBException {
        final File base = new File(juliet_APP_BASEDIR);
        final File pod = new File(base,
                "/CWE121/s01/CWE129_large/semantics/ktadvance/x11/x11_CWE121_Stack_Based_Buffer_Overflow__CWE129_large_11_bad_pod.xml");

        testBindPod2Ppo(pod, base);

    }

    @Test
    @Deprecated
    public void testCountFilesJuliet() throws JAXBException {

        final File dir = new File(juliet_APP_BASEDIR);

        final Collection<File> spos = FileUtils.listFiles(dir,
            FsAbstraction.spoFileFilter,
            TrueFileFilter.INSTANCE);

        final Collection<File> ppos = FileUtils.listFiles(dir,
            FsAbstraction.ppoFileFilter,
            TrueFileFilter.INSTANCE);

        final Collection<File> pods = FileUtils.listFiles(dir,
            FsAbstraction.podFileFilter,
            TrueFileFilter.INSTANCE);

        final Collection<File> apis = FileUtils.listFiles(dir,
            FsAbstraction.apiFileFilter,
            TrueFileFilter.INSTANCE);

        final int expectedNumberOfFiles = 4258;
        assertEquals(expectedNumberOfFiles, apis.size());
        assertEquals(expectedNumberOfFiles, spos.size());
        assertEquals(expectedNumberOfFiles, ppos.size());
        assertEquals(expectedNumberOfFiles, pods.size());
    }

    @Test
    public void testListPPOs() throws JAXBException {

        int counter = 0;

        final File dir = new File(juliet_APP_BASEDIR);
        final Collection<File> iter = FileUtils.listFiles(dir,
            FsAbstraction.ppoFileFilter,
            TrueFileFilter.INSTANCE);
        final MapCounter<String> mapCounter = new MapCounter<>(1);
        for (final File ppoFile : iter) {
            counter++;
            if (ppoFile.isFile()) {
                try {
                    final PpoFile ppoXMl = FsAbstraction.readPpoXml(ppoFile, dir);

                    validateHeader(ppoXMl);

                    final Map<String, PrimaryProofObligation> map = ppoXMl.getPPOsAsMap();

                    for (final String key : map.keySet()) {

                        mapCounter.inc(map.get(key).getStatusCode().label, 0, 1);
                        mapCounter.inc("total", 0, 1);
                    }

                    verifyPPO(ppoXMl, ppoFile.getName());
                } catch (final Exception e) {
                    throw new RuntimeException(ppoFile.getAbsolutePath() + "", e);
                }

            }
        }
        line();
        System.out.println(mapCounter.toStringTable());
        assertEquals(4258, counter);
        line();
        System.out.println("total SPO files read: " + counter);

        line();
    }

    @Test
    public void testListSPOs() throws JAXBException {

        int counter = 0;

        final File dir = new File(juliet_APP_BASEDIR);

        final FsAbstraction fs = new FsAbstraction(dir);
        final Collection<File> iter = FileUtils.listFiles(dir,
            FsAbstraction.spoFileFilter,
            TrueFileFilter.INSTANCE);
        int c = 0;
        //        final MapCounter<String> mapCounter = new MapCounter<>(1);
        for (final File spoFile : iter) {
            counter++;
            if (spoFile.isFile()) {
                try {
                    final SpoFile spos = fs.readSpoXml(spoFile);
                    validateHeader(spos);

                } catch (final Exception e) {
                    c++;
                    LOG.error(c + "\t" +
                            spoFile.getParentFile().getName() + "/" + spoFile.getName() + ":"
                            + e.getLocalizedMessage());
                }

                catch (final Error e) {
                    c++;
                    LOG.error(c + "\t" +
                            spoFile.getParentFile().getName() + "/" + spoFile.getName() + ":"
                            + e.getLocalizedMessage());
                }

            }
        }
        assertEquals(0, c);
        line();
        System.out.println("total SPO files read: " + counter);
        line();
        //        System.out.println(mapCounter.toStringTable());
        assertEquals(4258, counter);
    }

    @Test
    public void testReadAllPrd() throws JAXBException {

        final File dir = new File(juliet_APP_BASEDIR);
        final FsAbstraction fs = new FsAbstraction(dir);

        final Collection<File> predicatesFiles = FileUtils.listFiles(dir,
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

    @Test
    public void testReadPodPpoSpoXml() throws JAXBException {
        final FsAbstraction fs = new FsAbstraction(new File(juliet_APP_BASEDIR));

        final File podFile = new File(juliet_APP_BASEDIR,
                "/CWE121/s01/CWE129_large/semantics/ktadvance/x11/x11_CWE121_Stack_Based_Buffer_Overflow__CWE129_large_11_bad_pod.xml");

        final File ppoFile = XmlNamesUtils.replaceSuffix(podFile, FsAbstraction.POD_SUFFIX, FsAbstraction.PPO_SUFFIX);
        final File spoFile = XmlNamesUtils.replaceSuffix(podFile, FsAbstraction.POD_SUFFIX, FsAbstraction.SPO_SUFFIX);
        /*************/
        final PodFile dict = fs.readPodXml(podFile);
        final PpoFile ppos = fs.readPpoXml(ppoFile);
        final SpoFile spos = fs.readSpoXml(spoFile);
        /*************/

        assertEquals(ppos.function.proofObligations.size(), dict.function.ppoTypes.size());

    }

    @Test
    public void testReadPodXml() throws JAXBException {

        final FsAbstraction fs = new FsAbstraction(new File(juliet_APP_BASEDIR));

        final File podFile = new File(fs.getBaseDir(),
                "/CWE122/s06/CWE135/semantics/ktadvance/x66b/x66b_CWE122_Heap_Based_Buffer_Overflow__CWE135_66b_goodG2BSink_pod.xml");

        /*************/
        final PodFile dict = fs.readPodXml(podFile);
        /*************/

        validateHeader(dict);

        assertNotNull(dict.function);

        assertNotNull(dict.function.assumptions);
        assertNotNull(dict.function.ppoTypes);
        assertNotNull(dict.function.spoTypes);

        assertEquals(1, dict.function.spoTypes.size());
        assertEquals(58, dict.function.ppoTypes.size());
        assertEquals(2, dict.function.assumptions.size());
    }

    @Test
    public void testReadPpoXml() throws JAXBException {
        final FsAbstraction fs = new FsAbstraction(new File(juliet_APP_BASEDIR));
        final File ppoFile = new File(fs.getBaseDir(),
                "CWE121/s01/CWE129_large/semantics/ktadvance/std_thread/std_thread_stdThreadCreate_ppo.xml");

        /*************/
        final PpoFile ppos = fs.readPpoXml(ppoFile);
        /*************/

        verifyPPO(ppos, ppoFile.getName());

        assertEquals(73, ppos.function.proofObligations.size());

        final Map<String, PrimaryProofObligation> map = ppos.getPPOsAsMap();
        final PrimaryProofObligation po30 = map.get(ppos.functionId() + "32");
        assertNotNull(po30.toString() + " has no S", po30.status);

        assertNotNull(po30.toString() + " has no invs", po30.getInvariants());

        assertNotNull(po30.toString() + " has no ippo", po30.ippo);
        assertNotNull(po30.toString() + " has no timeStamp(ts)", po30.timeStamp);
        assertEquals(1, po30.getInvariants().length);
        assertNotNull(po30.d);
        assertNotNull(po30.evaluation);
        assertNotNull(po30.evaluation.text);
        assertEquals("null has been explicitly excluded (either by assignment or by checking)", po30.evaluation.text);

    }

    @Test
    public void testReadSinglePpo() throws JAXBException {
        final FsAbstraction fs = new FsAbstraction(new File(juliet_APP_BASEDIR));
        final File ppoFile = new File(fs.getBaseDir(),
                "/CWE121/s01/char_type_overrun_memcpy/semantics/ktadvance/main_linux/main_linux_main_ppo.xml");

        /*************/
        final PpoFile ppos = fs.readPpoXml(ppoFile);
        /*************/

        verifyPPO(ppos, ppoFile.getName());

        assertEquals(12, ppos.function.proofObligations.size());

        final Map<String, PrimaryProofObligation> map = ppos.getPPOsAsMap();
        assertEquals(12, map.size());

    }

    @Test
    public void testReadSinglePrd() throws JAXBException {
        final File base = new File(juliet_APP_BASEDIR);
        final File prd = new File(base, "CWE121/s02/CWE193_char_alloca_loop/semantics/ktadvance/x15_prd.xml");

        //        final File pod = new File(base,
        //                "CWE121/s02/CWE193_char_alloca_loop/semantics/ktadvance/x15/x15_CWE121_Stack_Based_Buffer_Overflow__CWE193_char_alloca_loop_15_bad_pod.xml");

        final File dir = new File(base,
                "CWE121/s02/CWE193_char_alloca_loop/semantics/ktadvance/x15");
        final Collection<File> predicatesFiles = Collections.singleton(prd);
        //        final Collection<File> pods = Collections.singleton(pod);

        final Collection<File> pods = FileUtils.listFiles(dir,
            FsAbstraction.podFileFilter,
            TrueFileFilter.INSTANCE);

        final Map<PredicateKey, Predicate> allPredicatesMap = FsAbstraction.readAllPredicateXmls(predicatesFiles,
            base);

        final Map<PpoTypeRefKey, PpoTypeRef> proofObligationTypes = FsAbstraction.readAllPodFilesMap(pods, base);

        //        for (final PredicateKey p : allPredicatesMap.keySet()) {
        //            System.err.println(p.toString());
        //        }
        //
        //        line();
        //        for (final PpoTypeRef p : proofObligationTypes.values()) {
        //            System.err.println(p.getPredicateKey());
        //        }

        FsAbstraction.bindPredicates(allPredicatesMap, proofObligationTypes.values());

        for (final PpoTypeRef p : proofObligationTypes.values()) {
            assertNotNull(p.getPredicate());
            //            System.err.println(p.getPredicate());
        }
    }

    @Test
    public void testReadSinglePrd2() throws JAXBException {
        final File base = new File(juliet_APP_BASEDIR);
        final FsAbstraction fs = new FsAbstraction(base);
        final File file = new File(fs.getBaseDir(),
                "/CWE121/s01/CWE129_rand/semantics/ktadvance/x01_prd.xml");

        final PrdFile prdXml = fs.readPrdXml(file);
        final Map<PredicateKey, Predicate> predicatesAsMap = prdXml.getPredicatesAsMap(base);
        final PredicateKey predicateKey = PrdFile.makePredicateKey(prdXml, 18, base);
        final Predicate predicate = predicatesAsMap.get(predicateKey);
        assertNotNull("no predicate found for the key " + predicateKey, predicate);
        assertEquals("iu", predicate.type);

        for (final PredicateKey k : predicatesAsMap.keySet()) {
            System.out.println(k);
        }

    }

    @Test
    public void testReadSingleSpo() throws JAXBException {
        final FsAbstraction fs = new FsAbstraction(new File(juliet_APP_BASEDIR));
        final File spoFile = new File(fs.getBaseDir(),
                "/CWE121/s01/CWE129_large/semantics/ktadvance/x44/x44_CWE121_Stack_Based_Buffer_Overflow__CWE129_large_44_bad_spo.xml");

        /*************/
        final SpoFile spos = fs.readSpoXml(spoFile);
        /*************/
        assertNotNull(spos);
        assertNotNull(spos.header);
        assertNotNull(spos.header.application);
        assertNotNull(spos.function);
        assertNotNull(spos.function.spos);
        assertNotNull(spos.function.spos.callsites);
        assertNotNull(spos.function.spos.callsites.indirectCalls);

        assertEquals(1, spos.function.spos.callsites.indirectCalls.size());
        final SPOCall spoCall = spos.function.spos.callsites.indirectCalls.get(0);

        assertNotNull(spoCall.apiConditions);
        assertEquals(1, spoCall.apiConditions.size());

        final ApiCondition apiCondition = spoCall.apiConditions.get(0);
        assertNotNull(apiCondition);
        assertNotNull(apiCondition.iapi);
        assertNotNull(apiCondition.proofObligation);
        assertEquals("r", apiCondition.proofObligation.status);
        assertNotNull(apiCondition.proofObligation.evaluation);
        assertEquals("lower bound of index value: 10, exceeds length: 10",
            apiCondition.proofObligation.evaluation.text);

    }

    @Test
    public void testReadSpoXml2() throws JAXBException {

        final FsAbstraction fs = new FsAbstraction(new File(juliet_APP_BASEDIR));
        final File spoFile = new File(juliet_APP_BASEDIR,
                "/CWE121/s01/char_type_overrun_memcpy/semantics/ktadvance/main_linux/main_linux_main_spo.xml");

        /*************/
        final SpoFile spos = fs.readSpoXml(spoFile);
        /*************/
        assertNotNull(spos);
        assertNotNull(spos.header);
        assertNotNull(spos.header.application);
        assertNotNull(spos.function);
        assertNotNull(spos.function.spos);
        assertNotNull(spos.function.spos.callsites);

        //
        final List<SPOCall> directCalls = spos.function.spos.callsites.directCalls;
        assertNotNull(directCalls);

        assertEquals(36, directCalls.size());
        final SPOCall spoCall = directCalls.get(0);

        assertNotNull(spoCall.apiConditions);

        for (final SPOCall call : directCalls) {
            assertNotNull(call.iloc);
            assertNotNull(call.ivinfo);
            assertNotNull(call.ictxt);
            //            LOG.info(call.iloc + ",");
        }
    }

    public void validateHeader(AnalysisXml xml) {
        final String message = "Invalid or missing header in file " + xml.getOrigin();
        assertNotNull(message, xml.header);
        assertNotNull("no <application> tag: " + message, xml.header.application);
        assertNotNull("no file attr in <application> tag: " + message, xml.header.application.file);
    }

    void bindSpoCallsApiConditions(final Map<Integer, SpoTypeNode> spoTypesAsMap, List<SPOCall> spoCalls,
            AnalysisXml origin, File baseDir) {

        for (final SPOCall spoCall : spoCalls) {

            for (final ApiCondition apiCondition : spoCall.apiConditions) {
                final Integer id = apiCondition.proofObligation.id;

                final SpoTypeRef spoTypeRef = spoTypesAsMap.get(id).asSpoTypeRef(origin, baseDir);

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

    void testBindPod2Ppo(final File pod, File baseDir) throws JAXBException {
        final File podFile = pod;
        final File ppoFile = XmlNamesUtils.replaceSuffix(podFile, FsAbstraction.POD_SUFFIX, FsAbstraction.PPO_SUFFIX);
        final File spoFile = XmlNamesUtils.replaceSuffix(podFile, FsAbstraction.POD_SUFFIX, FsAbstraction.SPO_SUFFIX);

        /*************/
        final PodFile dict = FsAbstraction.readPodXml(podFile, baseDir);
        final PpoFile ppos = FsAbstraction.readPpoXml(ppoFile, baseDir);
        final SpoFile spos = FsAbstraction.readSpoXml(spoFile, baseDir);
        /*************/

        validateHeader(dict);
        validateHeader(ppos);
        validateHeader(spos);

        //        if (dict.function.assumptions.size() > 0 && dict.function.ppoTypes.size() > 0
        //                && dict.function.spoTypes.size() > 0) {
        //            System.err.println(podFile.getAbsolutePath());
        //        }

        //        final Map<Integer, PpoTypeNode> ppoTypesAsMap = dict.function.getPpoTypesAsMap();
        final Map<Integer, SpoTypeNode> spoTypesAsMap = dict.function.getSpoTypesAsMap();

        final Map<PpoTypeRefKey, PpoTypeRef> ppoTypeRefAsMap = dict.getPpoTypeRefAsMap();

        //if()
        assertEquals("The number of ppo tags in file [" + ppoFile.getAbsolutePath()
                + "] does not correspond the number of nodes of ppo-type-table in [" + pod.getAbsolutePath() + "]",
            ppos.function.proofObligations.size(), ppoTypeRefAsMap.size());

        //        final int spoCount = spoCalls.size() + spos.getCallsites().indirectCalls.size()
        //                + spos.getCallsites().returnSites.size();

        //        assertEquals("The number of spo tags in file [" + spoFile.getAbsolutePath()
        //                + "] does not correspond the number of nodes of spo-type-table in [" + pod.getAbsolutePath() + "]",
        //
        //            spoCount, spoTypesAsMap.size());

        for (final PrimaryProofObligation ppo : ppos.function.proofObligations) {
            try {
                final PpoTypeRef ppoTypeRef = ppoTypeRefAsMap.get(ppo.getPpoTypeRefKey(ppos));
                ppo.type = ppoTypeRef;
                assertNotNull(ppoTypeRef);
            } catch (final Exception e) {
                LOG.error(e.getLocalizedMessage() + " when parsing " + podFile);
            }

        }

        //        List<SPOCall> spoCalls = spos.getCallsites().directCalls;

        bindSpoCallsApiConditions(spoTypesAsMap, spos.getCallsites().directCalls, dict, baseDir);
        bindSpoCallsApiConditions(spoTypesAsMap, spos.getCallsites().indirectCalls, dict, baseDir);
        bindSpoCallsApiConditions(spoTypesAsMap, spos.getCallsites().returnSites, dict, baseDir);
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
