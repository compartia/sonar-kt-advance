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
package org.sonar.plugins.kt.advance.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

public class MapCounter<K> {
    private final Map<K, Double[]> map = new HashMap<>();
    private final int numberOfColumns;

    private final Map<Integer, String> columnNames = new HashMap<>();
    private final Map<String, Integer> columnNamesReverse = new HashMap<>();

    public MapCounter(int numberOfColumns) {
        super();
        this.numberOfColumns = numberOfColumns;
    }

    public Double get(K key, int col) {
        return map.get(key)[col];
    }

    public Double get(K key, int col, double defaultVal) {
        final Double[] val = map.get(key);

        if (null == val) {
            return defaultVal;
        } else {
            return val[col];
        }
    }

    public void inc(K key, int col, double inc) {
        Double[] value = map.get(key);
        if (value == null) {
            value = new Double[numberOfColumns];
            Arrays.fill(value, Double.valueOf(0));
            map.put(key, value);
        }
        value[col] += inc;
    }

    public void inc(K key, String col, double inc) {
        final Integer cname = columnNamesReverse.get(col);

        this.inc(key, cname, inc);
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public void setColumnName(Integer idx, String name) {
        this.columnNames.put(idx, name);
        this.columnNamesReverse.put(name, idx);
    }

    /**
     * [{"kt_pev_open":[0.1,1.0]},{"kt_pev_violated":[0.1,1.0]}]
     *
     * @return
     */
    public String toJson() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");

        final SortedSet<K> set = new TreeSet<>(keySet());
        final Iterator<K> iterator = set.iterator();
        while (iterator.hasNext()) {

            final K key = iterator.next();
            sb.append("{");

            sb.append("\"key\":");
            sb.append("\"").append(key).append("\",");
            sb.append("\"value\":");
            sb.append("[").append(StringUtils.join(map.get(key), ",")).append("]");

            sb.append("}");
            if (iterator.hasNext()) {
                sb.append(",");
            }

        }
        sb.append("]");
        return sb.toString();
    }

    public String toStringTable() {
        final StringBuilder sb = new StringBuilder();

        final SortedSet<K> set = new TreeSet<>(keySet());
        final Iterator<K> iterator = set.iterator();
        while (iterator.hasNext()) {

            final K key = iterator.next();

            sb.append(key).append("\t\t");

            sb.append(StringUtils.join(map.get(key), "\t"));

            if (iterator.hasNext()) {
                sb.append("\n");
            }

        }

        return sb.toString();
    }

}
