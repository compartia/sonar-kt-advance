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

public class PredicateNamingTest {

    static File BASEDIR;

    @BeforeClass
    public static void setup() throws JAXBException {
        final URL url = PredicateNamingTest.class
                .getResource("/test_project/itc-benchmarks/01.w_Defects/ch_analysis/uninit_pointer/");
        BASEDIR = new File(url.getFile());
    }

    private static Map<Integer, PrimaryProofObligation> readPpoMap(String fname) throws JAXBException {
        final File ppoFile = new File(BASEDIR, fname);
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final Map<Integer, PrimaryProofObligation> ppOsAsMap = ppo.getPPOsAsMap();
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
        final Map<Integer, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, 1, "valid-mem (startof (pbuf))");
        testPredicateName(ppOsAsMap, 2, "lower-bound (startof (pbuf))");
        testPredicateName(ppOsAsMap, 3, "upper-bound (startof (pbuf))");

    }

    @Test
    public void testReadPpo_uninit_pointer_007() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_007_ppo.xml";
        final Map<Integer, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, 81, "pointer-cast (buf1)");
        testPredicateName(ppOsAsMap, 82, "initialized (buf1)");
        testPredicateName(ppOsAsMap, 83, "valid-mem (caste (buf1:(void*)))");
        testPredicateName(ppOsAsMap, 84, "lower-bound (caste (buf1:(void*)))");
        testPredicateName(ppOsAsMap, 85, "upper-bound (caste (buf1:(void*)))");

        testPredicateName(ppOsAsMap, 86, "allocation-base (caste (buf3:(void*)))");
        testPredicateName(ppOsAsMap, 87, "pointer-cast (buf3)");
        testPredicateName(ppOsAsMap, 88, "initialized (buf3)");

        testPredicateName(ppOsAsMap, 1, "not-null (\"String1\")");
    }

    @Test
    public void testReadPpo_uninit_pointer_009() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_009_ppo.xml";
        final Map<Integer, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, 6, "not-null (caste (buf:(char*)))");
        testPredicateName(ppOsAsMap, 7, "null-terminated (caste (buf:(char*)))");
        testPredicateName(ppOsAsMap, 8, "lower-bound (caste (buf:(char*)))");
        testPredicateName(ppOsAsMap, 9,
            "ptr-upper-bound (caste (buf:(char*)) + null-terminator-pos[caste (buf:(char*))])");
        testPredicateName(ppOsAsMap, 10,
            "initialized-range (caste (buf:(char*)), null-terminator-pos[caste (buf:(char*))])");
        testPredicateName(ppOsAsMap, 11, "no-overlap (caste (startof (buf1):(char*)), caste (buf:(char*)))");

    }

    @Test
    public void testReadPpo_uninit_pointer_013() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_013_ppo.xml";
        final Map<Integer, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, 1, "int-underflow (5 * sizeof ((int*)))");
        testPredicateName(ppOsAsMap, 2, "int-overflow (5 * sizeof ((int*)))");

        testPredicateName(ppOsAsMap, 3, "pointer-cast (tmp)");
        testPredicateName(ppOsAsMap, 4, "initialized (tmp)");
        testPredicateName(ppOsAsMap, 5, "initialized (i)");
        testPredicateName(ppOsAsMap, 6, "int-underflow (5 * sizeof (int))");

        testPredicateName(ppOsAsMap, 8, "initialized (ptr)");
        testPredicateName(ppOsAsMap, 10, "not-null (ptr)");
        testPredicateName(ppOsAsMap, 11, "valid-mem (ptr)");
        testPredicateName(ppOsAsMap, 12, "ptr-lower-bound (ptr[i])");
    }

    @Test
    public void testReadPpo_uninit_pointer_016() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_016_ppo.xml";
        final Map<Integer, PrimaryProofObligation> ppOsAsMap = readPpoMap(fname);

        testPredicateName(ppOsAsMap, 12, "cast (*(((lval (uninit_pointer_016_gbl_doubleptr) +i lval (i)):((char*)*)))");
        testPredicateName(ppOsAsMap, 13, "initialized (((uninit_pointer_016_gbl_doubleptr +i i):((char*)*))");
        testPredicateName(ppOsAsMap, 15, "initialized (i)");

        testPredicateName(ppOsAsMap, 18, "ptr-lower-bound (uninit_pointer_016_gbl_doubleptr[i])");
        testPredicateName(ppOsAsMap, 19, "ptr-upper-bound-deref (uninit_pointer_016_gbl_doubleptr[i])");

        testPredicateName(ppOsAsMap, 20, "not-null (((uninit_pointer_016_gbl_doubleptr +i i):((char*)*))");

    }

    @Test
    public void testReadPpo_ziplist_zipSaveInteger() throws JAXBException {

        final URL url = PredicateNamingTest.class
                .getResource("/test_project/redis/ch_analysis/src/ziplist/");
        final File BASEDIR2 = new File(url.getFile());

        final String fname = "ziplist_zipSaveInteger_ppo.xml";
        final File ppoFile = new File(BASEDIR2, fname);
        final PpoFile ppo = FsAbstraction.readPpoXml(ppoFile);
        final Map<Integer, PrimaryProofObligation> ppOsAsMap1 = ppo.getPPOsAsMap();
        final Map<Integer, PrimaryProofObligation> ppOsAsMap = ppOsAsMap1;

        testPredicateName(ppOsAsMap, 45, "ptr-lower-bound (caste (addrof (i32):(uint8_t*)) + 1)");
        testPredicateName(ppOsAsMap, 46, "ptr-upper-bound-deref (caste (addrof (i32):(uint8_t*)) + 1)");
        testPredicateName(ppOsAsMap, 47,
            "valid-mem (caste (((caste (addrof (i32):(uint8_t*)) +i 1):(uint8_t*):(void*)))");

    }

    @Test
    public void testReadSpo_uninit_pointer_005() throws JAXBException {

        final String fname = "uninit_pointer_uninit_pointer_005_spo.xml";
        final CallSiteObligation callSiteObligation = readSpoMap(fname).get(0);

        final Map<Integer, SecondaryProofObligation> spOsAsMap = callSiteObligation.getSPOsAsMap();

        testPredicateName(spOsAsMap, 12, "initialized (((*(((startof (pbuf) +i 1):((int*)*)) +i 1):(int*))");
        testPredicateName(spOsAsMap, 4, "not-null (startof (pbuf))");
        testPredicateName(spOsAsMap, 5, "ptr-upper-bound-deref (startof (pbuf) + 3)");
        testPredicateName(spOsAsMap, 6, "ptr-upper-bound-deref (*(((startof (pbuf) +i 1):((int*)*)) + 1)");
        testPredicateName(spOsAsMap, 8, "ptr-upper-bound-deref (startof (pbuf) + 1)");
        testPredicateName(spOsAsMap, 10, "initialized (((startof (pbuf) +i 1):((int*)*))");
        testPredicateName(spOsAsMap, 11, "not-null (*(((startof (pbuf) +i 1):((int*)*)))");

    }

    private void testPredicateName(final Map<Integer, ? extends ProofObligation> ppOsAsMap, int id, String varname) {
        final ProofObligation primaryProofObligation = ppOsAsMap.get(id);
        assertEquals(varname, primaryProofObligation.predicate.toString());
    }
}
