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
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.REPOSITORY_BASE_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateType;

public class PredicatesTableTest {

    static final PredicateKey PREDICATE_KEY_VALID_MEM = new PredicateKey("valid-mem");
    static final PredicateKey PREDICATE_TYPE_ANY = new PredicateKey("any");

    @Ignore
    @Test
    public void rulesToConsoleXml() throws IOException {
        final List<PredicateType> pp = PredicateTypes.loadPredicates();
        final StringBuilder sb = new StringBuilder();
        for (final PredicateType p : pp) {
            sb.append("\n\n<rule>\n\t<key>").append(p.key.toString()).append("</key>");
            sb.append("\n\t<name>").append(p.name).append("</name>");
            sb.append("\n\t<description><![CDATA[<p>").append(p.name).append("</p>]]></description>");
            sb.append("\n\t<tag>").append(p.key.getTag()).append("</tag>").append("</rule>");
        }
        System.out.println(sb);
    }

    @Test
    public void testApplyDischargeRule() {
        final IssuableProofObligation ipo = Factory.createDischargedPO(PREDICATE_TYPE_ANY);
        final IssuableProofObligation ipo2 = Factory.createViolatedPO(PREDICATE_TYPE_ANY);
        //
        //		assertEquals(RuleKey.of(REPOSITORY_KEY, KTRule.RULE_PEV_DISCHARGED.name()), ipo.getRuleKey());
        //		assertEquals(RuleKey.of(REPOSITORY_KEY, KTRule.RULE_PEV_VIOLATION.name()), ipo2.getRuleKey());
        //
        assertEquals(RuleKey.of(REPOSITORY_BASE_KEY + POState.DISCHARGED,
            ipo.getPredicateType().toString()),
            ipo.getRuleKey());
        assertEquals(RuleKey.of(REPOSITORY_BASE_KEY + POState.VIOLATION,
            ipo2.getPredicateType().toString()),
            ipo2.getRuleKey());
    }

    @Test
    public void testOpenPoRule() {
        final IssuableProofObligation ipo = Factory.createOpenPO(PREDICATE_TYPE_ANY);

        //		assertEquals(RuleKey.of(REPOSITORY_KEY, KTRule.RULE_PEV_OPEN.name()), ipo.getRuleKey());

        assertEquals(
            RuleKey.of(REPOSITORY_BASE_KEY + POState.OPEN, ipo.getPredicateType().toString()),
            ipo.getRuleKey());
    }

    @Test
    public void testPoRule() {

        final IssuableProofObligation ipo = Factory.createPrimaryPO(PREDICATE_KEY_VALID_MEM);

        assertEquals(
            RuleKey.of(REPOSITORY_BASE_KEY + POState.OPEN, ipo.getPredicateType().toString()),
            ipo.getRuleKey());
    }

    @Test
    public void testPredicatesTSV() throws IOException {

        try (final InputStream stream = this
                .getClass().getClassLoader().getResourceAsStream(PredicateTypes.PREDICATES_TSV);) {

            final HashSet<String> keys = new HashSet<>();
            final List<String> readLines = IOUtils.readLines(stream, "UTF-8");

            int line = 1;
            for (final String l : readLines) {
                final String[] split = l.split("\t");

                assertEquals("line " + line + ": " + l, 3, split.length);

                /**
                 * check unique keys
                 */
                assertTrue("duplicated " + split[1], !keys.contains(split[1]));

                /**
                 * check default value
                 */
                assertTrue("wrong default value at line " + line + ": " + l, Double.parseDouble(split[0]) > 0);

                keys.add(split[1]);

                line++;
            }
        }
    }

}