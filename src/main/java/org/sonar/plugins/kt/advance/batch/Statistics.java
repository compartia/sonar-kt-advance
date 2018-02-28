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

import static org.sonar.plugins.kt.advance.KtMetrics.METRIC_KT_PO_BY_PREDICATE_DISTR;
import static org.sonar.plugins.kt.advance.KtMetrics.compexityMetricKey;
import static org.sonar.plugins.kt.advance.KtMetrics.compexityPerLineMetricKey;
import static org.sonar.plugins.kt.advance.KtMetrics.getFloatMetric;
import static org.sonar.plugins.kt.advance.KtMetrics.getIntMetric;
import static org.sonar.plugins.kt.advance.KtMetrics.getMetricsMap;
import static org.sonar.plugins.kt.advance.KtMetrics.metricKey;
import static org.sonar.plugins.kt.advance.KtMetrics.metricKeyPc;
import static org.sonar.plugins.kt.advance.KtMetrics.predicateMetric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.KtMetrics.PredicateKey;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POComplexity;
import org.sonar.plugins.kt.advance.util.MapCounter;

import com.google.common.base.Preconditions;
import com.kt.advance.api.CApplication;
import com.kt.advance.api.CFile;
import com.kt.advance.api.Definitions.POLevel;
import com.kt.advance.api.Definitions.POStatus;
import com.kt.advance.api.PO;

public class Statistics {

    public enum Col {
        PRIMARY_OPEN, PRIMARY_VIOLATION, SECONDARY_OPEN, SECONDARY_VIOLATION;
    }

    interface DoubleMeasureHolder extends MeasureHolder<Double> {
        DoubleMeasureHolder inc();

        DoubleMeasureHolder inc(Number val);

        DoubleMeasureHolder setValue(Number newVal);

        Double value();
    }

    static class MapMeasureHolderImpl implements MeasureHolder<Serializable> {

        private final Metric<?> metric;
        final MapCounter<PredicateKey> counter;

        public MapMeasureHolderImpl(Metric<?> metric, int columns) {
            this.metric = metric;
            counter = new MapCounter<>(columns);
        }

        @Override
        public Measure<Serializable> asMeasure() {
            final Measure<Serializable> measure = new Measure<>(metric, 0d);
            measure.setData(counter.toJson());
            return measure;
        }

        @Override
        public InputFile getResource() {
            return null;
        }

        public void inc(PredicateKey key, int column) {
            inc(key, column, 1d);
        }

        public void inc(PredicateKey key, int column, double val) {
            counter.inc(key, column, val);
        }

        @Override
        public String key() {
            return metric.getKey();
        }

    }

    interface MeasureHolder<T extends Serializable> {

        static String key(InputFile resource, Metric<?> metric) {
            Preconditions.checkNotNull(metric, "Metric is required");
            return resource == null ? metric.getKey() : (resource.absolutePath() + ":" + metric.getKey());
        }

        Measure<T> asMeasure();

        InputFile getResource();

        String key();

    }

    static class MeasureHolderImpl implements DoubleMeasureHolder {
        private final Measure<Double> measure;
        private final Metric<?> metric;
        private final InputFile resource;

        public MeasureHolderImpl(Metric<? extends Serializable> metric, InputFile resource) {
            this.metric = metric;
            this.resource = resource;
            this.measure = new Measure<>(metric, 0d);
        }

        private static double zeroIfNull(Number n) {
            if (n == null) {
                return 0;
            }
            return n.doubleValue();
        }

        @Override
        public Measure<Double> asMeasure() {
            return getMeasure();
        }

        @Override
        public InputFile getResource() {
            return resource;
        }

        @Override
        public DoubleMeasureHolder inc() {
            inc(1D);
            return this;
        }

        @Override
        public DoubleMeasureHolder inc(Number val) {
            getMeasure().setValue(getMeasure().getValue() + zeroIfNull(val));
            return this;
        }

        @Override
        public String key() {
            return MeasureHolder.key(getResource(), getMetric());
        }

        @Override
        public DoubleMeasureHolder setValue(Number newVal) {
            getMeasure().setValue(zeroIfNull(newVal));
            return this;
        }

        @Override
        public String toString() {
            return "MeasureHolderImpl [metric=" + metric.getKey() + ", measure=" + measure.getValue() + ", resource="
                    + resource + "]";
        }

        @Override
        public Double value() {
            return measure.getValue();
        }

        private Measure<Double> getMeasure() {
            return measure;
        }

        private Metric<?> getMetric() {
            return metric;
        }
    }

    private static final Logger LOG = Loggers.get(Statistics.class.getName());

    final static String MEASURE_PATTERN = "measure saved: %s \t\t %s \t\t %s \t\t%s";

    private final Map<String, MeasureHolder<? extends Serializable>> measures = new HashMap<>();

    final MapMeasureHolderImpl perPredicateMetrics;

    private final PerFileStatistics perFileStatistics = new PerFileStatistics();

    public Statistics() {
        super();

        perPredicateMetrics = new MapMeasureHolderImpl(METRIC_KT_PO_BY_PREDICATE_DISTR, 4);
        measures.put(perPredicateMetrics.key(), perPredicateMetrics);
    }

    public static int metricColumn(POStatus state, final POLevel level) {
        final String colName = (level.name() + "_" + state.name()).toUpperCase();
        return Col.valueOf(colName).ordinal();
    }

    public static double percentage(double p, double t) {
        return t > 0 ? (100.0 * p / t) : 0;
    }

    private static Metric<Integer> getPerPredicateMetric(String metricKey, PredicateKey predicateKey) {
        final Metric<Integer> predicateMetric = predicateMetric(metricKey, predicateKey);
        Preconditions.checkNotNull(predicateMetric, "no metric found for " + metricKey + "  type:" + predicateKey);
        return predicateMetric;
    }

    public <X extends PO> X handle(X ipo, CApplication app, CFile cfile, SonarResourceLocator rl) {

        final InputFile resource = rl.getResource(app, cfile);//  fileSystem.getResource(ipo.getLocation().file);
        /**
         * per project
         */
        handle(ipo, null);

        /**
         * per resource
         */
        handle(ipo, resource);
        perFileStatistics.handle(resource);
        return ipo;

    }

    public void save(SensorContext sensorContext) {

        /**
         * calculate percentage
         */
        calcPercentage(null);
        for (final InputFile inf : perFileStatistics.getResources()) {
            calcPercentage(inf);
        }

        /**
         * saving all measures;
         */
        for (final MeasureHolder<?> measure : measures.values()) {
            saveMeasure(measure.asMeasure(), measure.getResource(), sensorContext);
        }

        final int totalLines = perFileStatistics.getTotalNumberOfLines();
        LOG.info("Total number of lines: " + totalLines);

        for (final POComplexity c : POComplexity.values()) {
            for (final POLevel l : POLevel.values()) {
                saveComplexityPerLineMeasure(sensorContext, totalLines,
                    getFloatMetric(compexityPerLineMetricKey(l, c)),
                    getIntMetric(compexityMetricKey(l, c)));
            }
        }

        //        saveComplexityPerLineMeasure(sensorContext, totalLines,
        //            METRIC_PPO_COMPLEXITY_PER_LINE_C,
        //            METRIC_PPO_COMPLEXITY_C);
        //        saveComplexityPerLineMeasure(sensorContext, totalLines,
        //            METRIC_PPO_COMPLEXITY_PER_LINE_P,
        //            METRIC_PPO_COMPLEXITY_P);
        //        saveComplexityPerLineMeasure(sensorContext, totalLines,
        //            METRIC_PPO_COMPLEXITY_PER_LINE_G,
        //            METRIC_PPO_COMPLEXITY_G);
        //
        //        saveComplexityPerLineMeasure(sensorContext, totalLines,
        //            METRIC_SPO_COMPLEXITY_PER_LINE_C,
        //            METRIC_SPO_COMPLEXITY_C);
        //        saveComplexityPerLineMeasure(sensorContext, totalLines,
        //            METRIC_SPO_COMPLEXITY_PER_LINE_P,
        //            METRIC_SPO_COMPLEXITY_P);
        //        saveComplexityPerLineMeasure(sensorContext, totalLines,
        //            METRIC_SPO_COMPLEXITY_PER_LINE_G,
        //            METRIC_SPO_COMPLEXITY_G);

        //
        saveNcLocMeasure(sensorContext);

    }

    private void handle(PO ipo, InputFile scope) {

        getOrCreateMeasure(metricKey(ipo.getLevel()), scope).inc();
        final String stateMetricKey = metricKey(ipo.getLevel(), ipo.getStatus());

        getOrCreateMeasure(stateMetricKey, scope).inc();

        if (!ipo.isSafe()) {

            //by predicate
            final PredicateKey predicateKey = new PredicateKey(ipo.getPredicate().type);
            getOrCreateMeasure(stateMetricKey, predicateKey, scope).inc();

            if (scope == null) {
                perPredicateMetrics.inc(predicateKey, metricColumn(ipo.getStatus(), ipo.getLevel()));
            }

        }

        //complexities XXX:
        //        if (null != ipo.getComplexityC()) {
        //            getOrCreateMeasure(compexityMetricKey(ipo.getLevel(), POComplexity.C), scope)
        //                    .inc(ipo.getComplexityC());
        //        }
        //        if (null != ipo.getComplexityP()) {
        //            getOrCreateMeasure(compexityMetricKey(ipo.getLevel(), POComplexity.P), scope)
        //                    .inc(ipo.getComplexityP());
        //        }
        //        if (null != ipo.getComplexityG()) {
        //            getOrCreateMeasure(compexityMetricKey(ipo.getLevel(), POComplexity.G), scope)
        //                    .inc(ipo.getComplexityG());
        //        }

    }

    private Measure<Serializable> makePerLineComplexityMeasure(
            Metric<Float> destMetric,
            Metric<Integer> complexityMeasure,
            InputFile resource,
            int totalLines) {

        final DoubleMeasureHolder m = getMeasure(complexityMeasure, resource);
        if (null != m && totalLines > 0) {
            final Integer c = m.asMeasure().getIntValue();
            final double cPerLine = c / (double) totalLines;
            return new Measure<>(destMetric, cPerLine);
        }
        return null;
    }

    void calcPercentage(final InputFile scope) {

        for (final POLevel l : POLevel.values()) {
            final DoubleMeasureHolder totalPerLevel = getOrCreateMeasure(metricKey(l), scope);

            for (final POStatus s : POStatus.values()) {
                final DoubleMeasureHolder totalPerLevelState = getOrCreateMeasure(metricKey(l, s), scope);

                getOrCreateMeasure(metricKeyPc(l, s), scope)
                        .setValue(percentage(totalPerLevelState.value(), totalPerLevel.value()));
            }
        }
    }

    DoubleMeasureHolder getMeasure(Metric<? extends Serializable> metric, InputFile resource) {
        final String key = MeasureHolder.key(resource, metric);
        final MeasureHolder<?> measure = measures.get(key);

        return (DoubleMeasureHolder) measure;
    }

    DoubleMeasureHolder getOrCreateMeasure(Metric<? extends Serializable> metric, InputFile resource) {
        final String key = MeasureHolder.key(resource, metric);
        MeasureHolder<?> measure = measures.get(key);

        if (measure == null) {
            measure = new MeasureHolderImpl(metric, resource);
            measures.put(key, measure);
        }

        return (DoubleMeasureHolder) measure;
    }

    DoubleMeasureHolder getOrCreateMeasure(String metricKey, InputFile resource) {
        final Metric<?> metric = getMetricsMap().get(metricKey);
        Preconditions.checkNotNull(metric, "No metric for key {1}", metricKey);
        return getOrCreateMeasure(metric, resource);
    }

    DoubleMeasureHolder getOrCreateMeasure(String metricKey, PredicateKey predicateTag, InputFile scope) {
        Preconditions.checkNotNull(predicateTag);
        final Metric<Integer> metric = getPerPredicateMetric(metricKey, predicateTag);
        return getOrCreateMeasure(metric, scope);
    }

    void saveComplexityPerLineMeasure(SensorContext sensorContext, final int totalLines, final Metric<Float> dst,
            final Metric<Integer> src) {

        final Measure<Serializable> perLineComplexityMeasure = makePerLineComplexityMeasure(
            dst,
            src,
            null,
            totalLines);

        saveMeasure(
            perLineComplexityMeasure,
            null, sensorContext);

        for (final InputFile inf : perFileStatistics.getResources()) {
            final Measure<Serializable> perFileLineComplexityMeasure = makePerLineComplexityMeasure(
                dst,
                src,
                inf,
                inf.lines());

            saveMeasure(
                perFileLineComplexityMeasure,
                inf, sensorContext);

        }
    }

    void saveMeasure(Measure<?> measure, InputFile resource, SensorContext sensorContext) {
        if (null == measure) {
            return;
        }

        if (null == resource) {
            LOG.info(String.format(MEASURE_PATTERN, "/", measure.getMetricKey(), measure.getValue(),
                measure.hasData() ? measure.getData() : ""));
            sensorContext.saveMeasure(measure);
        } else {
            LOG.info(String.format(MEASURE_PATTERN, resource.absolutePath(), measure.getMetricKey(), measure.getValue(),
                measure.hasData() ? measure.getData() : ""));
            sensorContext.saveMeasure(resource, measure);
        }
    }

    void saveNcLocMeasure(SensorContext sensorContext) {

        for (final InputFile inf : perFileStatistics.getResources()) {
            final DoubleMeasureHolder holder = getOrCreateMeasure(CoreMetrics.NCLOC, inf);
            holder.setValue(inf.lines());

            saveMeasure(
                holder.asMeasure(),
                inf, sensorContext);

        }
    }

}
