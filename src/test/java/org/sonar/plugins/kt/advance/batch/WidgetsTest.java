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

import static org.junit.Assert.assertNotEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.sonar.plugins.kt.advance.ui.AdvanceBarChartsWidget;
import org.sonar.plugins.kt.advance.ui.KtAdvanceWidget;

@Ignore
public class WidgetsTest {

    @Test
    public void testIdsAreDifferent() {
        final AdvanceBarChartsWidget w1 = new AdvanceBarChartsWidget();
        final KtAdvanceWidget w2 = new KtAdvanceWidget();

        assertNotEquals(w1.getId(), w2.getId());
        assertNotEquals(w1.getTitle(), w2.getTitle());

        assertNotEquals(w1.getTemplate(), w2.getTemplate());

    }

}
