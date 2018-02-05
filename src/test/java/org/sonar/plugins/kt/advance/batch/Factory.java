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
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import com.kt.advance.xml.XmlNamesUtils;

public class Factory {

    public static DefaultInputDir makeDefaultInputDir(File basedir, String _filename) {
        final String rel = basedir.toPath().relativize(new File(_filename).toPath()).toString();

        final DefaultInputDir dir = new DefaultInputDir("", rel);
        dir.setModuleBaseDir(basedir.toPath());

        return dir;
    }

    public static DefaultInputFile makeDefaultInputFile(File basedir, String _filename, int len) {

        final String rel = basedir.toPath().relativize(new File(_filename).toPath()).toString();

        final DefaultInputFile dif = new DefaultInputFile("", rel);
        dif.setModuleBaseDir(basedir.toPath());
        dif.setLines(len);
        final int[] originalLineOffsets = new int[len];
        for (int f = 0; f < originalLineOffsets.length; f++) {
            originalLineOffsets[f] = f * 200;
        }
        dif.setOriginalLineOffsets(originalLineOffsets);
        return dif;
    }

    public static DefaultFileSystem makeFileSystem(final File MODULE_BASEDIR) {
        final DefaultFileSystem fileSystem = new DefaultFileSystem(MODULE_BASEDIR.toPath());
        {
            final Iterator<File> iter = FileUtils.iterateFiles(MODULE_BASEDIR,
                new String[] { XmlNamesUtils.XML_EXT, "c" },
                true);

            while (iter.hasNext()) {
                final File file = iter.next();
                if (file.getName().endsWith("_cdict.xml")) {
                    System.err.println(file.getAbsolutePath());
                }

                if (file.isFile()) {
                    fileSystem.add(Factory.makeDefaultInputFile(MODULE_BASEDIR, file.getAbsolutePath(), 282));
                }
            }
        }

        return fileSystem;
    }

}