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

import static org.sonar.plugins.kt.advance.batch.FsAbstraction.PEV_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.PPO_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.SEV_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.SPO_SUFFIX;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.readPevXml;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.readSevXml;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.readSpoXml;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.replaceSuffix;
import static org.sonar.plugins.kt.advance.batch.FsAbstraction.xmlFilename;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private int errorsCounterXml = 0;

    private final Set<XmlParsingIssue> xmlParsingIssues = new HashSet<>();

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
        if (errorsCounterXml > 0) {
            LOG.error("There are " + errorsCounterXml + " error(s) in XMLs; ");
        }
    }

    public void analysePpoSpoXml(final File ppoXml) {
        try {

            final PpoFile ppoFile = FsAbstraction.readPpoXml(ppoXml);
            processPPOs(ppoFile);

            final File spoXml = replaceSuffix(ppoXml, PPO_SUFFIX, SPO_SUFFIX);
            processSPOs(spoXml, false);

        } catch (final JAXBException e) {
            handleParsingError(ppoXml, "XML parsing failed: " + e.getMessage());
        }
    }

    public Statistics getStatistics() {
        return statistics;
    }

    private void analyseImpl(SensorContext sensorContext) throws JAXBException {

        fsAbstraction.forEachPpoFile(this::analysePpoSpoXml);

        for (final IpoKey key : fsAbstraction.getSavedKeys()) {
            saveProofObligationAsIssueToSq(fsAbstraction.get(key));
        }

        statistics.save(sensorContext);
        for (final XmlParsingIssue pi : xmlParsingIssues) {
            saveParsingIssueToSq(pi);
        }
    }

    /**
     * returns cached IssuableProofObligation if any. if IpoKey is not in the
     * cache, it parses the given XML file.
     *
     * @param ppoXml
     *            file to parse in case there's no IPO in the cache
     * @param id
     * @return
     * @throws JAXBException
     */
    private IssuableProofObligation getPPoThroughCache(File ppoXml, IpoKey ppoKey) throws JAXBException {
        final IssuableProofObligation primaryIpo = fsAbstraction.get(ppoKey);

        if (primaryIpo == null) {
            final PpoFile ppoFile = FsAbstraction.readPpoXml(ppoXml);
            processPPOs(ppoFile);
            return fsAbstraction.get(ppoKey);
        } else {
            return primaryIpo;
        }

    }

    private IssuableProofObligation getSPoTroughCache(File spoXml, IpoKey spoKey) throws JAXBException {
        final IssuableProofObligation secondaryIpo = fsAbstraction.get(spoKey);
        if (secondaryIpo == null) {
            processSPOs(spoXml, true);
            return fsAbstraction.get(spoKey);
        }
        return secondaryIpo;
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

    /**
     * link SPO to PPO via assumptions
     *
     * @throws JAXBException
     */
    private void linkAssumptions(ApiFile assumptionOriginApiFile,
            String apiId,
            final IssuableProofObligation sourceIpo,
            final CallSiteObligation co,
            File ppoOriginXml,
            File spoOriginFile) throws JAXBException {

        final ApiAssumption assumption = assumptionOriginApiFile.function.getApiAssumptionsAsMap().get(apiId);

        Preconditions.checkNotNull(assumption);

        for (final PoRef ref : assumption.dependentPPOs) {
            IpoKey targetIpoKey = new IpoKey(ppoOriginXml, assumptionOriginApiFile.function.name,
                    ref.getId(), POLevel.PRIMARY);

            IssuableProofObligation targetIpo = getPPoThroughCache(ppoOriginXml, targetIpoKey);
            if (targetIpo == null) {
                LOG.warn("api-assumption nr=" + assumption.nr +
                        " refers dependent-primary-proof-obligation with id=" + ref.getId()
                        + "; but that PPO id is not found in file " + relativize(ppoOriginXml)
                        + "; trying to find SPO");

                /**
                 * this is fallback solution. Actually, there is an
                 * inconsistency in XMLs: secondary POs are referenced the way
                 * as they were primary.
                 */
                targetIpoKey = new IpoKey(spoOriginFile, assumptionOriginApiFile.function.name,
                        ref.getId(), POLevel.SECONDARY);

                targetIpo = getSPoTroughCache(spoOriginFile, targetIpoKey);
                //XXX: there might be nothing in cache yet, if id refers another file/funciton.
            }

            if (targetIpo != null) {

                sourceIpo.addReference(targetIpo, assumption);
                // targetIpo.addReference(sourceIpo, assumption); XXX: are we okay with one-way references?

                putPoToCache(sourceIpo);
                putPoToCache(targetIpo);

            } else {
                //XXX: it could be discharged, look for it in SEV file!
                /**
                 * check if we're looking for it in the proper file
                 */

                handleParsingError(assumptionOriginApiFile.getOrigin(), "api-assumption with nr=" + assumption.nr +
                        " refers to dependent proof obligation with id=" + ref.getId()
                        + " but neither PPO nor SPO was found in files " + relativize(ppoOriginXml) + " and "
                        + relativize(spoOriginFile));

            }
        }
    }

    private void linkAssumptions(
            final SpoFile spoXml,
            final CallSiteObligation spoCallSiteObligation) throws JAXBException {

        if (!spoCallSiteObligation.proofObligations.isEmpty()) {

            File ppoXmlFile = null;
            File spoXmlFileRef = null;
            final ApiFile api = fsAbstraction.getApiByFunc(spoCallSiteObligation.fname,
                spoCallSiteObligation.location.file);
            Preconditions.checkNotNull(api, "no API file *" + spoCallSiteObligation.fname + "_api.xml");

            if (spoCallSiteObligation.fname != null) {
                final File apiXmlFile = api.getOrigin();

                final String nameFilePattern = apiXmlFile.getParentFile().getName() + "_"
                        + spoCallSiteObligation.fname;

                ppoXmlFile = xmlFilename(apiXmlFile, nameFilePattern, PPO_SUFFIX);
                spoXmlFileRef = xmlFilename(apiXmlFile, nameFilePattern, SPO_SUFFIX);
            }

            final Map<String, ApiAssumption> apiAssumptionsById = (api == null) ? null
                    : api.function.getApiAssumptionsAsMap();

            /**
             * link assumptions
             */
            if (apiAssumptionsById != null) {
                for (final SecondaryProofObligation spo : spoCallSiteObligation.proofObligations) {
                    /**
                     * get SPO from cache
                     */
                    final IpoKey key = IpoKey.secondary(spoXml, spo.getId());
                    final IssuableProofObligation sourceIpo = fsAbstraction.get(key);
                    Preconditions.checkNotNull(sourceIpo);

                    final ApiAssumption assumption = apiAssumptionsById.get(spo.getApiId());

                    if (null == assumption) {
                        handleParsingError(spoXml.getOrigin(), "obligation with id " + spo.getId() + " refers api-id="
                                + spo.getApiId() + ", but no assumption with nr=" + spo.getApiId() + " found in "
                                + relativize(api.getOrigin()));
                    } else {
                        linkAssumptions(api, spo.getApiId(), sourceIpo, spoCallSiteObligation,
                            ppoXmlFile, spoXmlFileRef);
                    }

                }
            }

        }
    }

    private void processCallSiteObligation(
            SpoFile spoXml,
            final CallSiteObligation spoCallSiteObligation,
            final Map<String, PO> dischargedSPOs) throws JAXBException {

        final InputFile callsiteSource = fsAbstraction.getResource(spoCallSiteObligation.location.file);

        if (!spoCallSiteObligation.proofObligations.isEmpty()) {

            final File spoXmlFile = spoXml.getOrigin();

            if (callsiteSource != null) {

                final SPOBuilder builder = IssuableProofObligation.newBuilder(spoXml, spoCallSiteObligation);
                builder.setInputFile(callsiteSource);

                for (final SecondaryProofObligation spo : spoCallSiteObligation.proofObligations) {

                    final IssuableProofObligation newSecondaryIpo = builder
                            .setSpo(spo)
                            .setDischarge(dischargedSPOs.get(spo.getId()))
                            .build();

                    putPoToCache(newSecondaryIpo);
                }

            } else {
                handleParsingError(spoXmlFile,
                    "callsite-obligation fvid= " + spoCallSiteObligation.fvid + " refers non-existent file \'"
                            + spoCallSiteObligation.location.file + "\'");
            }
        }
    }

    private void putPoToCache(IssuableProofObligation proofObligation) {
        fsAbstraction.save(proofObligation);
    }

    private boolean saveParsingIssueToSq(XmlParsingIssue pi) {

        Preconditions.checkNotNull(pi.getFile());

        final InputFile inputFile = fsAbstraction.getXmlAbsoluteResource(pi.getFile());
        Preconditions.checkNotNull(inputFile);

        final Issuable issuable = perspectives.as(Issuable.class, inputFile);

        if (null == issuable) {
            LOG.error(
                "Can't find an Issuable corresponding to InputFile:" + inputFile.absolutePath());
            return false;
        } else {
            try {
                final Issue issue = pi.toIssue(inputFile, issuable, activeRules, settings, fsAbstraction);
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

    FsAbstraction getFsContext() {
        return fsAbstraction;
    }

    List<IssuableProofObligation> processPPOs(PpoFile ppo)
            throws JAXBException {

        Preconditions.checkNotNull(ppo);

        final List<IssuableProofObligation> ret = new ArrayList<>();
        final PevFile pev = readPevXml(replaceSuffix(ppo.getOrigin(), PPO_SUFFIX, PEV_SUFFIX));
        final Map<String, PO> dischargedPOs = (pev == null) ? new HashMap<>() : pev.getDischargedPOsAsMap();

        for (final PrimaryProofObligation po : ppo.function.proofObligations) {
            final IpoKey key = new IpoKey(ppo.getOrigin(), ppo.function.name, po.getId(),
                    POLevel.PRIMARY);
            IssuableProofObligation ipo = fsAbstraction.get(key);

            if (ipo == null) {
                final InputFile resource = fsAbstraction.getResource(po.location.file);

                if (resource != null) {

                    ipo = IssuableProofObligation.newBuilder(ppo, po)
                            .setDischarge(dischargedPOs.get(po.getId()))
                            .setInputFile(resource)
                            .build();

                    putPoToCache(ipo);

                } else {
                    handleParsingError(ppo.getOrigin(),
                        "proof-obligation id=" + po.getId() + " refers non existing source file: \'" + po.location.file
                                + "\'");
                }
            }

            if (ipo != null) {
                ret.add(ipo);
            }
        }
        return ret;
    }

    SpoFile processSPOs(final File spoXml, boolean ignoreAssumptions) throws JAXBException {
        final SpoFile spo = readSpoXml(spoXml);

        if (spo != null) {
            final SevFile sev = readSevXml(replaceSuffix(spo.getOrigin(), SPO_SUFFIX, SEV_SUFFIX));
            final Map<String, PO> dischargedPOs = (sev == null) ? new HashMap<>() : sev.getDischargedPOsAsMap();

            for (final CallSiteObligation co : spo.function.spoWrapper.proofObligations) {
                processCallSiteObligation(spo, co, dischargedPOs);
            }

            /**
             * XXX: there are <post-expectations> as well!
             */

            if (!ignoreAssumptions) {
                for (final CallSiteObligation co : spo.function.spoWrapper.proofObligations) {
                    linkAssumptions(spo, co);
                }
            }
        }
        return spo;
    }

    String relativize(File f) {
        return fsAbstraction.getBaseDir().toPath().relativize(f.toPath()).toString();
    }

}
