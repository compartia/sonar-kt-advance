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

import javax.xml.bind.JAXBException;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.KtLanguage;

public class KtAdvanceSensorRunner implements Sensor {

    static final Logger LOG = Loggers.get(KtAdvanceSensorRunner.class.getName());

    /**
     * The file system object for the project being analysed.
     */
    private final FileSystem fileSystem;
    private final ResourcePerspectives perspectives;
    private final ActiveRules activeRules;
    private final Settings settings;

    public KtAdvanceSensorRunner(final Settings settings, final FileSystem fileSystem, final ActiveRules activeRules,
            final ResourcePerspectives perspectives) throws JAXBException {
        this.settings = settings;
        this.fileSystem = fileSystem;
        this.activeRules = activeRules;
        this.perspectives = perspectives;

    }

    @Override
    public void analyse(Project project, SensorContext sensorContext) {

        final Iterable<InputFile> files = fileSystem.inputFiles(fileSystem.predicates().hasType(InputFile.Type.MAIN));
        if (LOG.isDebugEnabled()) {
            LOG.info("listing input files:");
            for (final InputFile f : files) {
                LOG.info(f.relativePath() + " " + f.language() + " " + f.lines());
            }
        }
        try {
            final KtAdvanceSensor session = new KtAdvanceSensor(settings, fileSystem, activeRules, perspectives);
            session.analyse(sensorContext);

        } catch (final JAXBException e) {
            throw new ScanFailedException(e);
        }

    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return fileSystem.languages().contains(KtLanguage.KEY);
    }

}
