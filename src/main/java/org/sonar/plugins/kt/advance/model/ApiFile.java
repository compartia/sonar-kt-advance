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
package org.sonar.plugins.kt.advance.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.sonar.plugins.kt.advance.model.PpoFile.PoPredicate;
import org.sonar.plugins.kt.advance.model.PpoFile.PpoLocation;

@XmlRootElement(name = "c-analysis")
public class ApiFile implements HasOriginFile {
    public static class ApiAssumption {

        @XmlElement(name = "predicate")
        public PoPredicate predicate;

        @XmlAttribute(name = "nr")
        public String nr;

        @XmlElementWrapper(name = "dependent-primary-proof-obligations")
        @XmlElement(name = "po")
        public List<PoRef> dependentPPOs = new ArrayList<>();

        @XmlElementWrapper(name = "dependent-secondary-proof-obligations")
        @XmlElement(name = "po")
        public List<PoRef> dependentSPOs = new ArrayList<>();

        @Override
        public String toString() {
            return "ApiAssumption [nr=" + nr + ", dependentPPOs=" + dependentPPOs.size() + ", dependentSPOs="
                    + dependentSPOs.size()
                    + "]";
        }

    }

    public static class ApiFunction {
        @XmlAttribute(name = "gvid", required = true)
        public Integer gvid;

        @XmlAttribute(name = "name", required = true)
        public String name;

        @XmlAttribute(name = "cfilename", required = true)
        public String cfilename;

        @XmlElementWrapper(name = "callers")
        @XmlElement(name = "caller")
        public List<Caller> callers = new ArrayList<>();

        @XmlElementWrapper(name = "api-assumptions")
        @XmlElement(name = "api-assumption")
        public List<ApiAssumption> apiAssumptions = new ArrayList<>();

        @XmlElementWrapper(name = "rv-assumptions")
        @XmlElement(name = "rv-assumption")
        public List<ApiAssumption> rvAssumptions = new ArrayList<>();

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
            final ApiFunction other = (ApiFunction) obj;
            if (gvid == null) {
                if (other.gvid != null) {
                    return false;
                }
            } else if (!gvid.equals(other.gvid)) {
                return false;
            }
            return true;
        }

        public Map<String, ApiAssumption> getApiAssumptionsAsMap() {
            final Map<String, ApiAssumption> ret = new HashMap<>();

            for (final ApiAssumption aa : apiAssumptions) {
                ret.put(aa.nr, aa);
            }

            return ret;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((gvid == null) ? 0 : gvid.hashCode());
            return result;
        }
    }

    public static class Caller {
        @XmlAttribute(name = "gvid")
        public Integer gvid;

        @XmlElement(name = "call-site")
        public CallSite callSite;

        @XmlAttribute(name = "fname")
        public String fname;

        @XmlAttribute(name = "cfilename")
        public String cfilename;

    }

    public static class CallSite extends PpoLocation {
        private static final long serialVersionUID = 8574970888560338925L;
    }

    public static class PoRef {

        private String id;

        @XmlAttribute(name = "id")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @XmlElement(name = "function")
    public ApiFunction function;

    private File origin;

    @Override
    public File getOrigin() {
        return origin;
    }

    @Override
    public String getTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOrigin(File originatingFile) {
        this.origin = originatingFile;

    }

}
