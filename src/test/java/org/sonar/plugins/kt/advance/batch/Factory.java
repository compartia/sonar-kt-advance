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

import static org.mockito.Mockito.mock;

import java.io.File;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;
import org.sonar.plugins.kt.advance.model.EvFile.Evidence;
import org.sonar.plugins.kt.advance.model.EvFile.PO;
import org.sonar.plugins.kt.advance.model.PpoFile;
import org.sonar.plugins.kt.advance.model.PpoFile.PoPredicate;
import org.sonar.plugins.kt.advance.model.PpoFile.PpoLocation;
import org.sonar.plugins.kt.advance.model.PpoFile.PrimaryProofObligation;
import org.sonar.plugins.kt.advance.model.SpoFile;

public class Factory {
    private final static String FAKE_XML_NAME = "dir/__xml.xml";
    private final static File FAKE_XML_FILE = new File(FAKE_XML_NAME);

    private static PpoFile fakePPo;
    private static SpoFile fakeSPo;
    static {
        fakePPo = new PpoFile();
        fakePPo.function.name = "fake_func";
        fakePPo.setOrigin(FAKE_XML_FILE);

        fakeSPo = new SpoFile();
        fakeSPo.function.name = "fake_func";
        fakeSPo.setOrigin(FAKE_XML_FILE);
    }

    public static IssuableProofObligation createDischargedPO(final PredicateKey predicateType) {
        final PrimaryProofObligation ppo = Factory.createPPO(predicateType);

        final PO proof = new PO();
        proof.evidence = new Evidence();
        proof.evidence.comment = "evidence";
        final IssuableProofObligation ipo = IssuableProofObligation.newBuilder(fakePPo, ppo)
                .setInputFile(mock(InputFile.class))
                .setDischarge(proof).build();

        return ipo;
    }

    public static IssuableProofObligation createOpenPO(final PredicateKey predicateType) {
        final PrimaryProofObligation ppo = Factory.createPPO(predicateType);
        final IssuableProofObligation ipo = IssuableProofObligation.newBuilder(fakePPo, ppo)
                .setInputFile(mock(InputFile.class))
                .build();

        return ipo;
    }

    public static PrimaryProofObligation createPPO(final PredicateKey predicateType) {
        final PrimaryProofObligation ppo = new PrimaryProofObligation();
        ppo.predicate = new PoPredicate();
        ppo.predicate.tag = predicateType.getTag();
        ppo.location = new PpoLocation();
        ppo.location.line = 1;
        ppo.location.file = "foo/bar/fake.c";
        return ppo;
    }

    public static PrimaryProofObligation createPPO(final PredicateKey predicateType, String id) {
        final PrimaryProofObligation ppo = createPPO(predicateType);
        ppo.setId(id);
        return ppo;
    }

    public static IssuableProofObligation createPrimaryPO(final PredicateKey predicateType) {
        final PrimaryProofObligation ppo = Factory.createPPO(predicateType);

        final IssuableProofObligation ipo = IssuableProofObligation.newBuilder(fakePPo, ppo)
                .setInputFile(mock(InputFile.class))
                .build();
        return ipo;
    }

    public static IssuableProofObligation createPrimaryPO(final PredicateKey predicateType, String id) {
        final PrimaryProofObligation ppo = Factory.createPPO(predicateType, id);
        final IssuableProofObligation ipo = IssuableProofObligation.newBuilder(fakePPo, ppo)
                .setInputFile(mock(InputFile.class))
                .build();
        return ipo;
    }

    public static IssuableProofObligation createViolatedPO(final PredicateKey predicateType) {
        return Factory.createViolatedPO(predicateType, mock(InputFile.class));
    }

    public static IssuableProofObligation createViolatedPO(final PredicateKey predicateType, InputFile inputFile) {
        final PrimaryProofObligation ppo = Factory.createPPO(predicateType);

        ppo.complexityC = 1;
        ppo.complexityP = 1;

        final PO proof = new PO();
        proof.evidence = new Evidence();
        proof.evidence.comment = "evidence";
        proof.violation = true;

        final IssuableProofObligation ipo = IssuableProofObligation.newBuilder(fakePPo, ppo)
                .setDischarge(proof)
                .setInputFile(inputFile)
                .build();

        return ipo;
    }

    public static DefaultInputFile makeDefaultInputFile(File basedir, String filename, int len) {
        final DefaultInputFile dif = new DefaultInputFile("", filename);
        dif.setModuleBaseDir(basedir.toPath());
        dif.setLines(len);
        final int[] originalLineOffsets = new int[len];
        for (int f = 0; f < originalLineOffsets.length; f++) {
            originalLineOffsets[f] = f * 200;
        }
        dif.setOriginalLineOffsets(originalLineOffsets);
        return dif;
    }

}