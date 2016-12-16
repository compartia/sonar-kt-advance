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

import java.io.File;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;

import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POLevel;

public class IpoKey implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1403100647754956716L;

    public static final String ANY_FNAME_CONTEXT = "-";

    @XmlAttribute(name = "id")
    public int id;

    @XmlAttribute(name = "fname")
    public String fname;

    /**
     * relative to C file path
     */
    @XmlAttribute(name = "xml")
    public String originXml;

    /**
     * true for PPO, false for SPO
     */
    @XmlAttribute
    public POLevel level;

    public IpoKey() {
    }

    public IpoKey(File originXml, int id, POLevel level) {
        this(originXml.getAbsolutePath(), IpoKey.ANY_FNAME_CONTEXT, id, level);
    }

    public IpoKey(String originXml, String fname, int id, POLevel level) {
        super();
        this.originXml = originXml;
        this.fname = fname;
        this.id = id;
        this.level = level;
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
        final IpoKey other = (IpoKey) obj;
        if (fname == null) {
            if (other.fname != null) {
                return false;
            }
        } else if (!fname.equals(other.fname)) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        if (level != other.level) {
            return false;
        }
        if (originXml == null) {
            if (other.originXml != null) {
                return false;
            }
        } else if (!originXml.equals(other.originXml)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fname == null) ? 0 : fname.hashCode());
        result = prime * result + id;
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((originXml == null) ? 0 : originXml.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s:%d %s:%s", level.name(), id, originXml, fname);
    }

}