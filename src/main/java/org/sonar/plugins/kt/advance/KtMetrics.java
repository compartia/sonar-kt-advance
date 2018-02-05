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

import static kt.advance.model.Definitions.POLevel.PRIMARY;
import static kt.advance.model.Definitions.POLevel.SECONDARY;
import static kt.advance.model.Definitions.POStatus.dead;
import static kt.advance.model.Definitions.POStatus.discharged;
import static kt.advance.model.Definitions.POStatus.open;
import static kt.advance.model.Definitions.POStatus.violation;

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
//import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;

import com.google.common.base.Preconditions;

import kt.advance.model.Definitions.POLevel;
import kt.advance.model.Definitions.POStatus;
import kt.advance.model.PredicatesFactory.PredicateType;

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
            metricKey(PRIMARY, discharged),
            "Discharged Proof Obligations",
            Metric.ValueType.INT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_GENERAL)
                    .create();

    public static final Metric<Integer> METRIC_SPO_DISCHARGED = new Metric.Builder(
            metricKey(SECONDARY, discharged),
            "Discharged Proof Obligations",
            Metric.ValueType.INT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_GENERAL)
                    .create();

    public static final Metric<Integer> METRIC_PPO_OPEN = makeIssueCounterMetric(
        metricKey(PRIMARY, open), "Open Primary Proof Obligations");

    public static final Metric<Integer> METRIC_SPO_OPEN = makeIssueCounterMetric(
        metricKey(SECONDARY, open), "Open Secondary Proof Obligations");

    public static final Metric<Double> METRIC_PPO_DISCHARGED_PC = new Metric.Builder(
            metricKeyPc(PRIMARY, discharged),
            "% Of Discharged Primary Proof Obligations",
            Metric.ValueType.PERCENT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_ISSUES)
                    .create();

    public static final Metric<Double> METRIC_SPO_DISCHARGED_PC = new Metric.Builder(
            metricKeyPc(SECONDARY, discharged),
            "% Of Discharged Secondary Proof Obligations",
            Metric.ValueType.PERCENT)
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(CoreMetrics.DOMAIN_ISSUES)
                    .create();
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

    //---------------------
    private static final Logger LOG = Loggers.get(KtMetrics.class.getName());

    /**
     * per predicate metrics
     */
    private static final Map<String, Map<String, Metric<Integer>>> PER_PREDICATE_METRICS = new HashMap<>();
    static {
        createMetricsForPredicates(metricKey(PRIMARY, open), "OPN", "Open PPOs by",
            "Open PPOs by Predicate Type");

        createMetricsForPredicates(metricKey(PRIMARY, violation), "VL1", "Violations by",
            "Violations by Predicate Type");

        createMetricsForPredicates(metricKey(SECONDARY, open), "OPN", "Open SPOs by",
            "Open SPOs by Predicate Type");

        createMetricsForPredicates(metricKey(SECONDARY, violation), "VL2", "Secondary Violations by",
            "Secondary Violations by Predicate Type");

        createMetricsForPredicates(metricKey(PRIMARY, dead), "DEAD", "Dead-code PPOs by",
            "Dead-code PPOs by Predicate Type");

        createMetricsForPredicates(metricKey(SECONDARY, dead), "DEAD", "Dead-code SPOs by",
            "Dead-code SPOs by Predicate Type");
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

            makeIssuePercentMetric(metricKeyPc(PRIMARY, open), "% Of Open Primary Proof Obligations"),
            makeIssuePercentMetric(metricKeyPc(SECONDARY, open), "% Of Open Secondary Proof Obligations"),

            makeIssuePercentMetric(metricKeyPc(PRIMARY, violation), "% Of Primary Violations"),
            makeIssuePercentMetric(metricKeyPc(SECONDARY, violation), "% Of Secondary Violations"),

            makeIssuePercentMetric(metricKeyPc(PRIMARY, dead), "% Of PPO Dead-code"),
            makeIssuePercentMetric(metricKeyPc(SECONDARY, dead), "% Of SPO Dead-code"),

            makeIssueCounterMetric(metricKey(PRIMARY, violation), "Violations, Primary"),
            makeIssueCounterMetric(metricKey(SECONDARY, violation), "Violations, Secondary"),

            makeIssueCounterMetric(metricKey(PRIMARY, dead), "Dead-code, Primary"),
            makeIssueCounterMetric(metricKey(SECONDARY, dead), "Dead-code, Secondary"),

            METRIC_KT_PO_BY_PREDICATE_DISTR));

        for (final POLevel level : POLevel.values()) {
            for (final POComplexity c : POComplexity.values()) {

                final Metric<Float> complexityPerLineMetric = makeComplexityMetric(
                    compexityPerLineMetricKey(level, c),
                    level.key().toUpperCase() + " " + c.name() + "-complexity per line");

                allMetrics.add(complexityPerLineMetric);

                final Metric<Float> complexityMetric = makeComplexityMetric(
                    compexityMetricKey(level, c),
                    level.key().toUpperCase() + " " + c.name() + "-complexity");

                allMetrics.add(complexityMetric);
            }

        }

        allMetrics.addAll(getMetricsForPredicates(metricKey(PRIMARY, open)).values());
        allMetrics.addAll(getMetricsForPredicates(metricKey(PRIMARY, violation)).values());

        allMetrics.addAll(getMetricsForPredicates(metricKey(SECONDARY, open)).values());
        allMetrics.addAll(getMetricsForPredicates(metricKey(SECONDARY, violation)).values());

        for (final Metric<?> m : allMetrics) {
            final String metricKey = m.getKey();
            Preconditions.checkArgument(!allMetricsMap.containsKey(metricKey));
            allMetricsMap.put(metricKey, m);
            LOG.debug("Registered metric: " + m);
        }
    }

    public static String compexityMetricKey(POLevel level, POComplexity c) {
        return join(PREFIX, level.key(), COMPLEXITY, c.key());
    }

    public static String compexityPerLineMetricKey(POLevel level, POComplexity c) {
        return join(PREFIX, level.key(), COMPLEXITY, "per_line", c.key());
    }

    @SuppressWarnings("unchecked")
    public static Metric<Float> getFloatMetric(String key) {
        return (Metric<Float>) allMetricsMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public static Metric<Integer> getIntMetric(String key) {
        return (Metric<Integer>) allMetricsMap.get(key);
    }

    public static Metric<?> getMetric(String key) {
        return allMetricsMap.get(key);
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

    public static String metricKey(POLevel level, POStatus state) {
        return join(PREFIX, level.key(), state.name());
    }

    public static String metricKeyPc(POLevel level, POStatus state) {
        return join(PREFIX, level.key(), state.name(), PCT);
    }

    public static Metric<Integer> predicateMetric(String metricKey, PredicateType predicateType) {
        try {
            return getMetricsForPredicates(metricKey)
                    .get(joinKey(metricKey, predicateType.name()));
        } catch (final NullPointerException ex) {
            throw new IllegalArgumentException(metricKey + " " + predicateType, ex);
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

        Preconditions.checkArgument(!pm.containsKey(metricKey));

        for (final PredicateType pd : PredicateType
                .values()) {

            final String key = joinKey(metricKey, pd.name());
            final Metric<Integer> metric = new Metric.Builder(
                    key,
                    "(" + namePrefix + ") " + pd.label,
                    Metric.ValueType.INT)
                            .setDescription("(" + descrPrefix + ") " + pd.label)
                            .setDirection(Metric.DIRECTION_WORST)
                            .setQualitative(true)
                            .setDomain(domain)
                            //.setHidden(true)
                            .create();

            Preconditions.checkArgument(!pm.containsKey(key));
            pm.put(key, metric);
        }
        return pm;
    }

    @Override
    public List<Metric> getMetrics() {
        return allMetrics;
    }

}
