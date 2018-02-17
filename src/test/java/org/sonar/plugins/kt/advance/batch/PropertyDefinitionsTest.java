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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.measures.Metric;
import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.plugins.kt.advance.KtMetrics;
import org.sonar.plugins.kt.advance.ui.AdvanceBarChartsWidget;
import org.sonar.plugins.kt.advance.ui.KtAdvanceWidget;

import kt.advance.model.PredicatesFactory.PredicateType;

public class PropertyDefinitionsTest {

    @Test
    public void testBarCharProperties() throws IOException {

        final KtMetrics km = new KtMetrics();
        final List<Metric> metrics = km.getMetrics();
        final Properties p = getStrings();

        for (final PredicateType pt : PredicateType.values()) {
            final String name = "widget.kt.advance.bc.property.predicate_" + pt.name() + ".name";
            assertTrue("no message for key " + name, p.containsKey(name));
        }
    }

    /**
     * ensure all widget properties have names defined
     *
     * @throws IOException
     */
    @Test
    public void testBarCharWidgetProperties() throws IOException {
        final Class<? extends AbstractRubyTemplate> widgetClass = AdvanceBarChartsWidget.class;
        testWidgetProperties(widgetClass);
    }

    @Test
    public void testBarCharWidgetPropertiesNumber() throws IOException {
        final Class<AdvanceBarChartsWidget> widgetClass = AdvanceBarChartsWidget.class;
        testWidgetProperties(widgetClass);

        final WidgetProperties annotation = widgetClass.getAnnotation(WidgetProperties.class);

        final WidgetProperty[] value = annotation.value();
        final Set<String> propertyKeys = Arrays.asList(value)
                .stream()
                .filter(p -> p.key().contains("predicate_"))
                .map(p -> p.key().substring(10))
                .collect(Collectors.toSet());

        for (final PredicateType pt : PredicateType.values()) {
            assertTrue("no key " + pt.name() + " available are: " + StringUtils.join(propertyKeys, "\t\n"),
                propertyKeys.contains(pt.name()));
        }

    }

    @Test
    public void testKeysUnique() {
        final PluginParameters pp = new PluginParameters();

        final List<PropertyDefinition> propertyDefinitions = pp.getPropertyDefinitions();
        final Set<String> keys = new HashSet<>();
        for (final PropertyDefinition pd : propertyDefinitions) {
            final String key = pd.key();
            assertTrue("duplicated key " + key, !keys.contains(key));
            keys.add(key);
        }
    }

    @Test
    public void testMetricKeysUnique() {

        final KtMetrics km = new KtMetrics();
        final List<Metric> metrics = km.getMetrics();
        final Set<String> keys = new HashSet<>();
        for (final Metric pd : metrics) {
            final String key = pd.key();
            assertTrue("duplicated key " + key, !keys.contains(key));
            keys.add(key);
        }
    }

    @Test
    public void testMetricsHaveDescr() throws IOException {

        final KtMetrics km = new KtMetrics();
        final List<Metric> metrics = km.getMetrics();
        final Properties properties = getStrings();

        for (final Metric<?> pd : metrics) {
            final String descrKey = "metric." + pd.key() + ".description";

            if (pd.key().indexOf("_predicate__") == -1) {
                assertTrue("no description for metric key " + descrKey + "; valid keys are: "
                        + StringUtils.join(properties.keySet().toArray(), ", "),
                    properties.containsKey(descrKey));
            }
        }

    }

    @Test
    public void testMetricsHaveWidgetNames() throws IOException {

        final KtMetrics km = new KtMetrics();
        final List<Metric> metrics = km.getMetrics();
        final Properties p = getStrings();

        for (final Metric<?> pd : metrics) {
            final String name = "widget.metric." + pd.key() + ".name";

            if (name.indexOf("_open_predicate_") == -1
                    && name.indexOf("_violation_predicate_") == -1
                    && name.indexOf("_dead_predicate_") == -1) {
                assertTrue("no message for key " + name, p.containsKey(name));
            }
        }
    }

    /**
     * ensure all widget properties have names defined
     *
     * @throws IOException
     */
    @Test
    public void testStatsWidgetProperties() throws IOException {
        final Class<? extends AbstractRubyTemplate> widgetClass = KtAdvanceWidget.class;
        testWidgetProperties(widgetClass);

    }

    private Properties getStrings() throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream("/org/sonar/l10n/advance.properties");) {
            assertNotNull("cannot read 'advance.properties'", stream);
            final Properties p = new Properties();
            p.load(stream);

            assertTrue(!p.isEmpty());

            return p;
        }
    }

    void testWidgetProperties(Class<? extends AbstractRubyTemplate> widgetClass) throws IOException {
        final WidgetProperties annotation = widgetClass.getAnnotation(WidgetProperties.class);
        if (annotation != null) {

            final WidgetProperty[] values = annotation.value();

            final Properties properties = getStrings();
            for (final WidgetProperty wp : values) {

                final String key = "widget.kt.advance.bc.property." + wp.key() + ".name";
                assertTrue("no key " + key + " in .properties file ", properties.containsKey(key));
            }
        }
    }

}
