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

import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POComplexity.C;
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POComplexity.G;
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POComplexity.P;
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POLevel.PRIMARY;
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POLevel.SECONDARY;
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState.DISCHARGED;
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState.OPEN;
import static org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState.VIOLATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POComplexity;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POLevel;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState;
import org.sonar.plugins.kt.advance.batch.PredicateTypes;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateType;

import com.google.common.base.Preconditions;

public final class KtMetrics implements Metrics {

    public static final String COMPLEXITY = "complexity";

    private static final String PCT = "pc";

    public static final String PREFIX = "kt";

    private static final String KT_PER_PREDICATE_DISTR = PREFIX + "_per_predicate_distr";

    public static final Metric<String> METRIC_KT_PO_BY_PREDICATE_DISTR = new Metric.Builder(
            KT_PER_PREDICATE_DISTR,
            "PO Distribution by predicate tag",
            Metric.ValueType.DATA)
                    .setDirection(Metric.DIRECTION_NONE)
                    .setDomain(CoreMetrics.DOMAIN_GENERAL)
                    .create();

    public static final Metric<Integer> METRIC_PPO_DISCHARGED = new Metric.Builder(
            metricKey(PRIMARY, DISCHARGED),
            "Discharged Proof Obligations",
            Metric.ValueType.INT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_GENERAL)
                    .create();

    public static final Metric<Integer> METRIC_SPO_DISCHARGED = new Metric.Builder(
            metricKey(SECONDARY, DISCHARGED),
            "Discharged Proof Obligations",
            Metric.ValueType.INT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_GENERAL)
                    .create();

    public static final Metric<Integer> METRIC_PPO_OPEN = makeIssueCounterMetric(
        metricKey(PRIMARY, OPEN), "Open Primary Proof Obligations");

    public static final Metric<Integer> METRIC_SPO_OPEN = makeIssueCounterMetric(
        metricKey(SECONDARY, OPEN), "Open Secondary Proof Obligations");

    public static final Metric<Double> METRIC_PPO_OPEN_PC = makeIssuePercentMetric(
        metricKeyPc(PRIMARY, OPEN), "% Of Open Primary Proof Obligations");

    public static final Metric<Double> METRIC_SPO_OPEN_PC = makeIssuePercentMetric(
        metricKeyPc(SECONDARY, OPEN), "% Of Open Secondary Proof Obligations");

    public static final Metric<Double> METRIC_PPO_DISCHARGED_PC = new Metric.Builder(
            metricKeyPc(PRIMARY, DISCHARGED),
            "% Of Discharged Primary Proof Obligations",
            Metric.ValueType.PERCENT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_ISSUES)
                    .create();

    public static final Metric<Double> METRIC_SPO_DISCHARGED_PC = new Metric.Builder(
            metricKeyPc(SECONDARY, DISCHARGED),
            "% Of Discharged Secondary Proof Obligations",
            Metric.ValueType.PERCENT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_ISSUES)
                    .create();
    public static final Metric<Integer> METRIC_PPO_VIOLATIONS = makeIssueCounterMetric(
        metricKey(PRIMARY, VIOLATION), "Violations, Primary");

    public static final Metric<Double> METRIC_PPO_VIOLATIONS_PC = makeIssuePercentMetric(
        metricKeyPc(PRIMARY, VIOLATION), "% Of Primary Violations");

    public static final Metric<Double> METRIC_SPO_VIOLATIONS_PC = makeIssuePercentMetric(
        metricKeyPc(SECONDARY, VIOLATION), "% Of Secondary Violations");

    public static final Metric<Integer> METRIC_SPO_VIOLATIONS = makeIssueCounterMetric(
        metricKey(SECONDARY, VIOLATION), "Violations, Secondary");

    public static final Metric<Integer> METRIC_PPO = new Metric.Builder(
            metricKey(PRIMARY),
            "Primary Proof Obligations",
            Metric.ValueType.INT)
                    .setDirection(Metric.DIRECTION_NONE)
                    .setQualitative(false)
                    .setDomain(CoreMetrics.DOMAIN_GENERAL)
                    .create();
    public static final Metric<Integer> METRIC_SPO = new Metric.Builder(
            metricKey(SECONDARY),
            "Secondary Proof Obligations",
            Metric.ValueType.INT)
                    .setDirection(Metric.DIRECTION_NONE)
                    .setQualitative(false)
                    .setDomain(CoreMetrics.DOMAIN_GENERAL)
                    .create();

    public static final Metric<Float> METRIC_PPO_COMPLEXITY_PER_LINE_C = makeComplexityMetric(
        compexityPerLineMetricKey(PRIMARY, C), "PPO C-complexity per line");
    public static final Metric<Float> METRIC_PPO_COMPLEXITY_PER_LINE_P = makeComplexityMetric(
        compexityPerLineMetricKey(PRIMARY, P), "PPO P-complexity per line");
    public static final Metric<Float> METRIC_PPO_COMPLEXITY_PER_LINE_G = makeComplexityMetric(
        compexityPerLineMetricKey(PRIMARY, G), "PPO G-complexity per line");

    public static final Metric<Float> METRIC_SPO_COMPLEXITY_PER_LINE_C = makeComplexityMetric(
        compexityPerLineMetricKey(SECONDARY, C), "SPO C-complexity per line");
    public static final Metric<Float> METRIC_SPO_COMPLEXITY_PER_LINE_P = makeComplexityMetric(
        compexityPerLineMetricKey(SECONDARY, P), "SPO P-complexity per line");
    public static final Metric<Float> METRIC_SPO_COMPLEXITY_PER_LINE_G = makeComplexityMetric(
        compexityPerLineMetricKey(SECONDARY, G), "SPO G-complexity per line");

    //primary
    public static final Metric<Integer> METRIC_PPO_COMPLEXITY_C = makeComplexityIntMetric(
        compexityMetricKey(PRIMARY, C), "PPO C-complexity");
    public static final Metric<Integer> METRIC_PPO_COMPLEXITY_P = makeComplexityIntMetric(
        compexityMetricKey(PRIMARY, P), "PPO P-complexity");
    public static final Metric<Integer> METRIC_PPO_COMPLEXITY_G = makeComplexityIntMetric(
        compexityMetricKey(PRIMARY, G), "PPO G-complexity");

    //secondary:
    public static final Metric<Integer> METRIC_SPO_COMPLEXITY_C = makeComplexityIntMetric(
        compexityMetricKey(SECONDARY, C), "SPO C-complexity");
    public static final Metric<Integer> METRIC_SPO_COMPLEXITY_P = makeComplexityIntMetric(
        compexityMetricKey(SECONDARY, P), "SPO P-complexity");
    public static final Metric<Integer> METRIC_SPO_COMPLEXITY_G = makeComplexityIntMetric(
        compexityMetricKey(SECONDARY, G), "SPO G-complexity");
    //---------------------
    private static final Logger LOG = Loggers.get(KtMetrics.class.getName());

    /**
     * per predicate metrics
     */
    private static final Map<String, Map<String, Metric<Integer>>> PER_PREDICATE_METRICS = new HashMap<>();
    static {
        createMetricsForPredicates(metricKey(PRIMARY, OPEN), "OPN", "Open PPOs by",
            "Open PPOs by Predicate Type");

        createMetricsForPredicates(metricKey(PRIMARY, VIOLATION), "VL1", "Violations by",
            "Violations by Predicate Type");

        createMetricsForPredicates(metricKey(SECONDARY, OPEN), "OPN", "Open SPOs by",
            "Open SPOs by Predicate Type");

        createMetricsForPredicates(metricKey(SECONDARY, VIOLATION), "VL2", "Secondary Violations by",
            "Secondary Violations by Predicate Type");
    }

    static final List<Metric> allMetrics = new ArrayList<>();
    static final Map<String, Metric<?>> allMetricsMap = new HashMap<>();

    static {

        allMetrics.addAll(Arrays.asList(
            METRIC_PPO,
            METRIC_SPO,
            METRIC_PPO_OPEN,
            METRIC_SPO_OPEN,

            METRIC_PPO_DISCHARGED,
            METRIC_PPO_DISCHARGED_PC,

            METRIC_SPO_DISCHARGED,
            METRIC_SPO_DISCHARGED_PC,

            METRIC_PPO_OPEN_PC,
            METRIC_SPO_OPEN_PC,

            METRIC_PPO_VIOLATIONS_PC,
            METRIC_SPO_VIOLATIONS_PC,

            METRIC_PPO_VIOLATIONS,
            METRIC_SPO_VIOLATIONS,

            METRIC_PPO_COMPLEXITY_C,
            METRIC_PPO_COMPLEXITY_P,
            METRIC_PPO_COMPLEXITY_G,

            METRIC_SPO_COMPLEXITY_C,
            METRIC_SPO_COMPLEXITY_P,
            METRIC_SPO_COMPLEXITY_G,

            METRIC_PPO_COMPLEXITY_PER_LINE_C,
            METRIC_PPO_COMPLEXITY_PER_LINE_P,
            METRIC_PPO_COMPLEXITY_PER_LINE_G,

            METRIC_SPO_COMPLEXITY_PER_LINE_C,
            METRIC_SPO_COMPLEXITY_PER_LINE_P,
            METRIC_SPO_COMPLEXITY_PER_LINE_G,

            METRIC_KT_PO_BY_PREDICATE_DISTR));

        allMetrics.addAll(getMetricsForPredicates(metricKey(PRIMARY, OPEN)).values());
        allMetrics.addAll(getMetricsForPredicates(metricKey(PRIMARY, VIOLATION)).values());

        allMetrics.addAll(getMetricsForPredicates(metricKey(SECONDARY, OPEN)).values());
        allMetrics.addAll(getMetricsForPredicates(metricKey(SECONDARY, VIOLATION)).values());

        for (final Metric<?> m : allMetrics) {
            allMetricsMap.put(m.getKey(), m);
            LOG.debug("Registered metric: " + m);
        }
    }

    public static String compexityMetricKey(POLevel level, POComplexity c) {
        return join(PREFIX, level.key(), COMPLEXITY, c.key());
    }

    public static String compexityPerLineMetricKey(POLevel level, POComplexity c) {
        return join(PREFIX, level.key(), COMPLEXITY, "per_line", c.key());
    }

    public static Map<String, Metric<Integer>> getMetricsForPredicates(String metricKey) {
        final Map<String, Metric<Integer>> map = PER_PREDICATE_METRICS.get(metricKey);
        Preconditions.checkNotNull(map,
            "No map for metric " + metricKey + " have only " + PER_PREDICATE_METRICS.keySet());
        return map;
    }

    public static Map<String, Metric<?>> getMetricsMap() {
        return allMetricsMap;
    }

    public static String join(String... components) {
        return StringUtils.join(components, "_");
    }

    public static String joinKey(String keyPrefix, String k) {
        return new StringBuilder()
                .append(keyPrefix)
                .append('_')
                .append(k).toString();
    }

    public static final Metric<Integer> makeComplexityIntMetric(String key, String name) {
        return new Metric.Builder(
                key,
                name,
                Metric.ValueType.INT)
                        .setDirection(Metric.DIRECTION_WORST)
                        .setQualitative(true)
                        .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
                        .create();
    }

    public static final Metric<Float> makeComplexityMetric(String key, String name) {
        return new Metric.Builder(
                key,
                name,
                Metric.ValueType.FLOAT)
                        .setDirection(Metric.DIRECTION_WORST)
                        .setQualitative(true)
                        .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
                        .create();
    }

    public static String metricKey(POLevel level) {
        return join(PREFIX, level.key(), "");
    }

    public static String metricKey(POLevel level, POState state) {
        return join(PREFIX, level.key(), state.key());
    }

    public static String metricKeyPc(POLevel level, POState state) {
        return join(PREFIX, level.key(), state.key(), PCT);
    }

    public static Metric<Integer> predicateMetric(String metricKey, PredicateKey predicateKey) {
        try {
            return getMetricsForPredicates(metricKey)
                    .get(joinKey(metricKey, predicateKey.toString()));
        } catch (final NullPointerException ex) {
            throw new IllegalArgumentException(metricKey + " " + predicateKey, ex);
        }
    }

    private static final Metric<Integer> makeIssueCounterMetric(String key, String name) {
        return new Metric.Builder(
                key,
                name,
                Metric.ValueType.INT)
                        .setDirection(Metric.DIRECTION_WORST)
                        .setQualitative(true)
                        .setBestValue(0D)
                        .setDomain(CoreMetrics.DOMAIN_ISSUES)
                        .create();
    }

    private static final Metric<Double> makeIssuePercentMetric(String key, String name) {
        return new Metric.Builder(
                key,
                name,
                Metric.ValueType.PERCENT)
                        .setDirection(Metric.DIRECTION_WORST)
                        .setQualitative(true)
                        .setBestValue(0D)
                        .setDomain(CoreMetrics.DOMAIN_ISSUES)
                        .create();
    }

    static Map<String, Metric<Integer>> createMetricsForPredicates(String metricKey, String namePrefix,
            String descrPrefix, String domain) {

        final Map<String, Metric<Integer>> pm = new HashMap<>();
        PER_PREDICATE_METRICS.put(metricKey, pm);

        for (final PredicateType pd : PredicateTypes.loadPredicates()) {

            final String key = joinKey(metricKey, pd.key.toString());
            final Metric<Integer> metric = new Metric.Builder(
                    key,
                    "(" + namePrefix + ") " + pd.name,
                    Metric.ValueType.INT)
                            .setDescription("(" + descrPrefix + ") " + pd.name)
                            .setDirection(Metric.DIRECTION_WORST)
                            .setQualitative(true)
                            .setDomain(domain)
                            //.setHidden(true)
                            .create();

            pm.put(key, metric);
        }
        return pm;
    }

    @Override
    public List<Metric> getMetrics() {
        return allMetrics;
    }

}
