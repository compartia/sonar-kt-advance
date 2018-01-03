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

import static org.sonar.plugins.kt.advance.batch.EffortComputer.oneIfNull;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_LEVEL_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PO_STATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PREDICATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.paramKey;
import static org.sonar.plugins.kt.advance.util.StringTools.findVarLocation;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POComplexity;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POLevel;
import org.sonar.plugins.kt.advance.batch.KtAdvanceRulesDefinition.POState;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;
import org.sonar.plugins.kt.advance.model.ApiFile.ApiAssumption;
import org.sonar.plugins.kt.advance.model.EvFile;
import org.sonar.plugins.kt.advance.model.EvFile.PO;
import org.sonar.plugins.kt.advance.model.GoodForCache;
import org.sonar.plugins.kt.advance.model.HasOriginFile;
import org.sonar.plugins.kt.advance.model.PpoFile;
import org.sonar.plugins.kt.advance.model.PpoFile.PoPredicate;
import org.sonar.plugins.kt.advance.model.PpoFile.PpoLocation;
import org.sonar.plugins.kt.advance.model.PpoFile.PrimaryProofObligation;
import org.sonar.plugins.kt.advance.model.PpoFile.Symbol;
import org.sonar.plugins.kt.advance.model.PpoFile.SymbolType;
import org.sonar.plugins.kt.advance.model.SpoFile;
import org.sonar.plugins.kt.advance.model.SpoFile.CallSiteObligation;
import org.sonar.plugins.kt.advance.model.SpoFile.SecondaryProofObligation;
import org.sonar.plugins.kt.advance.util.StringTools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

public class IssuableProofObligation implements GoodForCache {

    public abstract static class AbstractPOBuilder {
        private PO discharge;
        private InputFile inputFile;
        private final HasOriginFile xml;
        protected String functionName;

        public AbstractPOBuilder(HasOriginFile xml) {
            this.xml = xml;
        }

        public abstract IssuableProofObligation build();

        public PO getDischarge() {
            return discharge;
        }

        public InputFile getInputFile() {
            return inputFile;
        }

        public File getOriginXml() {
            return xml.getOrigin();
        }

        public IPOTextRange getTextRange(PoPredicate predicate) {
            if (getLocation() == null) {
                //                /XXX
                return new IPOTextRange(0, 0, 0, 0);
            }
            final Symbol symbol = predicate.getVarName();
            if (symbol != null) {

                final String varname = symbol.value;

                if (StringUtils.isNotBlank(varname)) {
                    final List<String> lines = FsAbstraction.readInputFile(inputFile);
                    if (!lines.isEmpty()) {

                        final int line = getLocation().line;
                        final String string = lines.get(line - 1);
                        final int start = findVarLocation(varname, string);

                        if (start >= 0) {
                            traceVarName(varname, line, string);
                            return new IPOTextRange(line, start, line, start + varname.length());
                        } else {
                            LOG.trace(inputFile.absolutePath() + ": " + line + "\t" +
                                    StringTools.quote(varname) + " not found  " + line + " in "
                                    + StringTools.quote(string.trim()) + " refer (" + getOriginXml() + ")");
                        }
                    }
                }
            }
            return new IPOTextRange(getLocation().line, null, null, null);

        }

        public AbstractPOBuilder setDischarge(PO discharge) {
            this.discharge = discharge;
            return this;
        }

        public AbstractPOBuilder setInputFile(InputFile inputFile) {
            Preconditions.checkNotNull(inputFile);
            this.inputFile = inputFile;
            return this;
        }

        private void traceVarName(final String varname, final int line, final String string) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(
                    varname + " found at L:" + line + " " + string + " on file "
                            + inputFile.relativePath());
            }
        }

        protected PredicateKey getPredicateType() {

            final PredicateKey dpt = getDischargePredicateType();
            if (dpt == null) {
                return new PredicateKey(getPredicate().tag);
            } else {
                return dpt;
            }

        }

        IssuableProofObligation buildTmp() {
            //            Preconditions.checkState(this.getInputFile() != null);
            final IssuableProofObligation ipo = new IssuableProofObligation();

            ipo.originXml = getOriginXml();
            ipo.fnameContext = IpoKey.ANY_FNAME_CONTEXT;
            ipo.id = getId();
            ipo.time = xml.getTime();
            ipo.location = getLocation();
            ipo.textRange = getTextRange(getPredicate());
            ipo.predicateType = getPredicateType();
            ipo.description = getDescription();
            ipo.shortDescription = getShortDescription();
            ipo.state = getRule();
            ipo.functionName = functionName;
            ipo.discharge = discharge;

            Symbol varName = getPredicate().getVarName();
            if (varName == null) {
                varName = new Symbol();
            }

            if (StringUtils.isBlank(varName.value)) {
                varName.value = getPredicate().getExpression();
                varName.type = SymbolType.CONST;
            }
            ipo.symbol = varName;
            return ipo;
        }

        String formatDescription(String prefix, String defaultBody, String suffix) {

            final StringBuilder sb = new StringBuilder();

            if (prefix != null) {
                sb.append(prefix);
            }
            if (getDischarge() != null) {
                sb.append(" ").append(getDischarge().evidence.comment).append(";");
            }
            if (defaultBody != null) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(defaultBody);
            }

            if (getDischarge() != null && getDischarge().method != null) {
                sb.append(": ").append(getDischarge().method);
            }
            if (suffix != null) {
                sb.append(" ").append(suffix);
            }

            return sb.toString();

        }

        abstract String getDescription();

        PredicateKey getDischargePredicateType() {
            if (discharge != null && discharge.predicateTag != null) {
                return new PredicateKey(discharge.predicateTag);
            }

            return null;
        }

        abstract String getId();

        abstract PpoLocation getLocation();

        abstract PoPredicate getPredicate();

        POState getRule() {
            if (discharge != null) {
                if (discharge.isViolated()) {
                    return POState.VIOLATION;
                } else {
                    return POState.DISCHARGED;
                }
            } else {
                return POState.OPEN;
            }
        }

        abstract String getShortDescription();

    }

    public static final class EVBuilder extends AbstractPOBuilder {
        EvFile origin;
        POLevel level;
        String sourceFileName;
        String functionName;

        public EVBuilder(EvFile origin, PO po) {
            super(origin);
            this.origin = origin;
            this.sourceFileName = origin.header.application.getFileNormalized();
            this.functionName = origin.function.name;
            super.setDischarge(po);
        }

        @Override
        public IssuableProofObligation build() {
            final IssuableProofObligation ipo = buildTmp();
            ipo.location = new PpoLocation();
            ipo.location.file = sourceFileName;
            ipo.level = level;
            ipo.functionName = functionName;

            ipo.missing = true;
            return ipo;
        }

        public POLevel getLevel() {
            return level;
        }

        public void setLevel(POLevel level) {
            this.level = level;
        }

        @Override
        String getDescription() {
            return getDischarge().evidence.comment;
        }

        @Override
        String getId() {
            return getDischarge().id;
        }

        @Override
        PpoLocation getLocation() {
            return null;
        }

        @Override
        PoPredicate getPredicate() {
            final PoPredicate p = new PoPredicate();
            p.tag = getDischarge().predicateTag;
            return p;
        }

        @Override
        String getShortDescription() {
            return getDischarge().evidence.comment;
        }

    }

    public static final class POBuilder extends AbstractPOBuilder {

        private final PrimaryProofObligation po;

        POBuilder(PpoFile origin, PrimaryProofObligation po) {
            super(origin);
            this.po = po;
            this.functionName = origin.function.name;
        }

        @Override
        public IssuableProofObligation build() {
            Preconditions.checkState(functionName != null);
            Preconditions.checkState(this.getInputFile() != null);
            final IssuableProofObligation ipo = super.buildTmp();
            ipo.level = POLevel.PRIMARY;
            ipo.complexity[POComplexity.C.ordinal()] = po.complexityC;
            ipo.complexity[POComplexity.P.ordinal()] = po.complexityP;

            return ipo;
        }

        @Override
        String getDescription() {
            return formatDescription("PO:", po.getDescription(), "");
        }

        @Override
        String getId() {
            return po.getId();
        }

        @Override
        PpoLocation getLocation() {
            return po.location;
        }

        @Override
        PoPredicate getPredicate() {
            return po.predicate;
        }

        @Override
        String getShortDescription() {
            return formatDescription(null, po.predicate.getExpression(), "");
        }

    }

    public static final class Reference implements GoodForCache {

        private static final long serialVersionUID = 1768805251161836506L;

        public final String file;
        public String apiId;
        public String type;
        public boolean missing;

        public final String message;
        public final String predicate;
        public final String referenceKey;
        public final String targetFuncName;
        public final IPOTextRange textRange;

        public final POState state;
        public final POLevel level;

        public Reference(String predicate,
                String file, IPOTextRange textRange, String message, String referenceKey,
                String targetFuncName,
                POLevel level, POState state) {
            super();
            this.predicate = predicate;
            this.file = file;
            this.textRange = textRange;
            this.message = message;
            this.referenceKey = referenceKey;
            this.targetFuncName = targetFuncName;
            this.level = level;
            this.state = state;
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
            final Reference other = (Reference) obj;
            if (file == null) {
                if (other.file != null) {
                    return false;
                }
            } else if (!file.equals(other.file)) {
                return false;
            }
            if (message == null) {
                if (other.message != null) {
                    return false;
                }
            } else if (!message.equals(other.message)) {
                return false;
            }
            if (referenceKey == null) {
                if (other.referenceKey != null) {
                    return false;
                }
            } else if (!referenceKey.equals(other.referenceKey)) {
                return false;
            }
            if (textRange == null) {
                if (other.textRange != null) {
                    return false;
                }
            } else if (!textRange.equals(other.textRange)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((file == null) ? 0 : file.hashCode());
            result = prime * result + ((message == null) ? 0 : message.hashCode());
            result = prime * result + ((referenceKey == null) ? 0 : referenceKey.hashCode());
            result = prime * result + ((textRange == null) ? 0 : textRange.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "Reference [file=" + file + ":" + textRange + ", message=" + message + "]";
        }

    }

    public static final class SPOBuilder extends AbstractPOBuilder {
        private SecondaryProofObligation spo;
        private final CallSiteObligation cso;

        SPOBuilder(SpoFile origin, CallSiteObligation cso) {
            super(origin);
            this.cso = cso;
            super.functionName = origin.function.name;
        }

        @Override
        public IssuableProofObligation build() {
            Preconditions.checkState(this.getInputFile() != null);

            final IssuableProofObligation ipo = super.buildTmp();
            ipo.level = POLevel.SECONDARY;
            /**
             * TODO: what about SPO C- and P- complexities??
             */
            ipo.complexity[POComplexity.G.ordinal()] = spo.complexityG;
            ipo.fnameContext = IpoKey.ANY_FNAME_CONTEXT;

            return ipo;
        }

        public AbstractPOBuilder setSpo(SecondaryProofObligation spo) {
            this.spo = spo;
            return this;
        }

        @Override
        String getDescription() {
            return formatDescription("SO:", this.spo.getDescription(), "");
        }

        @Override
        String getId() {
            return spo.getId();
        }

        @Override
        PpoLocation getLocation() {
            return cso.location;
        }

        @Override
        PoPredicate getPredicate() {
            return spo.predicate;
        }

        @Override
        String getShortDescription() {
            return formatDescription(null, spo.predicate.getExpression(), "");
        }

    }

    static final IssuableProofObligation MISSING = new IssuableProofObligation();

    private static final long serialVersionUID = 8626969892385862673L;

    private static final Logger LOG = Loggers.get(IssuableProofObligation.class.getName());

    /**
     * no info in PPO and SPO files. But might be something in SEV/PEV
     */
    @Deprecated
    private boolean missing = false;

    private PO discharge;

    /**
     * context-specific ID, not project-global
     */
    private String id;

    /**
     * C,P,G -- this is the order
     */
    private final Integer[] complexity = { 0, 0, 0 };

    private String description;

    private String functionName;
    private Symbol symbol;
    private String shortDescription;
    private PpoLocation location;
    private String time;

    private PredicateKey predicateType;

    private POLevel level;

    private POState state;

    private IPOTextRange textRange = new IPOTextRange(0, 0, 0, 0);

    private final Set<Reference> references = new HashSet<>();

    /**
     * TODO: use string?
     */
    @JsonIgnore
    private File originXml;

    private String fnameContext;
    int inReferencesCount = 0;

    private IssuableProofObligation() {

    }

    public static EVBuilder newBuilder(EvFile origin, PO po) {
        return new EVBuilder(origin, po);
    }

    public static POBuilder newBuilder(PpoFile originXml, PrimaryProofObligation po) {
        Preconditions.checkNotNull(po);
        Preconditions.checkNotNull(originXml);
        return new POBuilder(originXml, po);
    }

    public static SPOBuilder newBuilder(SpoFile originXml, CallSiteObligation cso) {
        Preconditions.checkNotNull(cso);
        Preconditions.checkNotNull(originXml);
        return new SPOBuilder(originXml, cso);
    }

    private static String makeReferenceName(IssuableProofObligation source, IssuableProofObligation target) {
        return "L" + source.getLocation().line + ": " + target.getDescription();
    }

    public Reference addReference(IssuableProofObligation target, ApiAssumption assumption, String type) {
        Preconditions.checkNotNull(target);
        Preconditions.checkArgument(!this.equals(target), "self connections are prohibited");
        Preconditions.checkNotNull(target.getLocation().file);
        Preconditions.checkNotNull(target.getFunctionName());

        final Reference ref = new Reference(
                assumption.predicate.tag,
                target.getLocation().file,//TOD: check if it is source or target!!!
                target.getTextRange(),
                makeReferenceName(this, target),
                target.getReferenceKey(),
                target.getFunctionName(),
                target.getLevel(),
                target.getState());

        ref.missing = target.isMissing();
        ref.apiId = assumption.nr;
        ref.type = type;
        references.add(ref);
        target.inReferencesCount++;
        return ref;
    }

    public Double computeEffort(ActiveRules activeRules, Settings settings) {

        if (isDischarged()) {
            /**
             * discharged predicates require no effort: <br>
             * TODO: this could be configurable.
             */
            return 0.0;

        } else {

            final ActiveRule rule = activeRules.find(getRuleKey());
            if (null == rule) {
                LOG.warn("no active rule with key " + getRuleKey());
                return 0.0;
            }

            final EffortComputer effortComputer = new EffortComputer();

            /** 1. PO state <code>s</code> */
            effortComputer.setStateMultiplier(oneIfNull(settings.getFloat(paramKey(state))));
            effortComputer.setStateScaleFactor(oneIfNull(settings.getFloat(PARAM_EFFORT_PO_STATE_SCALE)));

            /** 2. PO level primary versus secondary <code>l</code> */
            effortComputer.setPoLevelMultiplier(oneIfNull(settings.getFloat(paramKey(level))));
            effortComputer.setPoLevelScaleFactor(oneIfNull(settings.getFloat(PARAM_EFFORT_LEVEL_SCALE)));

            /** 3. predicate complexities <code>c</code> */
            effortComputer.setComplexityC(getComplexityC());
            effortComputer.setComplexityP(getComplexityP());
            effortComputer.setComplexityG(getComplexityG());

            effortComputer.setComplexityCScaleFactor(oneIfNull(settings.getFloat(paramKey(POComplexity.C))));
            effortComputer.setComplexityPScaleFactor(oneIfNull(settings.getFloat(paramKey(POComplexity.P))));
            effortComputer.setComplexityGScaleFactor(oneIfNull(settings.getFloat(paramKey(POComplexity.G))));

            /** 4. predicate type <code>t</code> */
            effortComputer.setPredicateTypeMultiplier(oneIfNull(settings.getFloat(getPredicateType().toString())));
            effortComputer.setPredicateScaleFactor(oneIfNull(settings.getFloat(PARAM_EFFORT_PREDICATE_SCALE)));

            final double effort = effortComputer.compute();

            if (LOG.isDebugEnabled()) {
                LOG.debug(effortComputer.toString() + " effort=" + effort);
            }

            return effort;
        }
    }

    public Integer getComplexityC() {
        return complexity[POComplexity.C.ordinal()];
    }

    public Integer getComplexityG() {
        return complexity[POComplexity.G.ordinal()];
    }

    public Integer getComplexityP() {
        return complexity[POComplexity.P.ordinal()];
    }

    @JsonIgnore
    public String getDescription() {
        return description;
    }

    public PO getDischarge() {
        return discharge;
    }

    public String getFile() {
        return getLocation().file;
    }

    public String getFunctionName() {
        return functionName;
    }

    public int getInReferencesCount() {
        return inReferencesCount;
    }

    @JsonIgnore
    public IpoKey getKey() {
        return new IpoKey(originXml, functionName, id);
    }

    public POLevel getLevel() {
        return level;
    }

    @JsonIgnore
    public PpoLocation getLocation() {
        return location;
    }

    @JsonIgnore
    public File getOriginXml() {
        return originXml;
    }

    public PredicateKey getPredicateType() {
        return predicateType;
    }

    public String getReferenceKey() {
        return getKey().toString();

    }

    public Set<Reference> getReferences() {
        return references;
    }

    @JsonIgnore
    public RuleKey getRuleKey() {
        return RuleKey.of(
            KtAdvanceRulesDefinition.REPOSITORY_BASE_KEY + getState(),
            predicateType.toString());
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public POState getState() {
        return state;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public IPOTextRange getTextRange() {
        return textRange;
    }

    public String getTime() {
        return time;
    }

    @JsonIgnore
    public boolean isDischarged() {
        return POState.DISCHARGED == getState();
    }

    @Deprecated
    public boolean isMissing() {
        return missing;
    }

    @JsonIgnore
    public boolean isOpen() {
        return POState.OPEN == getState();
    }

    @JsonIgnore
    public boolean isViolation() {
        return POState.VIOLATION == getState();
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public Issue toIssue(Issuable issuable, ActiveRules activeRules, Settings settings, FsAbstraction fs) {

        final InputFile inputFile = fs.getResource(getLocation().file);
        final RuleKey ruleKey = getRuleKey();

        //////////////

        final Issuable.IssueBuilder issueBuilder = issuable.newIssueBuilder();

        final NewIssueLocation primaryLocation = issueBuilder.newLocation()
                .on(inputFile)
                .message(getDescription());

        if (textRange != null) {
            primaryLocation.at(textRange.toTextRange(inputFile));
        }

        issueBuilder
                .ruleKey(ruleKey)
                .effortToFix(computeEffort(activeRules, settings))
                .at(primaryLocation);

        addLocationsToIssue(issueBuilder, fs);

        return issueBuilder.build();

    }

    @Override
    public String toString() {
        return "IssuableProofObligation [id=" + id + ", functionName=" + functionName + ", predicateType="
                + predicateType + ", level=" + level + ", state=" + state + "]";
    }

    private Issuable.IssueBuilder addLocationsToIssue(final Issuable.IssueBuilder issueBuilder, FsAbstraction fs) {

        for (final Reference r : getReferences()) {
            if (!r.missing) {
                final InputFile file = fs.getResource(r.file);

                final NewIssueLocation loc = issueBuilder.newLocation()
                        .on(file)
                        .at(r.textRange.toTextRange(file))
                        .message(r.message);

                issueBuilder.addLocation(loc);
            }
        }

        return issueBuilder;
    }

}
