package com.kt.advance.xml.model;

import java.io.File;

/**
 * we could use plain String as a key, put this is for type safety
 *
 * @author artem
 *
 */
public class ObjectKey {
    public final String key;

    public ObjectKey(File base, AnalysisXml file, Integer id) {

        final String relative = base
                .toURI().relativize(file.getOriginAnalysisDir().toURI()).getPath();

        this.key = relative + "::" + file.getSourceFilename() + "::" + file.getFunctionName() + "::" + id;
    }

    public ObjectKey(File base, AnalysisXml file, Integer id, String func) {

        final String relative = base
                .toURI().relativize(file.getOriginAnalysisDir().toURI()).getPath();

        this.key = relative + "::" + file.getSourceFilename() + "::" + func + "::" + id;
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
        final ObjectKey other = (ObjectKey) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
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
        return "[" + key + "]";
    }
}