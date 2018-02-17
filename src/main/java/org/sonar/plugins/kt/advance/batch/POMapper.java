package org.sonar.plugins.kt.advance.batch;

import static org.sonar.plugins.kt.advance.batch.EffortComputer.oneIfNull;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_LEVEL_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PO_STATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PREDICATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.paramKey;

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

import com.google.common.base.Preconditions;

import kt.advance.model.CApplication;
import kt.advance.model.CFile;
import kt.advance.model.CLocation;
import kt.advance.model.Definitions;
import kt.advance.model.Definitions.POLevel;
import kt.advance.model.PO;

public class POMapper {
    static final Logger LOG = Loggers.get(POMapper.class.getName());
    final ActiveRules activeRules;

    final Settings settings;

    public POMapper(ActiveRules activeRules, Settings settings) {
        super();
        this.activeRules = activeRules;
        this.settings = settings;
    }

    public Double computeEffort(PO po, ActiveRules activeRules, Settings settings) {

        if (po.isSafe()) {
            /**
             * discharged predicates require no effort: <br>
             * TODO: this could be configurable.
             */
            return 0.0;

        } else {

            final ActiveRule rule = activeRules.find(getRuleKey(po));
            if (null == rule) {
                LOG.warn("no active rule with key " + getRuleKey(po));
                return 0.0;
            }

            final EffortComputer effortComputer = new EffortComputer();

            /** 1. PO state <code>s</code> */
            effortComputer.setStateMultiplier(oneIfNull(settings.getFloat(paramKey(po.status))));
            effortComputer.setStateScaleFactor(oneIfNull(settings.getFloat(PARAM_EFFORT_PO_STATE_SCALE)));

            /** 2. PO level primary versus secondary <code>l</code> */
            effortComputer.setPoLevelMultiplier(oneIfNull(settings.getFloat(paramKey(po.getLevel()))));
            effortComputer.setPoLevelScaleFactor(oneIfNull(settings.getFloat(PARAM_EFFORT_LEVEL_SCALE)));

            /** 3. predicate complexities <code>c</code> */
            //            effortComputer.setComplexityC(getComplexityC());
            //            effortComputer.setComplexityP(getComplexityP());
            //            effortComputer.setComplexityG(getComplexityG());
            //XXX: add complexity

            effortComputer.setComplexityCScaleFactor(oneIfNull(settings.getFloat(paramKey(POComplexity.C))));
            effortComputer.setComplexityPScaleFactor(oneIfNull(settings.getFloat(paramKey(POComplexity.P))));
            effortComputer.setComplexityGScaleFactor(oneIfNull(settings.getFloat(paramKey(POComplexity.G))));

            /** 4. predicate type <code>t</code> */
            effortComputer.setPredicateTypeMultiplier(oneIfNull(settings.getFloat(po.getPredicate().type.name())));
            effortComputer.setPredicateScaleFactor(oneIfNull(settings.getFloat(PARAM_EFFORT_PREDICATE_SCALE)));

            final double effort = effortComputer.compute();

            return effort;
        }
    }

    public String getDescription(PO po) {
        final StringBuffer sb = new StringBuffer();

        sb.append(po.id).append(" ");
        sb.append(po.getLevel() == POLevel.SECONDARY ? "Secondary; " : "");
        if (null != po.explaination) {
            sb.append(po.explaination).append("; ");
        }
        sb.append("[").append(po.getPredicate().express()).append("]");

        if (po.deps.level != Definitions.DepsLevel.s /* self */
                && po.deps.level != Definitions.DepsLevel.i /* unknown */) {
            sb.append("; ").append(po.deps.level.label);
        }

        return sb.toString();
    }

    public RuleKey getRuleKey(PO po) {
        return RuleKey.of(
            KtAdvanceRulesDefinition.REPOSITORY_BASE_KEY + po.status,
            "predicate_" + po.getPredicate().type.name());//XXX: use code, not label
    }

    public final Issue toIssue(PO po, Issuable issuable, SonarResourceLocator fs, CApplication app, CFile file) {

        Preconditions.checkNotNull(po);
        Preconditions.checkNotNull(issuable);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(fs);

        final InputFile inputFile = fs.getResource(app, file);

        Preconditions.checkNotNull(inputFile);

        final RuleKey ruleKey = getRuleKey(po);

        //////////////

        final Issuable.IssueBuilder issueBuilder = issuable.newIssueBuilder();

        final CLocation poLoc = po.getLocation();
        final NewIssueLocation primaryLocation = issueBuilder.newLocation()
                .on(inputFile)
                .at(inputFile.newRange(poLoc.line, 0, poLoc.line, 0))
                .message(getDescription(po));

        //        final Object textRange = null;
        //        if (textRange != null) {
        //            primaryLocation.at(textRange.toTextRange(inputFile));
        //        }

        //XXX: map textRange

        issueBuilder
                .ruleKey(ruleKey)
                .effortToFix(computeEffort(po, activeRules, settings))
                .at(primaryLocation);

        addLocationsToIssue(issueBuilder, fs);

        return issueBuilder.build();

    }

    private Issuable.IssueBuilder addLocationsToIssue(final Issuable.IssueBuilder issueBuilder,
            SonarResourceLocator fs) {

        //      XXX: implement this
        //        for (final Reference r : getReferences()) {
        //            if (!r.missing) {
        //                final InputFile file = fs.getResource(r.file);
        //
        //                final NewIssueLocation loc = issueBuilder.newLocation()
        //                        .on(file)
        //                        .at(r.textRange.toTextRange(file))
        //                        .message(r.message);
        //
        //                issueBuilder.addLocation(loc);
        //            }
        //        }
        //

        return issueBuilder;
    }
}
