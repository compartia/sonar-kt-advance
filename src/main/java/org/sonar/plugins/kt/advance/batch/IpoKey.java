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
import org.sonar.plugins.kt.advance.model.SpoFile;

public class IpoKey implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1403100647754956716L;

    public static final String ANY_FNAME_CONTEXT = "-";

    @XmlAttribute(name = "id")
    private String id;

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

    public IpoKey(File originXml, String fname, String poId, POLevel level) {
        super();
        this.originXml = originXml.getName().substring(0, originXml.getName().lastIndexOf('.'));
        this.fname = fname;
        this.setId(poId);
        this.level = level;
    }

    public static IpoKey secondary(SpoFile spoXml, String id) {
        return new IpoKey(spoXml.getOrigin(), spoXml.function.name,
                id, POLevel.SECONDARY);
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
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
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

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fname == null) ? 0 : fname.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((originXml == null) ? 0 : originXml.hashCode());
        return result;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return level.ordinal() + "." + id + "." + fname + "." + originXml;
    }

}