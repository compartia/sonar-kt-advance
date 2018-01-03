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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;

public class CacheTest {
    private static final String SRC_MEMTEST_C = "src/memtest.c";
    private static File BASEDIR;
    private static File MODULE_BASEDIR;

    @SuppressWarnings("unused")
    private static final Logger LOG = Loggers.get(CacheTest.class.getName());
    private static DefaultFileSystem fileSystem;
    private static FsAbstraction fs;

    @BeforeClass
    public static void setup() throws JAXBException {
        final URL url = CacheTest.class.getResource("/test_project");
        BASEDIR = new File(url.getFile());
        MODULE_BASEDIR = new File(BASEDIR, "redis/");

        fileSystem = new DefaultFileSystem(MODULE_BASEDIR.toPath());

        final DefaultInputFile memtest = Factory.makeDefaultInputFile(MODULE_BASEDIR, SRC_MEMTEST_C, 282);
        fileSystem.add(memtest);

        fs = new FsAbstraction(fileSystem);
    }

    @Test
    public void tesCache1MPpos() {
        final int numberOfIssues = 1000;
        fs.doInCache(() -> {
            for (int a = 0; a < numberOfIssues; a++) {
                final IssuableProofObligation ipo = Factory.createPrimaryPO(new PredicateKey(""), Integer.toString(a));
                fs.save(ipo);
            }

            assertEquals(numberOfIssues, fs.getSavedKeys().size());

            for (final IpoKey key : fs.getSavedKeys()) {
                assertNotNull(fs.getFromCache(key, false));
            }

        });
    }

}
