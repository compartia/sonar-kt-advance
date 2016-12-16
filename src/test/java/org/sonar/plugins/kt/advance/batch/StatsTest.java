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
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.kt.advance.KtMetrics.METRIC_KT_PO_BY_PREDICATE_DISTR;
import static org.sonar.plugins.kt.advance.KtMetrics.METRIC_PPO_OPEN;
import static org.sonar.plugins.kt.advance.KtMetrics.METRIC_PPO_VIOLATIONS;
import static org.sonar.plugins.kt.advance.KtMetrics.predicateMetric;

import java.net.URL;

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;
import org.sonar.plugins.kt.advance.batch.Statistics.DoubleMeasureHolder;
import org.sonar.plugins.kt.advance.batch.Statistics.MapMeasureHolderImpl;

public class StatsTest {

    private static final PredicateKey PREDICATE_KEY_ALLOC_BASE = new PredicateKey("allocation-base");

    @BeforeClass
    public static void setup() throws JAXBException {
        final URL url = StatsTest.class.getResource("/test_project");

    }

    @Test
    public void testMapMeasureHolderImpl() {
        final Statistics.MapMeasureHolderImpl holder = new MapMeasureHolderImpl(
                METRIC_KT_PO_BY_PREDICATE_DISTR, 2);

        holder.inc(new PredicateKey("kt_pev_open_pc"), 0, 1);
        holder.inc(new PredicateKey("kt_ppo_complexity"), 0, 1);

        final String expected = "[{\"key\":\"predicate_kt_pev_open_pc\",\"value\":[1.0,0.0]},{\"key\":\"predicate_kt_ppo_complexity\",\"value\":[1.0,0.0]}]";
        assertEquals(expected, holder.asMeasure().getData());
    }

    @Test
    public void testStats_getOrCreateMeasure() throws JAXBException {

        final FsAbstraction fs = mock(FsAbstraction.class);
        final Statistics stats = new Statistics(fs);

        final DoubleMeasureHolder m1 = stats.getOrCreateMeasure(METRIC_PPO_VIOLATIONS, mock(InputFile.class));
        final DoubleMeasureHolder m2 = stats.getOrCreateMeasure(METRIC_PPO_VIOLATIONS, null);

        assertNotEquals(m1, m2);
        final String key1 = m1.key();
        final String key2 = m2.key();
        assertNotEquals(key1, key2);

        final String metricKey1 = m1.asMeasure().getMetricKey();
        final String metricKey2 = m2.asMeasure().getMetricKey();
        assertEquals(metricKey1, metricKey2);
    }

    @Test
    public void testStatsCountViolations() throws JAXBException {
        final FsAbstraction fs = mock(FsAbstraction.class);
        final Statistics stats = new Statistics(fs);
        final PredicateKey predicateType = PREDICATE_KEY_ALLOC_BASE;

        final InputFile inputFileMock = mock(InputFile.class);
        when(fs.getResource(any())).thenReturn(inputFileMock);
        final IssuableProofObligation ipo = Factory.createViolatedPO(predicateType, inputFileMock);
        //--------------
        stats.handle(ipo);

        final double violations = stats.getOrCreateMeasure(METRIC_PPO_VIOLATIONS, inputFileMock).value();
        final double open = stats.getOrCreateMeasure(METRIC_PPO_OPEN, inputFileMock).value();

        assertEquals(violations, 1.0, 0.1);
        assertEquals(open, 0.0, 0.1);

        final Metric<Integer> violationsMetric = predicateMetric(METRIC_PPO_VIOLATIONS.key(), predicateType);
        assertEquals(1.0, stats.getOrCreateMeasure(violationsMetric, inputFileMock).value(), 0.000001);
        assertEquals(1.0, stats.getOrCreateMeasure(violationsMetric, null).value(), 0.000001);

        stats.save(mock(SensorContext.class));
    }

    @Test
    public void testStatsSave() {

        final SensorContext sensorContext = mock(SensorContext.class);
        final FsAbstraction fs = mock(FsAbstraction.class);
        final Statistics stats = new Statistics(fs);
        final PredicateKey predicateType = PREDICATE_KEY_ALLOC_BASE;
        final InputFile inputFile = mock(InputFile.class);
        final IssuableProofObligation ipo = Factory.createViolatedPO(predicateType, inputFile);
        when(fs.getResource(any())).thenReturn(inputFile);
        when(inputFile.lines()).thenReturn(10);
        stats.handle(ipo);

        stats.getOrCreateMeasure(METRIC_KT_PO_BY_PREDICATE_DISTR, inputFile);

        stats.save(sensorContext);

        verify(sensorContext, times(1)).saveMeasure(inputFile,
            new Measure<>(METRIC_PPO_VIOLATIONS));
        verify(sensorContext, times(1)).saveMeasure(
            new Measure<>(METRIC_PPO_VIOLATIONS));

        verify(sensorContext, times(1)).saveMeasure(inputFile,
            new Measure<>(predicateMetric(METRIC_PPO_VIOLATIONS.key(), predicateType)));

    }

}
