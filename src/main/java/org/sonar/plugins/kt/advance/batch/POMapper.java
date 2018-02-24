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

import kt.advance.model.CApplication;
import kt.advance.model.CFunction;
import kt.advance.model.CLocation;
import kt.advance.model.Definitions;
import kt.advance.model.Definitions.POLevel;
import kt.advance.model.PO;
import kt.advance.model.PPO;
import kt.advance.model.SPO;

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

    public final Issue toIssue(PPO po, Issuable issuable, SonarResourceLocator fs, CApplication app, CFunction fun) {
        return _toIssue(po, issuable, fs, app, fun);
    }

    public final Issue toIssue(SPO po, Issuable issuable, SonarResourceLocator fs, CApplication app, CFunction file) {
        return _toIssue(po, issuable, fs, app, file);
    }

    private final Issue _toIssue(PO po, Issuable issuable, SonarResourceLocator fs, CApplication app, CFunction fun) {

        Preconditions.checkNotNull(po);
        Preconditions.checkNotNull(issuable);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(fun);
        Preconditions.checkNotNull(fs);

        //        CFile locCFile = app.getCFileStrictly(po.getLocation().file);
        //        P
        final InputFile inputFile = fs.getResource(app, fun.getCfile());

        Preconditions.checkNotNull(inputFile);

        final RuleKey ruleKey = getRuleKey(po);

        //////////////

        final Issuable.IssueBuilder issueBuilder = issuable.newIssueBuilder();

        final CLocation poLoc = po.getLocation();
        final NewIssueLocation primaryLocation = issueBuilder.newLocation()
                .on(inputFile)
                .at(makeSonarLocation(inputFile, poLoc))
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
                final InputFile file = fs.getResource(app, app.getCFileStrictly(spo.getLocation().file));
                Preconditions.checkNotNull(file);

                final NewIssueLocation loc = issueBuilder.newLocation()
                        .on(file)
                        .at(makeSonarLocation(file, spo.getLocation()))
                        .message(spo.deps != null ? spo.deps.toString() : "-//-");

                issueBuilder.addLocation(loc);

            });

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

    private TextRange makeSonarLocation(final InputFile inputFile, final CLocation poLoc) {
        return inputFile.newRange(poLoc.line, 0, poLoc.line, 0);
    }
}
