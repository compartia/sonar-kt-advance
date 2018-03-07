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
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

//---
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
//---
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.kt.advance.api.FsAbstraction;

public class SonarFsAbstractionImpl implements FsAbstraction {

    @SuppressWarnings("unused")
    private static final Logger LOG = Loggers.get(SonarFsAbstractionImpl.class.getName());

    final FileSystem fileSystem;

    private final File baseDirOverride;

    public SonarFsAbstractionImpl(FileSystem fileSystem) {
        super();
        this.fileSystem = fileSystem;
        this.baseDirOverride = fileSystem.baseDir();
    }

    private SonarFsAbstractionImpl(FileSystem fileSystem, File baseDir) {
        super();
        this.fileSystem = fileSystem;
        this.baseDirOverride = baseDir;
    }

    @Override
    public File getBaseDir() {
        return baseDirOverride;
    }

    @Override
    public FsAbstraction instance(File baseDir) {
        final SonarFsAbstractionImpl newone = new SonarFsAbstractionImpl(this.fileSystem, baseDir);

        return newone;
    }

    @Override
    public Collection<File> listAPIs() {
        return listFileByXmlSuffix(API_SUFFIX);
    }

    @Override
    public Collection<File> listCDICTs() {
        return listFileByXmlSuffix(CDICT_SUFFIX);
    }

    @Override
    public Collection<File> listCFuns() {
        return listFileByXmlSuffix(CFUN_SUFFIX);
    }

    @Override
    public Collection<File> listPODs() {
        return listFileByXmlSuffix(POD_SUFFIX);
    }

    @Override
    public Collection<File> listPPOs() {
        return listFileByXmlSuffix(PPO_SUFFIX);
    }

    @Override
    public Collection<File> listPRDs() {
        return listFileByXmlSuffix(PRD_SUFFIX);
    }

    @Override
    public Collection<File> listSPOs() {
        return listFileByXmlSuffix(SPO_SUFFIX);
    }

    @Override
    public Collection<File> listTargetFiles() {
        final Collection<File> cdicts = listFileByXmlSuffix(CDICT_SUFFIX);
        final Set<File> roots = new TreeSet<File>();
        for (final File f : cdicts) {
            roots.add(f.getParentFile());
        }

        return roots;
    }

    private Collection<File> listFileByXmlSuffix(String suffix) {

        final Set<File> files = new TreeSet<File>();

        final String path = fileSystem.baseDir().toPath().relativize(this.baseDirOverride.toPath()).toString();

        final String inclusionPattern = path + "/**/*" + suffix + ".xml";

        final FilePredicate filePredicate = fileSystem
                .predicates().matchesPathPattern(inclusionPattern);

        final Iterable<InputFile> ifiles = fileSystem.inputFiles(filePredicate);
        for (final InputFile ifile : ifiles) {
            files.add(ifile.file());
        }
        return files;
    }

    @Override
    public Collection<File> listXMLs(String arg0) {
        return listFileByXmlSuffix(arg0);

    }

}
