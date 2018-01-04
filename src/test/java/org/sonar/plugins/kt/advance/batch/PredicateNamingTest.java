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

import com.kt.advance.xml.model.PpoFile;
import com.kt.advance.xml.model.SpoFile;
import com.kt.advance.xml.model.PpoFile.PrimaryProofObligation;
import com.kt.advance.xml.model.PpoFile.ProofObligationBase;
import com.kt.advance.xml.model.SpoFile.CallSiteObligation;
import com.kt.advance.xml.model.SpoFile.SecondaryProofObligation;

public class PredicateNamingTest {

    static File BASEDIR;

    @BeforeClass
    public static void setup() throws JAXBException {
        final URL url = PredicateNamingTest.class
                .getResource("/test_project/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/");
        BASEDIR = new File(url.getFile());
    }

    private static Map<String, PrimaryProofObligation> readPpoMap(String fname) throws JAXBException {
        final File ppoFile = new File(BASEDIR, fname);
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final Map<String, PrimaryProofObligation> ppOsAsMap = ppo.getPPOsAsMap();
        return ppOsAsMap;
    }

    private static List<CallSiteObligation> readSpoMap(String fname) throws JAXBException {
        final File spoFile = new File(BASEDIR, fname);
        final SpoFile spo = FsAbstraction.readSpoXml(spoFile);
        return spo.function.spoWrapper.proofObligations;
    }

    @Test
    public void testReadPpo_uninit_pointer_005() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_005_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, Integer.toString(1), "valid-mem (startof (pbuf))");
        testPredicateName(ppOsAsMap, Integer.toString(2), "lower-bound (startof (pbuf))");
        testPredicateName(ppOsAsMap, Integer.toString(3), "upper-bound (startof (pbuf))");

    }

    @Test
    public void testReadPpo_uninit_pointer_007() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_007_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, Integer.toString(81), "pointer-cast (buf1)");
        testPredicateName(ppOsAsMap, Integer.toString(82), "initialized (buf1)");
        testPredicateName(ppOsAsMap, Integer.toString(83), "valid-mem (caste (buf1:(void*)))");
        testPredicateName(ppOsAsMap, Integer.toString(84), "lower-bound (caste (buf1:(void*)))");
        testPredicateName(ppOsAsMap, Integer.toString(85), "upper-bound (caste (buf1:(void*)))");

        testPredicateName(ppOsAsMap, Integer.toString(86), "allocation-base (caste (buf3:(void*)))");
        testPredicateName(ppOsAsMap, Integer.toString(87), "pointer-cast (buf3)");
        testPredicateName(ppOsAsMap, Integer.toString(88), "initialized (buf3)");

        testPredicateName(ppOsAsMap, Integer.toString(1), "not-null (\"String1\")");
    }

    @Test
    public void testReadPpo_uninit_pointer_009() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_009_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, Integer.toString(6), "not-null (caste (buf:(char*)))");
        testPredicateName(ppOsAsMap, Integer.toString(7), "null-terminated (caste (buf:(char*)))");
        testPredicateName(ppOsAsMap, Integer.toString(8), "lower-bound (caste (buf:(char*)))");
        testPredicateName(ppOsAsMap, Integer.toString(9),
            "ptr-upper-bound (caste (buf:(char*)) + null-terminator-pos[caste (buf:(char*))])");
        testPredicateName(ppOsAsMap, Integer.toString(10),
            "initialized-range (caste (buf:(char*)), null-terminator-pos[caste (buf:(char*))])");
        testPredicateName(ppOsAsMap, Integer.toString(11),
            "no-overlap (caste (startof (buf1):(char*)), caste (buf:(char*)))");

    }

    @Test
    public void testReadPpo_uninit_pointer_013() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_013_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, Integer.toString(1), "int-underflow (5 * sizeof ((int*)))");
        testPredicateName(ppOsAsMap, Integer.toString(2), "int-overflow (5 * sizeof ((int*)))");

        testPredicateName(ppOsAsMap, Integer.toString(3), "pointer-cast (tmp)");
        testPredicateName(ppOsAsMap, Integer.toString(4), "initialized (tmp)");
        testPredicateName(ppOsAsMap, Integer.toString(5), "initialized (i)");
        testPredicateName(ppOsAsMap, Integer.toString(6), "int-underflow (5 * sizeof (int))");

        testPredicateName(ppOsAsMap, Integer.toString(8), "initialized (ptr)");
        testPredicateName(ppOsAsMap, Integer.toString(10), "not-null (ptr)");
        testPredicateName(ppOsAsMap, Integer.toString(11), "valid-mem (ptr)");
        testPredicateName(ppOsAsMap, Integer.toString(12), "ptr-lower-bound (ptr[i])");
    }

    @Test
    public void testReadPpo_uninit_pointer_016() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_016_ppo.xml";
        final Map<String, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, Integer.toString(12),
            "cast (*(((lval (uninit_pointer_016_gbl_doubleptr) +i lval (i)):((char*)*)))");
        testPredicateName(ppOsAsMap, Integer.toString(13),
            "initialized (((uninit_pointer_016_gbl_doubleptr +i i):((char*)*))");
        testPredicateName(ppOsAsMap, Integer.toString(15), "initialized (i)");

        testPredicateName(ppOsAsMap, Integer.toString(18), "ptr-lower-bound (uninit_pointer_016_gbl_doubleptr[i])");
        testPredicateName(ppOsAsMap, Integer.toString(19),
            "ptr-upper-bound-deref (uninit_pointer_016_gbl_doubleptr[i])");

        testPredicateName(ppOsAsMap, Integer.toString(20),
            "not-null (((uninit_pointer_016_gbl_doubleptr +i i):((char*)*))");

    }

    @Test
    public void testReadPpo_ziplist_zipSaveInteger() throws JAXBException {

        final URL url = PredicateNamingTest.class
                .getResource("/test_project/redis/ch_analysis/src/ziplist/");
        final File BASEDIR2 = new File(url.getFile());

        final String fname = "ziplist_zipSaveInteger_ppo.xml";
        final File ppoFile = new File(BASEDIR2, fname);
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final Map<String, PrimaryProofObligation> ppOsAsMap1 = ppo.getPPOsAsMap();
        final Map<String, PrimaryProofObligation> ppOsAsMap = ppOsAsMap1;

        testPredicateName(ppOsAsMap, Integer.toString(45), "ptr-lower-bound (caste (addrof (i32):(uint8_t*)) + 1)");
        testPredicateName(ppOsAsMap, Integer.toString(46),
            "ptr-upper-bound-deref (caste (addrof (i32):(uint8_t*)) + 1)");
        testPredicateName(ppOsAsMap, Integer.toString(47),
            "valid-mem (caste (((caste (addrof (i32):(uint8_t*)) +i 1):(uint8_t*):(void*)))");

    }

    @Test
    public void testReadSpo_uninit_pointer_005() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_005_spo.xml";
        final CallSiteObligation callSiteObligation = readSpoMap(fname).get(0);

        final Map<String, SecondaryProofObligation> spOsAsMap = callSiteObligation.getSPOsAsMap();

        testPredicateName(spOsAsMap, Integer.toString(12),
            "initialized (((*(((startof (pbuf) +i 1):((int*)*)) +i 1):(int*))");
        testPredicateName(spOsAsMap, Integer.toString(4), "not-null (startof (pbuf))");
        testPredicateName(spOsAsMap, Integer.toString(5), "ptr-upper-bound-deref (startof (pbuf) + 3)");
        testPredicateName(spOsAsMap, Integer.toString(6),
            "ptr-upper-bound-deref (*(((startof (pbuf) +i 1):((int*)*)) + 1)");
        testPredicateName(spOsAsMap, Integer.toString(8), "ptr-upper-bound-deref (startof (pbuf) + 1)");
        testPredicateName(spOsAsMap, Integer.toString(10), "initialized (((startof (pbuf) +i 1):((int*)*))");
        testPredicateName(spOsAsMap, Integer.toString(11), "not-null (*(((startof (pbuf) +i 1):((int*)*)))");

    }

    private void testPredicateName(final Map<String, ? extends ProofObligationBase> ppOsAsMap, String id, String varname) {
        final ProofObligationBase primaryProofObligation = ppOsAsMap.get(id);
        assertEquals(varname, primaryProofObligation.predicate.toString());
    }
}
