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

import static org.sonar.api.config.PropertyDefinition.builder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POComplexity;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POLevel;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateType;

public class PluginParameters {

    private static final String M_SUFFIX = "multiplier";
    private static final String S_SUFFIX = "scale";

    private static final String PFX = "effort";

    /**
     * <code>m_t</code>
     */
    public static final String PARAM_EFFORT_PREDICATE_SCALE = join(PFX, "predicate", S_SUFFIX);

    /**
     * <code>m_s</code>
     */
    public static final String PARAM_EFFORT_PO_STATE_SCALE = join(PFX, "state", S_SUFFIX);
    /**
     * <code>m_s</code>
     */
    public static final String PARAM_EFFORT_LEVEL_SCALE = join(PFX, "level", S_SUFFIX);

    private static final String CATEGORY = "technicaldebt";

    //

    private static final String SUB_CATEGORY_STATE = "debt.computation.state";

    private static final String SUB_CATEGORY_LEVEL = "debt.computation.level";
    private static final String SUB_CATEGORY_COMPLEXITY = "debt.computation.complexity";
    private static final Logger LOG = Loggers.get(PluginParameters.class.getName());

    public static String join(String... components) {
        return StringUtils.join(components, ".");
    }

    public static String paramKey(POComplexity complexity) {
        return join(PFX, "complexity", complexity.key(), S_SUFFIX);
    }

    public static String paramKey(POLevel level) {
        return join(PFX, level.key(), M_SUFFIX);
    }

    public static String paramKey(POState state) {
        return join(PFX, state.key(), M_SUFFIX);
    }

    public List<PropertyDefinition> getPropertyDefinitions() {
        final List<PropertyDefinition> props = new ArrayList<>();

        /* PO state-specific params */
        {
            props.add(builder(PARAM_EFFORT_PO_STATE_SCALE)
                    .defaultValue("0.5")
                    .category(CATEGORY)
                    .subCategory(SUB_CATEGORY_STATE)
                    .type(PropertyType.FLOAT)
                    .build());

            props.add(builder(paramKey(POState.DISCHARGED))
                    .defaultValue("0")
                    .category(CATEGORY)
                    .subCategory(SUB_CATEGORY_STATE)
                    .type(PropertyType.FLOAT)
                    .build());
            props.add(builder(paramKey(POState.OPEN))
                    .defaultValue("2")
                    .subCategory(SUB_CATEGORY_STATE)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());
            props.add(builder(paramKey(POState.VIOLATION))
                    .defaultValue("10")
                    .subCategory(SUB_CATEGORY_STATE)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());
        }
        /* PO level-specific params */
        {
            props.add(builder(PARAM_EFFORT_LEVEL_SCALE)
                    .defaultValue("0.5")
                    .subCategory(SUB_CATEGORY_LEVEL)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());

            props.add(builder(paramKey(POLevel.PRIMARY))
                    .defaultValue("2")
                    .subCategory(SUB_CATEGORY_LEVEL)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());
            props.add(builder(paramKey(POLevel.SECONDARY))
                    .defaultValue("4")
                    .subCategory(SUB_CATEGORY_LEVEL)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());

        }

        /* predicate complexity-specific params */
        {
            /**
             * <code>m_c[c]</code>
             */
            props.add(builder(paramKey(POComplexity.C))
                    .defaultValue("0.5")
                    .subCategory(SUB_CATEGORY_COMPLEXITY)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());
            /**
             * <code>m_c[p]</code>
             */
            props.add(builder(paramKey(POComplexity.P))
                    .defaultValue("0.5")
                    .subCategory(SUB_CATEGORY_COMPLEXITY)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());

            /**
             * <code>m_c[g]</code>
             */
            props.add(builder(paramKey(POComplexity.G))
                    .defaultValue("0.5")
                    .subCategory(SUB_CATEGORY_COMPLEXITY)
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build());
        }

        props.addAll(loadPredicates());
        props.add(builder(PARAM_EFFORT_PREDICATE_SCALE)
                .defaultValue("1")
                .category(CATEGORY)
                .subCategory("efforts.by.predicate")
                .type(PropertyType.FLOAT)
                .build());

        return props;
    }

    List<PropertyDefinition> loadPredicates() {
        final List<PropertyDefinition> props = new ArrayList<>();

        for (final PredicateType t : PredicateTypes.loadPredicates()) {
            final PropertyDefinition prop = PropertyDefinition.builder(t.key.toString())
                    .name(t.name)
                    .description(t.name)
                    .defaultValue(t.defaultValue.toString())
                    .subCategory("efforts.by.predicate")
                    .category(CATEGORY)
                    .type(PropertyType.FLOAT)
                    .build();

            props.add(prop);
            if (LOG.isDebugEnabled()) {
                LOG.debug("defined property:" + prop + "=" + prop.defaultValue());
            }
        }

        return props;

    }

}
