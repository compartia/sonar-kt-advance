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

import java.io.InputStream;
import java.util.Collection;

import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.KtAdvancePlugin;
import org.sonar.plugins.kt.advance.KtLanguage;

import kt.advance.model.Definitions.POStatus;

public final class KtAdvanceRulesDefinition implements RulesDefinition {

    public enum POComplexity {
        C, P, G;

        public String key() {
            return this.name().toLowerCase();
        }
    }

    public static final String XML_INCONSISTENCY_RULE = "xml_inconsistency_rule";

    /**
     * logger
     */
    private static final Logger LOG = Loggers.get(KtAdvanceRulesDefinition.class.getName());

    private static final String RULES_FILENAME = "rules.xml";

    public static final String REPOSITORY_BASE_KEY = KtAdvancePlugin.KEY + ".p.";

    public static final String XML_PROBLEMS_REPO_KEY = REPOSITORY_BASE_KEY + "xml";

    private static final String DEFAULT_ISSUE_COST = "10min";

    @Override
    public void define(Context context) {

        final NewRepository repositoryXmlIssues = context
                .createRepository(XML_PROBLEMS_REPO_KEY, KtLanguage.KEY)
                .setName("KT Advance XML parsing issues");

        final NewRule xmlProblemRule = repositoryXmlIssues.createRule(XML_INCONSISTENCY_RULE)

                .setHtmlDescription("There's a problem with XML")
                .setStatus(RuleStatus.READY)
                .setSeverity(Severity.BLOCKER)
                .setName("XML inconsistency")
                .setType(RuleType.BUG);

        final NewRepository repositoryOpen = context
                .createRepository(REPOSITORY_BASE_KEY + POStatus.open, KtLanguage.KEY)
                .setName("KT Advance (open)");

        final NewRepository repositoryDischarged = context
                .createRepository(REPOSITORY_BASE_KEY + POStatus.discharged, KtLanguage.KEY)
                .setName("KT Advance (discharged)");

        final NewRepository repositoryViolations = context
                .createRepository(REPOSITORY_BASE_KEY + POStatus.violation, KtLanguage.KEY)
                .setName("KT Advance (violations)");

        //        XXX: support DEAD CODE status

        makeRulesSubSet(repositoryOpen,
            Severity.MINOR,
            RuleType.CODE_SMELL,
            "open");

        makeRulesSubSet(repositoryDischarged,
            Severity.INFO,
            RuleType.CODE_SMELL,
            "discharged");

        makeRulesSubSet(repositoryViolations,
            Severity.CRITICAL,
            RuleType.BUG,
            "violation");

        repositoryDischarged.done();
        repositoryOpen.done();
        repositoryViolations.done();
        repositoryXmlIssues.done();
    }

    Collection<NewRule> loadRulesDefenitions(NewRepository repository) {
        LOG.info("reading " + RULES_FILENAME);
        final InputStream rulesXml = this.getClass().getResourceAsStream(RULES_FILENAME);

        final RulesDefinitionXmlLoader rulesLoader = new RulesDefinitionXmlLoader();
        rulesLoader.load(repository, rulesXml, "UTF-8");
        return repository.rules();
    }

    void makeRulesSubSet(NewRepository repository, String severity,
            RuleType ruleType, String... additionalTags) {

        loadRulesDefenitions(repository);

        for (final NewRule rule : repository.rules()) {

            rule
                    .setSeverity(severity)
                    .setType(ruleType)
                    .addTags(additionalTags)
                    .setDebtRemediationFunction(rule.debtRemediationFunctions().linear(DEFAULT_ISSUE_COST));

        }

    }

}
