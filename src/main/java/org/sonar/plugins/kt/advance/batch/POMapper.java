package org.sonar.plugins.kt.advance.batch;

import static org.sonar.plugins.kt.advance.batch.EffortComputer.oneIfNull;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_LEVEL_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PO_STATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PREDICATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.paramKey;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
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
import com.kt.advance.api.CFunction;
import com.kt.advance.api.CLocation;
import com.kt.advance.api.Definitions;
import com.kt.advance.api.Definitions.POLevel;
import com.kt.advance.api.PO;
import com.kt.advance.api.PPO;
import com.kt.advance.api.SPO;

public class POMapper {
    private static final String REF_DEFAULT_MESSAGE = "-//-";
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
            effortComputer.setStateMultiplier(oneIfNull(settings.getFloat(paramKey(po.getStatus()))));
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

        //        sb.append(po.getId()).append(" ");
        sb.append(po.getLevel() == POLevel.SECONDARY ? "Secondary; " : "");
        if (null != po.getExplaination()) {
            sb.append(po.getExplaination()).append("; ");
        }
        sb.append("[").append(po.getPredicate().express()).append("]");

        if (po.getDeps().level != Definitions.DepsLevel.s /* self */
                && po.getDeps().level != Definitions.DepsLevel.i /* unknown */) {
            sb.append("; ").append(po.getDeps().level.toString());
        }

        return sb.toString();
    }

    public RuleKey getRuleKey(PO po) {
        return RuleKey.of(
            KtAdvanceRulesDefinition.REPOSITORY_BASE_KEY + po.getStatus(),
            "predicate_" + po.getPredicate().type.name());//XXX: use code, not label
    }

    public final Issue toIssue(PPO po, Issuable issuable, SonarResourceLocator fs,
            CFunction fun) {
        return _toIssue(po, issuable, fs, fun, po.getLocation());
    }

    public final Issue toIssue(SPO po, Issuable issuable, SonarResourceLocator fs, CFunction file) {
        return _toIssue(po, issuable, fs, file, po.getSite().getLocation());
    }

    private final Issue _toIssue(PO po, Issuable issuable, SonarResourceLocator fs,
            CFunction fun, CLocation poLoc) {

        Preconditions.checkNotNull(po);
        Preconditions.checkNotNull(issuable);

        Preconditions.checkNotNull(fun);
        Preconditions.checkNotNull(fs);

        final InputFile inputFile = fs.getResource(fun.getCfile());
        Preconditions.checkNotNull(inputFile, "cannot find resource %s", fun.getCfile().getName());

        final RuleKey ruleKey = getRuleKey(po);

        //////////////

        final Issuable.IssueBuilder issueBuilder = issuable.newIssueBuilder();

        final NewIssueLocation primaryLocation = issueBuilder.newLocation()
                .on(inputFile)
                .at(toSonarTextRange(inputFile, poLoc))
                .message(getDescription(po));

        //XXX: map textRange

        issueBuilder
                .ruleKey(ruleKey)
                .effortToFix(computeEffort(po, activeRules, settings))
                .at(primaryLocation);

        return issueBuilder.build();

    }

    private TextRange toSonarTextRange(final InputFile inputFile, final CLocation poLoc) {
        return inputFile.newRange(poLoc.getLine(), 0, poLoc.getLine(), 0);
    }
}
