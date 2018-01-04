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
import org.junit.Test;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.FsAbstraction;

import com.kt.advance.xml.model.PpoFile;
import com.kt.advance.xml.model.PpoFile.PrimaryProofObligation;
import com.kt.advance.xml.model.SpoFile;
import com.kt.advance.xml.model.SpoFile.ApiCondition;
import com.kt.advance.xml.model.SpoFile.SPOCall;

public class XmlReadingTests {
    static String APP_BASEDIR = "/Users/artem/work/KestrelTechnology/IN/juliet_v1.3";
    static String MODULE_BASEDIR = "/Users/artem/work/KestrelTechnology/IN/juliet_v1.3/CWE121/s01/CWE129_large/semantics/ktadvance/std_thread";
    private static final Logger LOG = Loggers.get(XmlReadingTests.class.getName());

    @Test
    public void testListPPOs() throws JAXBException {

        int counter = 0;

        final File dir = new File(APP_BASEDIR);
        final Collection<File> iter = FileUtils.listFiles(dir,
            FsAbstraction.ppoFileFilter,
            TrueFileFilter.INSTANCE);

        for (final File ppoFile : iter) {
            counter++;
            if (ppoFile.isFile()) {
                try {
                    final PpoFile ppos = FsAbstraction.readPpoXml(ppoFile);

                    verifyPPO(ppos, ppoFile.getName());
                } catch (final Exception e) {
                    throw new RuntimeException(ppoFile.getAbsolutePath() + "", e);
                }

            }
        }

        assertEquals(4258, counter);
    }

    @Test
    public void testReadPpoXml() throws JAXBException {
        final File ppoFile = new File(MODULE_BASEDIR, "std_thread_stdThreadCreate_ppo.xml");

        /*************/
        final PpoFile ppos = FsAbstraction.readPpoXml(ppoFile);
        /*************/

        verifyPPO(ppos, ppoFile.getName());

        assertEquals(73, ppos.function.proofObligations.size());

        final Map<String, PrimaryProofObligation> map = ppos.getPPOsAsMap();
        final PrimaryProofObligation po30 = map.get(ppos.functionId() + "30");
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
    public void testReadPpoXml2() throws JAXBException {
        final File ppoFile = new File(APP_BASEDIR,
                "/CWE121/s01/char_type_overrun_memcpy/semantics/ktadvance/main_linux/main_linux_main_ppo.xml");

        /*************/
        final PpoFile ppos = FsAbstraction.readPpoXml(ppoFile);
        /*************/

        verifyPPO(ppos, ppoFile.getName());

        assertEquals(12, ppos.function.proofObligations.size());

        final Map<String, PrimaryProofObligation> map = ppos.getPPOsAsMap();
        assertEquals(11, map.size());
        for (final String key : map.keySet()) {
            LOG.info(key);
        }

    }

    @Test
    public void testReadSpoXml() throws JAXBException {
        final File spoFile = new File(APP_BASEDIR,
                "/CWE121/s01/CWE129_large/semantics/ktadvance/x44/x44_CWE121_Stack_Based_Buffer_Overflow__CWE129_large_44_bad_spo.xml");

        /*************/
        final SpoFile spos = FsAbstraction.readSpoXml(spoFile);
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
        assertNotNull(apiCondition.po);
        assertEquals("r", apiCondition.po.status);
        assertNotNull(apiCondition.po.evaluation);
        assertEquals("lower bound of index value: 10, exceeds length: 10", apiCondition.po.evaluation.text);

    }

    @Test
    public void testReadSpoXml2() throws JAXBException {
        final File spoFile = new File(APP_BASEDIR,
                "/CWE121/s01/char_type_overrun_memcpy/semantics/ktadvance/main_linux/main_linux_main_spo.xml");

        /*************/
        final SpoFile spos = FsAbstraction.readSpoXml(spoFile);
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
            LOG.info(call.iloc + ",");
        }
    }

    void verifyPPO(final PpoFile ppos, String filename) {
        assertNotNull(ppos);
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
