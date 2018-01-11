package com.kt.advance.xml;

import java.io.File;

public class XmlNamesUtils {

    public static final String XML_EXT = "xml";

    public static File replaceSuffix(File file, String oldSuffix, String newuffix) {
        final String name = file.getName();
        final String newName = name.replace(oldSuffix, newuffix);
        return new File(file.getParentFile(), newName);
    }

    public static File xmlFilename(final File file, final String filePattern, String suff) {
        final StringBuilder sb = new StringBuilder()
                .append(filePattern)
                .append(suff)
                .append('.')
                .append(XmlNamesUtils.XML_EXT);
        return new File(file.getParentFile(), sb.toString());
    }

    public static String xmlSuffix(String postfix) {
        return postfix + "." + XmlNamesUtils.XML_EXT;
    }

}
