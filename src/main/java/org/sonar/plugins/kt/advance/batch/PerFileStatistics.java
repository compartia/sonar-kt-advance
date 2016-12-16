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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sonar.api.batch.fs.InputFile;

import com.google.common.base.Preconditions;

public class PerFileStatistics {

    public static class ByFileMetrics {
        public int numberOfLines;
    }

    private final Map<InputFile, ByFileMetrics> stats = new HashMap<>();

    public Collection<InputFile> getResources() {
        return stats.keySet();
    }

    public int getTotalNumberOfLines() {
        int summ = 0;
        for (final ByFileMetrics fm : stats.values()) {
            summ += fm.numberOfLines;
        }

        return summ;
    }

    public void handle(InputFile file) {

        Preconditions.checkNotNull(file);

        final InputFile key = file;
        if (stats.containsKey(key)) {
            return;
        } else {
            final ByFileMetrics s = new ByFileMetrics();
            stats.put(key, s);
            s.numberOfLines = file.lines();
        }
    }
}
