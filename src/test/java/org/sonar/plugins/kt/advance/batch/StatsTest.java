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

import org.junit.Test;
import org.sonar.plugins.kt.advance.KtMetrics.PredicateKey;
import org.sonar.plugins.kt.advance.util.MapCounter;

import com.kt.advance.api.Definitions.POLevel;
import com.kt.advance.api.Definitions.POStatus;
import com.kt.advance.api.Definitions.PredicateType;

public class StatsTest {

    /**
     *
     */
    @Test
    public void testMapCounter() {
        final int num = 5;
        final MapCounter<PredicateKey> mc = new MapCounter<>(4);

        final int metricColumn = Statistics.metricColumn(POStatus.open, POLevel.PRIMARY);
        final int metricColumn2 = Statistics.metricColumn(POStatus.open, POLevel.SECONDARY);
        final PredicateKey key = new PredicateKey(PredicateType._ab);
        final PredicateKey key2 = new PredicateKey(PredicateType._cbt);

        for (int f = 0; f < 5; f++) {
            mc.inc(key, metricColumn, 1.0);
            mc.inc(key, metricColumn2, 1.0);
            mc.inc(key2, metricColumn, 1.0);
            mc.inc(key2, metricColumn2, 1.0);
        }

        assertEquals(num, mc.get(key, metricColumn), 0.0001);

    }

}
