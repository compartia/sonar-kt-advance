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
package org.sonar.plugins.kt.advance;

import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState.DISCHARGED;

import java.util.Collection;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState;

public class KtProfile extends ProfileDefinition {
    private static final Logger LOG = Loggers.get(KtProfile.class.getName());

    private static final String PROFILE_NAME = "KT Advance way";

    private final RuleFinder ruleFinder;

    public KtProfile(RuleFinder ruleFinder) {
        this.ruleFinder = ruleFinder;
    }

    @Override
    public RulesProfile createProfile(ValidationMessages messages) {
        final RulesProfile profile = RulesProfile.create(PROFILE_NAME, KtLanguage.KEY);
        profile.setDefaultProfile(true);

        /**
         * keep POState.DISCHARGED not active by default
         */
        activateRulesInRepo(profile, KtAdvanceRulesDefinition.REPOSITORY_BASE_KEY + POState.OPEN);
        activateRulesInRepo(profile, KtAdvanceRulesDefinition.REPOSITORY_BASE_KEY + POState.VIOLATION);

        profile.setDefaultProfile(true);
        return profile;
    }

    private void activateRulesInRepo(RulesProfile profile, String repoKey) {

        final Collection<Rule> rules = ruleFinder.findAll(RuleQuery.create().withRepositoryKey(repoKey));

        for (final Rule r : rules) {

            final String key = r.getKey();
            if (!key.startsWith(DISCHARGED.name())) {
                profile.activateRule(r, r.getSeverity());
                LOG.info("Activating rule " + key + " severity=" + r.getSeverity());
            }

        }

    }

}