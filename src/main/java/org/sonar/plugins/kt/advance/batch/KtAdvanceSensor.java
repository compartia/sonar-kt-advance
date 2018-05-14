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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.google.common.base.Preconditions;
import com.kt.advance.api.CAnalysis;
import com.kt.advance.api.CAnalysisImpl;
import com.kt.advance.api.CApplication;
import com.kt.advance.api.CFile;
import com.kt.advance.api.CFunctionCallsiteSPOs;
import com.kt.advance.api.CFunctionSiteSPOs;
import com.kt.advance.api.FsAbstraction;

public class KtAdvanceSensor implements SonarResourceLocator {

    static final Logger LOG = Loggers.get(KtAdvanceSensor.class.getName());
    private final ActiveRules activeRules;

    private int errorsCounterXml = 0;
    /**
     * The file system object for the project being analysed.
     */

    private final ResourcePerspectives perspectives;

    private final Settings settings;

    private final Set<XmlParsingIssue> xmlParsingIssues = new HashSet<>();

    CAnalysis cAnalysis;
    @Deprecated
    final FileSystem fileSystem;

    final FsAbstraction fsAbstraction;

    final POMapper mapper;

    final Statistics statistics;

    public KtAdvanceSensor(final Settings settings, final FileSystem fileSystem, final ActiveRules ruleFinder,
            final ResourcePerspectives perspectives) {

        this.settings = settings;
        this.fileSystem = fileSystem;
        this.activeRules = ruleFinder;
        this.perspectives = perspectives;

        mapper = new POMapper(ruleFinder, settings);

        fsAbstraction = new SonarFsAbstractionImpl(fileSystem);

        statistics = new Statistics();
        printDiagnostics();

    }

    private void printDiagnostics() {
        final Collection<File> aDirs = fsAbstraction.listSubdirsRecursively(FsAbstraction.ANALYSIS_DIR_NAME);
        if (aDirs.size() == 0) {
            LOG.error("NO '{}' subdirectories found!", FsAbstraction.ANALYSIS_DIR_NAME);
        } else {
            LOG.info("number of dirs to scan: {}", aDirs.size());
            aDirs.forEach(f -> {
                LOG.info("DIR to scan: {}", f.getAbsolutePath());
            });
        }
    }

    public void analyse(SensorContext sensorContext) throws JAXBException {

        cAnalysis = new CAnalysisImpl(fsAbstraction);
        cAnalysis.read();
        //--------------------------------------------

        final Collection<CApplication> apps = cAnalysis.getApps();
        if (apps.isEmpty()) {
            LOG.error("NO analyzed C projects found! Nothing to report to SonarQube");
        } else {
            apps.forEach(this::analyzeApplication);
            statistics.save(sensorContext);
        }
        xmlParsingIssues.forEach(this::saveParsingIssueToSq);
    }

    @Override
    public InputFile getResource(CFile cfile) {

        Preconditions.checkNotNull(cfile);

        final File cSourceFile = new File(cfile.getApplication().getSourceDir(), cfile.getName());

        final String relative = cAnalysis.relativize(cSourceFile);

        final FilePredicate filePredicate = fileSystem.predicates().hasRelativePath(relative);
        final InputFile inputFile = fileSystem.inputFile(filePredicate);
        if (inputFile == null) {

            LOG.error("cannot find resource {}  in {}", relative, cfile.getApplication().getSourceDir());
            LOG.error("basedir: \t{}", fileSystem.baseDir());
            LOG.error("abs file: \t{}", cSourceFile);
            return null;
        }

        return inputFile;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    @Deprecated
    public InputFile getXmlAbsoluteResource(final File file) {

        final FilePredicate filePredicate = fileSystem.predicates().is(file);//hasARelativePath(relative);
        final InputFile inputFile = fileSystem.inputFile(filePredicate);

        if (inputFile == null) {
            LOG.error("cannot find '{}'", file.getAbsolutePath());
        }
        return inputFile;
    }

    private void analyzeApplication(final CApplication app) {
        app.getCfiles().forEach(this::analyzeCFile);
    }

    private void analyzeCFile(final CFile file) {
        final InputFile inputFile = getResource(file);
        Issuable fissuable = null;

        try {
            fissuable = perspectives.as(Issuable.class, inputFile);
        } catch (final Exception ex) {
            LOG.error("Cannot get issuable for file {} [{}]", file.getName(), file.getApplication().getSourceDir());
        }

        if (fissuable != null) {
            final Issuable issuable = fissuable;

            file.getCFunctions().forEach(function -> {

                Stream<Issue> poIssues = function.getPPOs()
                        .stream()
                        .map(ppo -> statistics.handle(ppo, file, this))
                        .map(ppo -> mapper.toIssue(ppo, issuable, this, function));

                for (final CFunctionCallsiteSPOs callsite : function.getCallsites()) {

                    final Stream<Issue> spoIssues = callsite.getSpos().stream()
                            .map(spo -> statistics.handle(spo, file, this))
                            .map(spo -> mapper.toIssue(spo, issuable, this, function));

                    poIssues = Stream.concat(poIssues, spoIssues);

                }

                for (final CFunctionSiteSPOs callsite : function.getReturnsites()) {

                    final Stream<Issue> spoIssues = callsite.getSpos().stream()
                            .map(spo -> statistics.handle(spo, file, this))
                            .map(spo -> mapper.toIssue(spo, issuable, this, function));

                    poIssues = Stream.concat(poIssues, spoIssues);

                }

                poIssues.forEach(
                    issue -> saveProofObligationAsIssueToSq(issue, issuable));
            });

        }
    }

    private void handleParsingError(File xmlFile, String msg) {
        handleParsingError(xmlFile, msg, true);
    }

    private void handleParsingError(File xmlFile, String msg, boolean log) {
        final XmlParsingIssue pi = new XmlParsingIssue();
        pi.setFile(xmlFile);
        pi.setMessage(msg);

        if (!xmlParsingIssues.contains(pi)) {
            errorsCounterXml++;
            if (log) {
                LOG.error("XML: (#{}) \t {}:{}", errorsCounterXml, xmlFile.getAbsolutePath(), msg);
            }
            if (xmlParsingIssues.size() < 5000) {
                xmlParsingIssues.add(pi);
            }
        }
    }

    private boolean saveParsingIssueToSq(XmlParsingIssue pi) {

        Preconditions.checkNotNull(pi.getFile());

        final InputFile inputFile = getXmlAbsoluteResource(pi.getFile());
        Preconditions.checkNotNull(inputFile);

        final Issuable issuable = perspectives.as(Issuable.class, inputFile);

        if (null == issuable) {
            LOG.error(
                "Can't find an Issuable corresponding to InputFile: {}", inputFile.absolutePath());
            return false;
        } else {
            try {
                final Issue issue = pi.toIssue(inputFile, issuable, activeRules, settings);
                final boolean result = issuable.addIssue(issue);
                return result;

            } catch (final org.sonar.api.utils.MessageException me) {
                LOG.error(String.format("Can't add issue on file %s ",
                    inputFile.absolutePath()),
                    me);
            }

        }

        return false;
    }

    /**
     *
     * saves PO-related data as SonarQube's Issue
     *
     * @param inputFile
     *            a resource PO is related to
     * @param proofObligation
     * @return
     */
    private boolean saveProofObligationAsIssueToSq(Issue issue, Issuable issuable) {

        Preconditions.checkNotNull(issue, "issue is null");
        Preconditions.checkNotNull(issuable, "issuable is null");

        try {
            final boolean result = issuable.addIssue(issue);
            return result;
        } catch (final org.sonar.api.utils.MessageException me) {
            LOG.error(me.getLocalizedMessage(), me);
        }

        return false;
    }

    FsAbstraction getFsContext() {
        return fsAbstraction;
    }

}
