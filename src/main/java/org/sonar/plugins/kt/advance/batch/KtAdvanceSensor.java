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
import java.util.HashSet;
import java.util.Set;

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
import com.kt.advance.xml.FsAbstraction;

import kt.advance.model.CAnalysis;
import kt.advance.model.CApplication;
import kt.advance.model.CFile;
import kt.advance.model.CFunction;
import kt.advance.model.CFunctionCallsiteSPO;

public class KtAdvanceSensor implements SonarResourceLocator {

    static final Logger LOG = Loggers.get(KtAdvanceSensor.class.getName());
    /**
     * The file system object for the project being analysed.
     */

    private final ResourcePerspectives perspectives;

    private final ActiveRules activeRules;
    private final Settings settings;

    final Statistics statistics;

    final FsAbstraction fsAbstraction;

    private int errorsCounterXml = 0;
    private final Set<XmlParsingIssue> xmlParsingIssues = new HashSet<>();

    final POMapper mapper;

    @Deprecated
    final FileSystem fileSystem;

    public KtAdvanceSensor(final Settings settings, final FileSystem fileSystem, final ActiveRules ruleFinder,
            final ResourcePerspectives perspectives) {

        this.settings = settings;
        this.fileSystem = fileSystem;
        this.activeRules = ruleFinder;
        this.perspectives = perspectives;

        mapper = new POMapper(ruleFinder, settings);

        fsAbstraction = new SonarFsAbstractionImpl(fileSystem);
        statistics = new Statistics();

    }

    public void analyse(SensorContext sensorContext) throws JAXBException {

        final CAnalysis cAnalysis = new CAnalysis(fsAbstraction);
        cAnalysis.read();
        //--------------------------------------------

        for (final CApplication app : cAnalysis.getApps()) {

            for (final CFile file : app.cfiles.values()) {

                final InputFile inputFile = getResource(app, file);
                final Issuable issuable = perspectives.as(Issuable.class, inputFile);

                for (final CFunction function : file.cfunctions.values()) {
                    function.getPPOs()
                            .stream()
                            .map(ppo -> {
                                statistics.handle(ppo, app, file, this);
                                return ppo;
                            })
                            .map(ppo -> mapper.toIssue(ppo, issuable, this, app, file))
                            .forEach(issue -> saveProofObligationAsIssueToSq(issue, issuable));
                    //XXX: trigger stats
                    for (final CFunctionCallsiteSPO callsite : function.getSPOs()) {
                        //XXX: trigger stats
                        callsite.spos.values().stream()
                                .map(spo -> {
                                    statistics.handle(spo, app, file, this);
                                    return spo;
                                })
                                .map(spo -> mapper.toIssue(spo, issuable, this, app, file))
                                .forEach(issue -> saveProofObligationAsIssueToSq(issue, issuable));

                    }
                }
            }
        }

        statistics.save(sensorContext);
        for (final XmlParsingIssue pi : xmlParsingIssues) {
            saveParsingIssueToSq(pi);
        }

    }

    @Override
    public InputFile getResource(CApplication app, CFile cfile) {

        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(cfile);
        File f = new File(app.fs.getBaseDir().getParentFile(), "sourcefiles");
        f = new File(f, cfile.getName());

        final String relative = relativize(f);

        //        final FilePredicate filePredicate = fileSystem.predicates().hasAbsolutePath(f.getAbsolutePath());
        final FilePredicate filePredicate = fileSystem.predicates().hasRelativePath(relative);
        final InputFile inputFile = fileSystem.inputFile(filePredicate);
        if (inputFile == null) {

            LOG.error("cannot find " + relative + " in " + app.fs.getBaseDir().getAbsolutePath());
            LOG.error("basedir:" + fileSystem.baseDir());
            LOG.error("app basedir:" + app.fs.getBaseDir());
            LOG.error("abs file:" + f);
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
            LOG.error("cannot find '" + file.getAbsolutePath());
        }
        return inputFile;
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
                LOG.error("XML: (#" + errorsCounterXml + ") " + xmlFile.getAbsolutePath() + " : " + msg);
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
                "Can't find an Issuable corresponding to InputFile:" + inputFile.absolutePath());
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

            //            statistics.handle(issue);
            //XXX: trigger stats!!!
            final boolean result = issuable.addIssue(issue);

            return result;
        } catch (final org.sonar.api.utils.MessageException me) {
            LOG.error(String.format("Can't add issue on file %s at line %d.",
                "-=file=-", issue),//XXX: file name
                me);
        }

        return false;
    }

    FsAbstraction getFsContext() {
        return fsAbstraction;
    }

    String relativize(File f) {
        return fsAbstraction.getBaseDir().toPath().relativize(f.toPath()).toString();
    }

}
