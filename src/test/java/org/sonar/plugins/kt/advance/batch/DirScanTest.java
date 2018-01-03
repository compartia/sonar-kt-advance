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
package org.sonar.plugins.kt.advance.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.model.ApiFile;
import org.sonar.plugins.kt.advance.model.ApiFile.ApiAssumption;
import org.sonar.plugins.kt.advance.model.ApiFile.Caller;
import org.sonar.plugins.kt.advance.model.ApiFile.PoRef;
import org.sonar.plugins.kt.advance.model.EvFile;
import org.sonar.plugins.kt.advance.model.PpoFile;
import org.sonar.plugins.kt.advance.model.PpoFile.PrimaryProofObligation;
import org.sonar.plugins.kt.advance.model.SpoFile;
import org.sonar.plugins.kt.advance.model.SpoFile.CallSiteObligation;
import org.sonar.plugins.kt.advance.model.SpoFile.SecondaryProofObligation;
import org.sonar.plugins.kt.advance.util.StringTools;

public class DirScanTest {

    private static final String SRC_MEMTEST_C = "src/memtest.c";
    private static File BASEDIR;
    private static File MODULE_BASEDIR;
    private static final String FILE_NAME_STEM = "memtest_memtest_test";
    private static File ppoFile;

    private static KtAdvanceSensor session;
    private static final String TEST_PPO_FILE = "/ch_analysis/src/memtest/" + FILE_NAME_STEM + "_ppo.xml";

    private static final Logger LOG = Loggers.get(DirScanTest.class.getName());

    static ResourcePerspectives resourcePerspectives;
    static DefaultFileSystem fileSystem;
    static ActiveRules activeRules;
    static Settings settings;
    private int counter = 0;

    public static InputFile getResource(final String file) {

        final FilePredicate filePredicate = fileSystem.predicates().hasRelativePath(file);
        return fileSystem.inputFile(filePredicate);

    }

    @BeforeClass
    public static void setup() throws JAXBException {
        final URL url = DirScanTest.class.getResource("/test_project");
        BASEDIR = new File(url.getFile());
        MODULE_BASEDIR = new File(BASEDIR, "redis/");

        fileSystem = new DefaultFileSystem(MODULE_BASEDIR.toPath());

        fileSystem.add(Factory.makeDefaultInputFile(MODULE_BASEDIR, SRC_MEMTEST_C, 282));
        {
            final Iterator<File> iter = FileUtils.iterateFiles(MODULE_BASEDIR, new String[] { FsAbstraction.XML_EXT },
                true);

            while (iter.hasNext()) {
                final File file = iter.next();

                if (file.isFile() && FsAbstraction.ppoFileFilter.accept(file)) {
                    fileSystem.add(Factory.makeDefaultInputFile(MODULE_BASEDIR, file.getAbsolutePath(), 282));
                }
            }
        }
        resourcePerspectives = mock(ResourcePerspectives.class);
        final Issuable issuable = mock(Issuable.class);
        final IssueBuilder issueBuilderMock = mock(IssueBuilder.class);
        when(issueBuilderMock.ruleKey(any())).thenReturn(issueBuilderMock);
        when(issueBuilderMock.effortToFix(any())).thenReturn(issueBuilderMock);
        when(issueBuilderMock.severity(any())).thenReturn(issueBuilderMock);
        when(issueBuilderMock.at(any())).thenReturn(issueBuilderMock);
        when(issueBuilderMock.build()).thenReturn(mock(Issue.class));
        when(issueBuilderMock.newLocation()).thenAnswer(invocation -> new DefaultIssueLocation());

        when(issuable.newIssueBuilder()).thenReturn(issueBuilderMock);
        when(resourcePerspectives.as(any(), any(InputPath.class))).thenReturn(issuable);

        activeRules = mock(ActiveRules.class);
        final ActiveRule ruleMock = mock(ActiveRule.class);
        when(ruleMock.param(any())).thenReturn("1.0");
        when(activeRules.find(any(RuleKey.class))).thenReturn(ruleMock);

        settings = mock(Settings.class);
        when(settings.getFloat(any())).thenReturn(1f);
        session = new KtAdvanceSensor(settings, fileSystem, activeRules, resourcePerspectives);
        ppoFile = new File(MODULE_BASEDIR, TEST_PPO_FILE);
    }

    @Before
    public void setupSensor() throws JAXBException {
        session = new KtAdvanceSensor(settings, fileSystem, activeRules, resourcePerspectives);
    }

    @Test
    public void testAnalyseFile() throws JAXBException, IOException {
        final File baseDir = new File(BASEDIR, "itc-benchmarks/01.w_Defects");
        final DefaultFileSystem fs = new DefaultFileSystem(baseDir);

        final DefaultInputFile uninit_pointer = Factory.makeDefaultInputFile(baseDir, "uninit_pointer.c", 10000);
        fs.add(uninit_pointer);
        fs.add(Factory.makeDefaultInputFile(baseDir,
            "ch_analysis/uninit_pointer/uninit_pointer_uninit_pointer_015_func_001_api.xml", 10000));

        final FilePredicate filePredicate = fs.predicates().all();

        System.err.println("listing FS");
        for (final File f : fs.files(filePredicate)) {
            System.err.println(f);
        }

        final File ppoFileL = new File(BASEDIR,
                "/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/"
                        + "uninit_pointer_uninit_pointer_015_ppo.xml");

        session = new KtAdvanceSensor(settings, fs, activeRules, resourcePerspectives);
        session.getFsContext().doInCache(() -> session.analysePpoSpoXml(ppoFileL));

    }

    @Test

    public void testCache() throws JAXBException, IOException {
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);

        final FsAbstraction fsContext = session.getFsContext();
        fsContext.doInCache(() -> {

            final List<IssuableProofObligation> processPPOs = session.processPPOs(ppo);

            for (final IssuableProofObligation ipo : processPPOs) {

                final IssuableProofObligation fromCache = fsContext.getFromCache(ipo.getKey(), true);
                assertNotNull(StringTools.quote(ipo.getKey().toString()) + " not found in cache", fromCache);
                assertEquals(ipo.getKey(), fromCache.getKey());
            }

        });
    }

    @Test
    public void testComplexityLoaded() throws JAXBException {

        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);

        double totalComplexityC = 0;
        double totalComplexityP = 0;
        double totalComplexityG = 0;
        final InputFile resourceMock = mock(InputFile.class);
        when(resourceMock.absolutePath()).thenReturn("");
        when(resourceMock.file()).thenReturn(new File(""));
        for (final PrimaryProofObligation pp : ppo.function.proofObligations) {

            final IssuableProofObligation ipo = IssuableProofObligation.newBuilder(ppo, pp)
                    .setInputFile(resourceMock)
                    .build();

            totalComplexityC += ipo.getComplexityC();
            totalComplexityP += ipo.getComplexityP();
            totalComplexityG += ipo.getComplexityG();

        }
        assertTrue(totalComplexityC > 0);
        assertTrue(totalComplexityP > 0);
        //assertTrue(totalComplexityG > 0);
    }

    @Test
    public void testListPPOs() throws JAXBException {

        final FsAbstraction fs = new FsAbstraction(fileSystem);
        counter = 0;
        fs.forEachPpoFile(file -> {
            counter++;
            System.out.println("testListPPOs:" + file.getAbsolutePath());
            assertTrue(file.getAbsolutePath().endsWith("_ppo.xml"));
        });
        assertEquals(13, counter);
    }

    @Test
    public void testProcessPPOs() throws JAXBException, IOException {

        session.getFsContext().doInCache(() -> {
            final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
            final List<IssuableProofObligation> processPPOs = session.processPPOs(ppo);
            assertEquals(103, processPPOs.size());

        });

    }

    @Test
    public void testReadApi() throws JAXBException {
        final ApiFile api = readApi();
        final PpoFile ppo = readPpo();

        final List<ApiAssumption> assumptions = api.function.apiAssumptions;
        final List<Caller> callers = api.function.callers;

        assertEquals(1, assumptions.size());
        final ApiAssumption apiAssumption = assumptions.get(0);
        assertEquals(1, apiAssumption.dependentPPOs.size());
        final PoRef poRef = apiAssumption.dependentPPOs.get(0);
        assertEquals("2", poRef.getId());

        //

        assertEquals(1, callers.size());
        final Caller caller = callers.get(0);
        assertEquals(136760, caller.callSite.byteNo);
        assertEquals("uninit_pointer.c", caller.callSite.file);
        assertEquals(158, caller.callSite.line);

        //
        final Map<String, PrimaryProofObligation> ppOsAsMap = ppo.getPPOsAsMap();
        for (final ApiAssumption a : assumptions) {
            LOG.info("" + a);
            for (final PoRef ref : a.dependentPPOs) {
                LOG.info("dependentPPO: " + ref.getId() + " " + ppOsAsMap.get(ref.getId()));
                assertNotNull(ppOsAsMap.get(ref.getId()));
            }
        }

    }

    @Test
    public void testReadPev() throws JAXBException {

        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final EvFile pev = FsAbstraction
                .readPevXml(
                    FsAbstraction.replaceSuffix(ppoFile, FsAbstraction.PPO_SUFFIX, FsAbstraction.PEV_SUFFIX));

        assertEquals(ppo.function.proofObligations.size(), pev.function.statistics.total);
        assertEquals(pev.function.statistics.total - pev.function.statistics.totalProven,
            pev.function.openProofObligations.size());

        assertEquals(92, pev.function.statistics.totalProven);
        assertEquals(pev.function.statistics.totalProven, pev.function.dischargedProofObligations.size());

    }

    @Test
    public void testReadPpo() throws JAXBException {

        final File ppoFile = new File(MODULE_BASEDIR, TEST_PPO_FILE);

        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);

        assertEquals(103, ppo.function.proofObligations.size());
        assertEquals(103, ppo.function.statistics.size);
        assertEquals(ppo.function.proofObligations.size(), ppo.function.statistics.size);

        assertEquals("int-underflow (megabytes * 1024)",
            ppo.function.proofObligations.get(1).predicate.toString());

    }

    @Test
    public void testReadSev() throws JAXBException {

        final EvFile sev = FsAbstraction
                .readSevXml(new File(BASEDIR, "invalid_memory_access_invalid_memory_access_main_sev.xml"));

        assertEquals(sev.function.statistics.total - sev.function.statistics.totalProven,
            sev.function.openProofObligations.size());

        assertEquals(66, sev.function.statistics.total);
        assertEquals(64, sev.function.statistics.totalProven);
        assertEquals(42, sev.function.statistics.invariant);
        assertEquals(0, sev.function.statistics.invariantWithApi);

        assertEquals(sev.function.statistics.totalProven, sev.function.dischargedProofObligations.size());

    }

    @Test
    public void testReadSev2() throws JAXBException {
        final File sevFile = new File(BASEDIR,
                "/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/"
                        + "uninit_pointer_uninit_pointer_005_sev.xml");
        final EvFile sev = FsAbstraction
                .readSevXml(sevFile);

        assertEquals(5,
            sev.getDischargedPOsAsMap().size());

    }

    @Test
    public void testReadSpo() throws JAXBException {
        final SpoFile spo = readSpo();

        assertTrue(spo.function.spoWrapper.proofObligations.size() > 0);
        for (final CallSiteObligation co : spo.function.spoWrapper.proofObligations) {
            assertNotNull(co.location);
            assertNotNull(co.location.file);
            assertNotNull(co.fvid);

            assertTrue(co.proofObligations.size() > 0);

            for (final SecondaryProofObligation sspo : co.proofObligations) {
                assertNotNull(sspo.predicate);
                assertNotNull(sspo.predicate.tag);
            }
        }

    }

    @Test
    public void testReadWrongPev() throws JAXBException {
        final File nonExistentFile = FsAbstraction.replaceSuffix(ppoFile, FsAbstraction.PPO_SUFFIX, "NOISE");
        final EvFile pev = FsAbstraction.readPevXml(nonExistentFile);
        assertNull(pev);
    }

    @Test
    public void testReplaceSuffix() {
        final File apiFile = FsAbstraction.replaceSuffix(ppoFile, "ppo", "api");
        assertEquals(apiFile.getName(), FILE_NAME_STEM + "_api.xml");
        System.out.println("filename change:");
        System.out.println(ppoFile);
        System.out.println(apiFile);
    }

    private ApiFile readApi() throws JAXBException {
        final File apiFile = new File(BASEDIR,
                "/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/"
                        + "uninit_pointer_uninit_pointer_008_func_001_api.xml");

        return FsAbstraction.readApiXml(apiFile);

    }

    private PpoFile readPpo() throws JAXBException {
        final File ppoFile = new File(BASEDIR,
                "/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/"
                        + "uninit_pointer_uninit_pointer_008_func_001_ppo.xml");

        return FsAbstraction.readPpoXml(ppoFile);

    }

    private SpoFile readSpo() throws JAXBException {
        final File spoFile = new File(BASEDIR,
                "/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/"
                        + "uninit_pointer_uninit_pointer_005_spo.xml");

        final SpoFile spo = FsAbstraction.readSpoXml(spoFile);
        return spo;
    }

}
