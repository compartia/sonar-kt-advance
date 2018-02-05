package org.sonar.plugins.kt.advance.batch;

import org.sonar.api.batch.fs.InputFile;

import kt.advance.model.CApplication;
import kt.advance.model.CFile;

public interface SonarResourceLocator {
    public InputFile getResource(CApplication app, CFile cfile);
}
