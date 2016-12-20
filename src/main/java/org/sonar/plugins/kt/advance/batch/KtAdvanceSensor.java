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

import static org.sonar.plugins.kt.advance.batch.FsAbstraction.API_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.PEV_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.PPO_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.SEV_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.SPO_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.readApiXml;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.readPevXml;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.readSevXml;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.readSpoXml;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.replaceSuffix;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.xmlFilename;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.IssuableProofObligation.SPOBuilder;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POLevel;
import org.sonar.plugins.kt.advance.model.ApiFile;
import org.sonar.plugins.kt.advance.model.ApiFile.ApiAssumption;
import org.sonar.plugins.kt.advance.model.ApiFile.PoRef;
import org.sonar.plugins.kt.advance.model.PevFile;
import org.sonar.plugins.kt.advance.model.PevFile.PO;
import org.sonar.plugins.kt.advance.model.PpoFile;
import org.sonar.plugins.kt.advance.model.PpoFile.PrimaryProofObligation;
import org.sonar.plugins.kt.advance.model.SevFile;
import org.sonar.plugins.kt.advance.model.SpoFile;
import org.sonar.plugins.kt.advance.model.SpoFile.CallSiteObligation;
import org.sonar.plugins.kt.advance.model.SpoFile.SecondaryProofObligation;

import com.google.common.base.Preconditions;

public class KtAdvanceSensor {

    static final Logger LOG = Loggers.get(KtAdvanceSensor.class.getName());
    /**
     * The file system object for the project being analysed.
     */

    private final ResourcePerspectives perspectives;
    private final ActiveRules activeRules;

    private final Settings settings;
    final Statistics statistics;

    final FsAbstraction fsAbstraction;

    public KtAdvanceSensor(FsAbstraction fs) {
        fsAbstraction = fs;
        settings = null;
        perspectives = null;
        statistics = new Statistics(fsAbstraction);
        activeRules = null;
    }

    public KtAdvanceSensor(final Settings settings, final FileSystem fileSystem, final ActiveRules ruleFinder,
            final ResourcePerspectives perspectives) throws JAXBException {
        this.settings = settings;

        this.activeRules = ruleFinder;
        this.perspectives = perspectives;

        fsAbstraction = new FsAbstraction(fileSystem);
        statistics = new Statistics(fsAbstraction);

    }

    public void analyse(SensorContext sensorContext) throws JAXBException {
        fsAbstraction.doInCache(() -> analyseImpl(sensorContext));
    }

    public void analysePpoXml(final File ppoXml) {
        try {

            final PpoFile ppoFile = FsAbstraction.readPpoXml(ppoXml);
            processPPOs(ppoFile);

            final File spoXml = replaceSuffix(ppoXml, PPO_SUFFIX, SPO_SUFFIX);
            final SpoFile spo = readSpoXml(spoXml);
            if (spo != null) {
                processSPOs(spo);
            }

        } catch (final JAXBException e) {
            LOG.error("Failed parsing file " + ppoXml, e);
        }
    }

    public FsAbstraction getFsContext() {
        return fsAbstraction;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    private void analyseImpl(SensorContext sensorContext) throws JAXBException {
        fsAbstraction.forEachPpoFile(this::analysePpoXml);

        for (final IpoKey key : fsAbstraction.getSavedKeys()) {
            saveProofObligationAsIssueToSq(fsAbstraction.get(key));
        }

        statistics.save(sensorContext);
    }

    private IssuableProofObligation getPoThroughCache(File ppoXml, int id) throws JAXBException {
        final IpoKey ppoKey = new IpoKey(ppoXml, id, POLevel.PRIMARY);
        final IssuableProofObligation primaryIpo = fsAbstraction.get(ppoKey);
        if (primaryIpo == null) {
            final PpoFile ppoFile = FsAbstraction.readPpoXml(ppoXml);
            processPPOs(ppoFile);
            return fsAbstraction.get(ppoKey);
        } else {
            return primaryIpo;
        }

    }

    /**
     * link SPO to PPO via assumptions
     *
     * @throws JAXBException
     */
    private void linkAssumptions(
            ApiAssumption assumption,
            final IssuableProofObligation secondaryIpo,
            final CallSiteObligation co,
            File ppoOriginXml) throws JAXBException {

        Preconditions.checkNotNull(assumption);

        for (final PoRef ref : assumption.dependentPPOs) {

            final IssuableProofObligation primaryIpo = getPoThroughCache(ppoOriginXml, ref.id);

            if (primaryIpo != null) {

                secondaryIpo.addReference(primaryIpo);
                primaryIpo.addReference(secondaryIpo);

                putPoToCache(secondaryIpo);
                putPoToCache(primaryIpo);

            } else {
                LOG.error("an assumption refers non-existent PPO with id " + ref.id + " in file " + co.fname);
            }
        }
    }

    private void processCallSiteObligation(
            SpoFile originXml,
            final CallSiteObligation spoCallSiteObligation,
            final Map<Integer, PO> dischargedSPOs) throws JAXBException {

        if (!spoCallSiteObligation.proofObligations.isEmpty()) {
            Map<Integer, ApiAssumption> apiAssumptionsById = null;

            File ppoXml = null;
            if (spoCallSiteObligation.fname != null) {
                final String filePattern = originXml.getOrigin().getParentFile().getName() + "_"
                        + spoCallSiteObligation.fname;
                LOG.trace("reading API, pattern:" + filePattern);

                final ApiFile api = readApiXml(xmlFilename(originXml.getOrigin(), filePattern, API_SUFFIX));
                if (api != null) {
                    apiAssumptionsById = api.function.getApiAssumptionsAsMap();
                }

                ppoXml = xmlFilename(originXml.getOrigin(), filePattern, PPO_SUFFIX);
            }

            final SPOBuilder builder = IssuableProofObligation.newBuilder(originXml, spoCallSiteObligation);
            builder.setInputFile(fsAbstraction.getResource(spoCallSiteObligation.location.file));

            for (final SecondaryProofObligation spo : spoCallSiteObligation.proofObligations) {

                final IssuableProofObligation newSecondaryIpo = builder
                        .setSpo(spo)
                        .setDischarge(dischargedSPOs.get(spo.id))
                        .build();

                if (apiAssumptionsById != null) {
                    linkAssumptions(apiAssumptionsById.get(spo.apiId), newSecondaryIpo, spoCallSiteObligation, ppoXml);
                }

                putPoToCache(newSecondaryIpo);
            }
        }
    }

    private boolean putPoToCache(IssuableProofObligation proofObligation) {
        fsAbstraction.save(proofObligation);
        return true;
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
    private boolean saveProofObligationAsIssueToSq(IssuableProofObligation proofObligation) {

        Preconditions.checkNotNull(proofObligation);

        final InputFile inputFile = fsAbstraction.getResource(proofObligation.getLocation().file);
        final Issuable issuable = perspectives.as(Issuable.class, inputFile);

        if (null == issuable) {
            LOG.error(
                "Can't find an Issuable corresponding to InputFile:" + inputFile.absolutePath());
            return false;
        } else {
            try {
                final Issue issue = proofObligation.toIssue(issuable, activeRules, settings, fsAbstraction);

                statistics.handle(proofObligation);
                final boolean result = issuable.addIssue(issue);

                if (LOG.isTraceEnabled()) {
                    LOG.trace(String.format("added issue on  %s : %d. effort=%f",
                        inputFile.relativePath(), proofObligation.getLocation().line,
                        issue.gap()));
                }
                return result;
            } catch (final org.sonar.api.utils.MessageException me) {
                LOG.error(String.format("Can't add issue on file %s at line %d.",
                    inputFile.absolutePath(), proofObligation.getLocation().line),
                    me);
            }

        }

        return false;
    }

    List<IssuableProofObligation> processPPOs(PpoFile ppo)
            throws JAXBException {

        Preconditions.checkNotNull(ppo);

        final List<IssuableProofObligation> ret = new ArrayList<>();
        final PevFile pev = readPevXml(replaceSuffix(ppo.getOrigin(), PPO_SUFFIX, PEV_SUFFIX));
        final Map<Integer, PO> dischargedPOs = (pev == null) ? new HashMap<>() : pev.getDischargedPOsAsMap();

        for (final PrimaryProofObligation po : ppo.function.proofObligations) {

            IssuableProofObligation ipo = fsAbstraction.get(new IpoKey(ppo.getOrigin(), po.id, POLevel.PRIMARY));

            if (ipo == null) {
                final InputFile resource = fsAbstraction.getResource(po.location.file);

                if (resource != null) {

                    ipo = IssuableProofObligation.newBuilder(ppo, po)
                            .setDischarge(dischargedPOs.get(po.id))
                            .setInputFile(resource)
                            .build();

                    putPoToCache(ipo);

                } else {
                    LOG.warn(
                        "processing \'" + ppo.getOrigin() + "\': no source with name \'" + po.location.file + "\'");
                }
            }

            if (ipo != null) {
                ret.add(ipo);
            }
        }
        return ret;
    }

    void processSPOs(SpoFile spo) throws JAXBException {

        final SevFile sev = readSevXml(replaceSuffix(spo.getOrigin(), SPO_SUFFIX, SEV_SUFFIX));
        final Map<Integer, PO> dischargedPOs = (sev == null) ? new HashMap<>() : sev.getDischargedPOsAsMap();

        for (final CallSiteObligation co : spo.function.spoWrapper.proofObligations) {
            processCallSiteObligation(spo, co, dischargedPOs);
        }

    }

}
