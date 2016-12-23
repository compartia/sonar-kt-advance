package org.sonar.plugins.kt.advance.batch;

import java.io.File;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;

public class XmlParsingIssue {
    private File file;
    private String message;

    public File getFile() {
        return file;
    }

    public String getMessage() {
        return message;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Issue toIssue(InputFile inputFile, Issuable issuable, ActiveRules activeRules, Settings settings,
            FsAbstraction fs) {

        final RuleKey ruleKey = RuleKey.of(KtAdvanceRulesDefinition.XML_PROBLEMS_REPO_KEY,
            KtAdvanceRulesDefinition.XML_INCONSISTENCY_RULE);

        final Issuable.IssueBuilder issueBuilder = issuable.newIssueBuilder();

        final NewIssueLocation primaryLocation = issueBuilder.newLocation()
                .on(inputFile)
                .message(getMessage());

        issueBuilder
                .ruleKey(ruleKey)
                .at(primaryLocation);

        return issueBuilder.build();

    }
}