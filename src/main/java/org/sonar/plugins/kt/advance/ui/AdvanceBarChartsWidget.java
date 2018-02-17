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
package org.sonar.plugins.kt.advance.ui;

import static org.sonar.api.web.WidgetPropertyType.BOOLEAN;

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.Description;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetScope;
import org.sonar.plugins.kt.advance.KtAdvancePlugin;

@UserRole(UserRole.USER)
@WidgetScope(value = { "PROJECT" })
@Description("KT Advance C static analyser")
@WidgetCategory("C language")
/**
 * shown by default: Valid Memory, Upper Bound, Lower Bound, Null-Terminated,
 * Not Null
 *
 *
 */
@WidgetProperties({
        @WidgetProperty(type = BOOLEAN, key = "wp_show_ppo_violation", defaultValue = "true"),
        @WidgetProperty(type = BOOLEAN, key = "wp_show_ppo_open", defaultValue = "true"),
        @WidgetProperty(type = BOOLEAN, key = "wp_show_spo_violation", defaultValue = "true"),
        @WidgetProperty(type = BOOLEAN, key = "wp_show_spo_open", defaultValue = "true"),

        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate__lb"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate__ub"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate__nn"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate__nt"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate__ub"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate__vm"),

        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__ab"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__pre"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__c"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__cb"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__cbt"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__ft"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__gm"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__ilb"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__iub"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__i"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__ir"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__io"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__iu"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__nneg"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__no"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__z"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__null"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__pc"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__plb"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__pub"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__pubd"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__csu"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__tao"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__cus"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__w"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate__vc")

})

public class AdvanceBarChartsWidget extends AbstractRubyTemplate implements RubyRailsWidget {

    @Override
    public String getId() {
        return KtAdvancePlugin.KEY + ".bc";
    }

    @Override
    public String getTitle() {
        return "KT Advance Bar Chart";
    }

    @Override
    protected String getTemplatePath() {
        return KtAdvanceWidget.templatePath("/ui/advance_bar_chart_widget.html.erb");
    }
}