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

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.plugins.kt.advance.model.PpoFile;
import org.sonar.plugins.kt.advance.model.PpoFile.PrimaryProofObligation;
import org.sonar.plugins.kt.advance.model.PpoFile.ProofObligation;
import org.sonar.plugins.kt.advance.model.SpoFile;
import org.sonar.plugins.kt.advance.model.SpoFile.CallSiteObligation;
import org.sonar.plugins.kt.advance.model.SpoFile.SecondaryProofObligation;
import org.sonar.plugins.kt.advance.util.StringTools;

import com.google.common.base.Preconditions;

public class VarLocationTest {

    static File BASEDIR;
    static File BASEDIR2;

    @BeforeClass
    public static void setup() throws JAXBException {
        BASEDIR = new File(VarLocationTest.class
                .getResource("/test_project/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/").getFile());
        BASEDIR2 = new File(VarLocationTest.class
                .getResource("/xmls/").getFile());

        Preconditions.checkNotNull(BASEDIR);
        Preconditions.checkNotNull(BASEDIR2);
        Preconditions.checkArgument(BASEDIR.isDirectory());
        Preconditions.checkArgument(BASEDIR2.isDirectory());
    }

    @Test
    public void test1() {
        final int pos = StringTools.findVarLocation("p",
            "uninit_pointer_004_func_001(p);/*Tool should detect this line as error*/ /*ERROR:Uninitialized pointer*/");
        assertEquals(28, pos);
    }

    @Test
    public void test2() {
        final int pos = StringTools.findVarLocation("foo", "a=foo");
        assertEquals(2, pos);
    }

    @Test
    public void test2a() {
        final int pos = StringTools.findVarLocation("decoded", "        decoded = sdstrim(decoded,\" \");");
        assertEquals(8, pos);
    }

    @Test
    public void test3() {
        final int pos = StringTools.findVarLocation("foo", "foo=a");
        assertEquals(0, pos);
    }

    @Test
    public void test4() {
        final int pos = StringTools.findVarLocation("foo_", "foo_=a");
        assertEquals(0, pos);
    }

    @Test
    public void test5() {
        final int pos = StringTools.findVarLocation("foo", " foo = a ");
        assertEquals(1, pos);
    }

    @Test
    public void test6() {
        final int pos = StringTools.findVarLocation("$foo", "$foo = a ");
        assertEquals(0, pos);
    }

    @Test
    public void test7() {
        final int pos = StringTools.findVarLocation("$foo", "$foo");
        assertEquals(0, pos);
    }

    @Test
    public void test8() {
        final int pos = StringTools.findVarLocation("sizeof (unsigned long)",
            "size_t words = size / sizeof (unsigned long);");
        assertEquals(22, pos);
    }

    @Test
    public void test9() {
        final int pos = StringTools.findVarLocation("*** MEMORY ADDRESSING ERROR: %p contains %lu",
            "printf(\"\n*** MEMORY ADDRESSING ERROR: %p contains %lu\n");
        assertEquals(9, pos);
    }

    @Test
    public void testReadPpo_hyperloglog_pfdebugCommand() throws JAXBException {

        final File ppoFile = new File(BASEDIR2, "hyperloglog_pfdebugCommand_ppo.xml");
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(ppoFile);

        for (int f = 388; f <= 398; f++) {
            if (f < 391 && f > 933) {
                testVarName(ppOsAsMap, Integer.toString(f), "decoded");
            }
        }

    }

    @Test
    public void testReadPpo_memtest_memtest_fill_value() throws JAXBException {

        final URL url = VarLocationTest.class
                .getResource("/test_project/redis/ch_analysis/src/memtest/");
        final File BASEDIR2 = new File(url.getFile());

        final String fname = "memtest_memtest_fill_value_ppo.xml";
        final File ppoFile = new File(BASEDIR2, fname);
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final Map<String, PrimaryProofObligation> ppOsAsMap1 = ppo.getPPOsAsMap();
        final Map<String, PrimaryProofObligation> ppOsAsMap = ppOsAsMap1;

        testVarName(ppOsAsMap, Integer.toString(17), "(bytes & 4095) == 0");

    }

    @Test
    public void testReadPpo_uninit_pointer_005() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_005_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testVarName(ppOsAsMap, Integer.toString(1), "pbuf");
        testVarName(ppOsAsMap, Integer.toString(2), "pbuf");
        testVarName(ppOsAsMap, Integer.toString(3), "pbuf");

    }

    @Test
    public void testReadPpo_uninit_pointer_007() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_007_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testVarName(ppOsAsMap, Integer.toString(81), "buf1");
        testVarName(ppOsAsMap, Integer.toString(82), "buf1");
        testVarName(ppOsAsMap, Integer.toString(83), "buf1");
        testVarName(ppOsAsMap, Integer.toString(84), "buf1");
        testVarName(ppOsAsMap, Integer.toString(85), "buf1");

        testVarName(ppOsAsMap, Integer.toString(86), "buf3");
        testVarName(ppOsAsMap, Integer.toString(87), "buf3");
        testVarName(ppOsAsMap, Integer.toString(88), "buf3");

        testVarName(ppOsAsMap, Integer.toString(1), "String1");

    }

    @Test
    public void testReadPpo_uninit_pointer_009() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_009_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testVarName(ppOsAsMap, Integer.toString(6), "buf");
        testVarName(ppOsAsMap, Integer.toString(7), "buf");
        testVarName(ppOsAsMap, Integer.toString(8), "buf");
        testVarName(ppOsAsMap, Integer.toString(9), "buf");
        testVarName(ppOsAsMap, Integer.toString(10), "buf");
        testVarName(ppOsAsMap, Integer.toString(11), "buf1");

    }

    @Test
    public void testReadPpo_uninit_pointer_013() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_013_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testVarName(ppOsAsMap, Integer.toString(1), "5");
        testVarName(ppOsAsMap, Integer.toString(2), "5");

        testVarName(ppOsAsMap, Integer.toString(3), "tmp");
        testVarName(ppOsAsMap, Integer.toString(4), "tmp");
        testVarName(ppOsAsMap, Integer.toString(5), "i");
        testVarName(ppOsAsMap, Integer.toString(6), "5");

        testVarName(ppOsAsMap, Integer.toString(6), "5");
        testVarName(ppOsAsMap, Integer.toString(8), "ptr");
        testVarName(ppOsAsMap, Integer.toString(10), "ptr");
        testVarName(ppOsAsMap, Integer.toString(11), "ptr");
        testVarName(ppOsAsMap, Integer.toString(12), "ptr");
        testVarName(ppOsAsMap, Integer.toString(9), "i");
    }

    @Test
    public void testReadPpo_uninit_pointer_016() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_016_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testVarName(ppOsAsMap, Integer.toString(12), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(13), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(14), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(15), "i");
        testVarName(ppOsAsMap, Integer.toString(16), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(17), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(18), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(19), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(20), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(21), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(22), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(23), "uninit_pointer_016_gbl_doubleptr");
        testVarName(ppOsAsMap, Integer.toString(24), "0");
        testVarName(ppOsAsMap, Integer.toString(25), "0");
    }

    @Test
    public void testReadPpo_ziplist_zipSaveInteger() throws JAXBException {

        final URL url = VarLocationTest.class
                .getResource("/test_project/redis/ch_analysis/src/ziplist/");
        final File BASEDIR2 = new File(url.getFile());

        final String fname = "ziplist_zipSaveInteger_ppo.xml";
        final File ppoFile = new File(BASEDIR2, fname);
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final Map<String, PrimaryProofObligation> ppOsAsMap = ppo.getPPOsAsMap();

        testVarName(ppOsAsMap, Integer.toString(41), "i32");
        testVarName(ppOsAsMap, Integer.toString(42), "i32");
        testVarName(ppOsAsMap, Integer.toString(43), "i32");
        testVarName(ppOsAsMap, Integer.toString(44), "i32");
        testVarName(ppOsAsMap, Integer.toString(45), "i32");
        testVarName(ppOsAsMap, Integer.toString(46), "i32");
        testVarName(ppOsAsMap, Integer.toString(47), "i32");
        testVarName(ppOsAsMap, Integer.toString(48), "i32");
        testVarName(ppOsAsMap, Integer.toString(49), "i32");
        testVarName(ppOsAsMap, Integer.toString(50), "i32");

        testVarName(ppOsAsMap, Integer.toString(63), "i32");
        testVarName(ppOsAsMap, Integer.toString(64), "i32");
        testVarName(ppOsAsMap, Integer.toString(65), "i32");
        testVarName(ppOsAsMap, Integer.toString(66), "i32");

    }

    @Test
    public void testReadSpo_uninit_pointer_005() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_005_spo.xml";
        final CallSiteObligation callSiteObligation = readSpoMap(fname).get(0);

        final Map<String, SecondaryProofObligation> spOsAsMap = callSiteObligation.getSPOsAsMap();

        testVarName(spOsAsMap, Integer.toString(4), "pbuf");
        testVarName(spOsAsMap, Integer.toString(5), "pbuf");
        testVarName(spOsAsMap, Integer.toString(6), "pbuf");
        testVarName(spOsAsMap, Integer.toString(7), "pbuf");
        testVarName(spOsAsMap, Integer.toString(8), "pbuf");
        testVarName(spOsAsMap, Integer.toString(9), "pbuf");
        testVarName(spOsAsMap, Integer.toString(10), "pbuf");
        testVarName(spOsAsMap, Integer.toString(11), "pbuf");
        testVarName(spOsAsMap, Integer.toString(12), "pbuf");

    }

    @Test
    public void testReadSpo_uninit_pointer_006() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_006_spo.xml";
        final CallSiteObligation callSiteObligation = readSpoMap(fname).get(0);

        final Map<String, SecondaryProofObligation> spOsAsMap = callSiteObligation.getSPOsAsMap();

        testVarName(spOsAsMap, Integer.toString(4), "p");
        testVarName(spOsAsMap, Integer.toString(5), "p");
        testVarName(spOsAsMap, Integer.toString(6), "p");
        testVarName(spOsAsMap, Integer.toString(7), "p");
        testVarName(spOsAsMap, Integer.toString(8), "p");

    }

    private Map<String, PrimaryProofObligation> readPpoMap(String fname) throws JAXBException {
        final File ppoFile = new File(BASEDIR, fname);

        return readPpoMap(ppoFile);
    }

    private List<CallSiteObligation> readSpoMap(String fname) throws JAXBException {
        final File spoFile = new File(BASEDIR, fname);
        final SpoFile spo = FsAbstraction.readSpoXml(spoFile);
        return spo.function.spoWrapper.proofObligations;
    }

    private void testVarName(final Map<String, ? extends ProofObligation> ppOsAsMap, String id, String varname) {
        final ProofObligation primaryProofObligation = ppOsAsMap.get(id);

        assertEquals("var name of PO " + id, varname, primaryProofObligation.predicate.getVarName().value);
    }

    Map<String, PrimaryProofObligation> readPpoMap(final File ppoFile) throws JAXBException {
        Preconditions.checkArgument(ppoFile.isFile(), ppoFile.getAbsolutePath());
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final Map<String, PrimaryProofObligation> ppOsAsMap = ppo.getPPOsAsMap();
        return ppOsAsMap;
    }
}
