package org.sonar.plugins.kt.advance.batch;

import static org.sonar.plugins.kt.advance.batch.EffortComputer.oneIfNull;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_LEVEL_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PO_STATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.PARAM_EFFORT_PREDICATE_SCALE;
import static org.sonar.plugins.kt.advance.batch.PluginParameters.paramKey;

import java.util.Set;

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
import com.kt.advance.api.CApplication;
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

    public final Issue toIssue(PPO po, Issuable issuable, SonarResourceLocator fs, CApplication app,
            CFunction fun) {
        return _toIssue(po, issuable, fs, app, fun);
    }

    public final Issue toIssue(SPO po, Issuable issuable, SonarResourceLocator fs, CApplication app, CFunction file) {
        return _toIssue(po, issuable, fs, app, file);
    }

    private final Issue _toIssue(PO po, Issuable issuable, SonarResourceLocator fs, CApplication app,
            CFunction fun) {

        Preconditions.checkNotNull(po);
        Preconditions.checkNotNull(issuable);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(fun);
        Preconditions.checkNotNull(fs);

        final InputFile inputFile = fs.getResource(app, fun.getCfile());

        Preconditions.checkNotNull(inputFile);

        final RuleKey ruleKey = getRuleKey(po);

        //////////////

        final Issuable.IssueBuilder issueBuilder = issuable.newIssueBuilder();

        final CLocation poLoc = po.getLocation();
        final NewIssueLocation primaryLocation = issueBuilder.newLocation()
                .on(inputFile)
                .at(toSonarTextRange(inputFile, poLoc))
                .message(getDescription(po));

        //XXX: map textRange

        issueBuilder
                .ruleKey(ruleKey)
                .effortToFix(computeEffort(po, activeRules, settings))
                .at(primaryLocation);

        //TODO: move to other method
        if (po instanceof PPO) {
            addLocationsToIssue(issueBuilder, fs, (PPO) po, fun, app);
        }

        return issueBuilder.build();

    }

    private Issuable.IssueBuilder addLocationsToIssue(final Issuable.IssueBuilder issueBuilder,
            SonarResourceLocator fs, PPO ppo, CFunction fun, CApplication app) {

        final Set<SPO> associatedSpos = ppo.getAssociatedSpos(fun);

        associatedSpos.stream().forEach(
            spo -> {
                final InputFile file = fs.getResource(app, spo.getLocation().getCfile());
                if (file != null) {
                    final NewIssueLocation loc = issueBuilder.newLocation()
                            .on(file)
                            .at(toSonarTextRange(file, spo.getLocation()))
                            .message(spo.getDeps() != null ? spo.getDeps().toString() : REF_DEFAULT_MESSAGE);

                    issueBuilder.addLocation(loc);
                } else {
                    LOG.warn("cannot find resource  " + spo.getLocation().toString());
                }

            });

        return issueBuilder;
    }

    private TextRange toSonarTextRange(final InputFile inputFile, final CLocation poLoc) {
        return inputFile.newRange(poLoc.getLine(), 0, poLoc.getLine(), 0);
    }
}
