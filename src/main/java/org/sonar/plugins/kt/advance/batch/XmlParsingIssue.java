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
        final XmlParsingIssue other = (XmlParsingIssue) obj;
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
        return true;
    }

    public File getFile() {
        return file;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
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