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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.model.GoodForCache;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class PredicateTypes {
    public static class PredicateKey implements Comparable<PredicateKey>, GoodForCache {

        private static final long serialVersionUID = -5866248534907435282L;

        public static final PredicateKey UNKNOWN = new PredicateKey("");

        private final String key;
        private final String tag;

        public PredicateKey(String tag) {
            this.tag = tag;
            this.key = "predicate_" + tag.replace("-", "_").replace(" ", "_").toLowerCase();
        }

        @Override
        public int compareTo(PredicateKey o) {
            return tag.compareTo(o.tag);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PredicateKey other = (PredicateKey) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            return true;
        }

        public String getTag() {
            return tag;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public static class PredicateType {
        public final Double defaultValue;
        public final PredicateKey key;
        public final String name;
        public final String description;

        public PredicateType(String key, String name, Double defaultValue) {
            super();
            this.key = new PredicateKey(key);
            this.name = name;
            this.description = key;
            this.defaultValue = defaultValue;
        }
    }

    private static final Logger LOG = Loggers.get(PredicateTypes.class.getName());

    private static List<PredicateType> predicates;

    static final String PREDICATES_TSV = "predicates.tsv";

    private PredicateTypes() {
    }

    public static List<PredicateType> loadPredicates() {
        if (null != predicates) {
            return predicates;
        }

        try (final InputStream stream = PredicateTypes.class.getClassLoader().getResourceAsStream(PREDICATES_TSV);) {
            final Builder<PredicateType> builder = ImmutableList.builder();
            final List<String> readLines = IOUtils.readLines(stream, Charset.defaultCharset());

            for (final String l : readLines) {
                final String[] split = l.split("\t");

                final String key = split[1];
                final String name = split.length == 3 ? split[2] : split[1];
                final String value = split[0];

                final PredicateType prop = new PredicateType(key, name, Double.valueOf(value));

                builder.add(prop);

            }
            predicates = builder.build();

        } catch (final IOException e) {
            LOG.error("Can not read " + PREDICATES_TSV, e);
        }

        return predicates;
    }

}
