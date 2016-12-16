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

        //
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_allocation_base"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_cast"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_common_base"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_common_base_type"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_format_string"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_f_precondition"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_global_mem"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_index_lower_bound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_index_upper_bound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_initialized"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_initialized_range"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_int_overflow"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_int_underflow"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate_lower_bound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_non_negative"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_no_overlap"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate_not_null"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_not_zero"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_null"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate_null_terminated"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_pointer_cast"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_ptr_lower_bound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_ptr_upper_bound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_ptr_upper_bound_deref"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_signed_to_unsigned_cast"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_type_at_offset"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_unsigned_to_signed_cast"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate_upper_bound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_width_overflow"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "true", key = "predicate_valid_mem"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_value_constraint"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_div_by_zero"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_ptr_upperbound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_ptr_op_lowerbound"),
        @WidgetProperty(type = BOOLEAN, defaultValue = "false", key = "predicate_ptr_op_upperbound")

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