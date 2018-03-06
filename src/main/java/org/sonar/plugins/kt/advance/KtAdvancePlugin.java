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

import java.util.List;

import org.sonar.api.SonarPlugin;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition;
import org.sonar.plugins.kt.advance.batch.KtAdvanceSensorRunner;
import org.sonar.plugins.kt.advance.batch.PluginParameters;
import org.sonar.plugins.kt.advance.ui.AdvanceBarChartsWidget;
import org.sonar.plugins.kt.advance.ui.KtAdvanceWidget;

/**
 * The entry point for all the plug-in's extensions
 *
 * @author artem
 *
 */

public final class KtAdvancePlugin extends SonarPlugin {
    public static final String KEY = "kt.advance";

    /**
     * Defines the plugin extensions: metrics, sensor and dashboard widget.
     *
     * @return the list of extensions for this plugin
     */
    @Override
    public List<Object> getExtensions() {
        final PluginParameters pt = new PluginParameters();
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<Object> extensions = (List) pt.getPropertyDefinitions();
        extensions.add(KtLanguage.class);
        extensions.add(KtAdvanceRulesDefinition.class);
        extensions.add(KtProfile.class);
        extensions.add(KtMetrics.class);
        extensions.add(KtAdvanceSensorRunner.class);
        extensions.add(KtAdvanceWidget.class);
        extensions.add(AdvanceBarChartsWidget.class);

        return extensions;
    }
}
