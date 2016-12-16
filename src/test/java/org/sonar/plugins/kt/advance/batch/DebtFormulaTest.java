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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.kt.advance.batch.PredicatesTableTest.PREDICATE_KEY_VALID_MEM;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.config.Settings;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;

public class DebtFormulaTest {

    @Test
    public void testComputeEffort() throws JAXBException {

        final ActiveRules ruleFinder = Mockito.mock(ActiveRules.class);

        final Settings settings = Mockito.mock(Settings.class);
        final PredicateKey predicateType = PREDICATE_KEY_VALID_MEM;
        when(settings.getFloat(Mockito.any())).thenReturn(1f);

        final ActiveRule rule = Mockito.mock(ActiveRule.class);

        //Expect zero-effort for disabled rule
        when(ruleFinder.find(Mockito.any())).thenReturn(null);
        {

            final IssuableProofObligation ipo = Factory.createViolatedPO(predicateType);
            final Double effort1 = ipo.computeEffort(ruleFinder, settings);
            assertEquals(0.0, effort1, 0.001);

        }

        when(ruleFinder.find(Mockito.any())).thenReturn(rule);

        {
            //Violated
            final IssuableProofObligation ipo = Factory.createViolatedPO(predicateType);
            final Double effort1 = ipo.computeEffort(ruleFinder, settings);
            assertEquals(4.0, effort1, 0.5);

        }

        {
            //Primary
            final IssuableProofObligation ipo = Factory.createPrimaryPO(predicateType);
            final Double effort = ipo.computeEffort(ruleFinder, settings);
            assertEquals(1.0, effort, 0.5);
        }

        {
            //Open
            final IssuableProofObligation ipo = Factory.createOpenPO(predicateType);
            final Double effort = ipo.computeEffort(ruleFinder, settings);
            assertEquals(1.0, effort, 0.5);
        }

        {
            //Discharged
            final IssuableProofObligation ipo = Factory.createDischargedPO(predicateType);
            final Double effort = ipo.computeEffort(ruleFinder, settings);
            assertEquals(0.0, effort, 0.01);
        }

    }

    @Test
    public void testComputeEffortFormulaPowers() {
        /**
         * inc bases
         */
        testComputeEffortFormulaTerms(0, 1f, 1f);
        /**
         * inc powers
         */
        testComputeEffortFormulaTerms(1, 2.0001f, 0.1f);
    }

    private void testComputeEffortFormulaTerms(int i, float c, float eps) {

        final float[] args = { c, c, c, c, c, c, c, c, c, c };
        double effortPrev = 0;
        for (int a = i; a < args.length; a += 2) {
            args[a] += eps;

            final EffortComputer effortComputer = new EffortComputer();

            effortComputer.setPredicateTypeMultiplier(args[0]);
            effortComputer.setPredicateScaleFactor(args[1]);

            effortComputer.setComplexityG((int) args[2]);
            effortComputer.setComplexityGScaleFactor(args[3]);

            effortComputer.setComplexityC((int) args[4]);
            effortComputer.setComplexityCScaleFactor(args[5]);

            effortComputer.setComplexityP((int) args[6]);
            effortComputer.setComplexityPScaleFactor(args[7]);

            effortComputer.setPoLevelMultiplier(args[8]);
            effortComputer.setPoLevelScaleFactor(args[9]);

            final double effort = effortComputer.compute();
            assertTrue(
                String.format("%f < %f ( offset=%d param = %d, step=%f)", effort, effortPrev, i, a, eps),
                effort > effortPrev);

            effortPrev = effort;
        }

    }
}