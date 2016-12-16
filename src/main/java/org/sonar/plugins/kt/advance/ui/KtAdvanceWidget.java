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

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.Description;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetScope;
import org.sonar.plugins.kt.advance.KtAdvancePlugin;

@UserRole(UserRole.USER)
@WidgetScope(value = { "PROJECT" })
@Description("KT Advance C static analyser")
@WidgetCategory("C language")
public class KtAdvanceWidget extends AbstractRubyTemplate implements RubyRailsWidget {

    public static String templatePath(String subPath) {
        if ("TRUE".equals(System.getenv("SONAR_DEV_MODE"))) {
            return System.getenv("SONAR_DEV_MODE_DIR") + subPath;
        } else {
            return subPath;
        }
    }

    @Override
    public String getId() {
        return KtAdvancePlugin.KEY;
    }

    @Override
    public String getTitle() {
        return "KT Advance";
    }

    @Override
    protected String getTemplatePath() {
        return templatePath("/ui/advance_widget.html.erb");
    }
}