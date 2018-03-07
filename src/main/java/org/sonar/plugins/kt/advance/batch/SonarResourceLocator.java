package org.sonar.plugins.kt.advance.batch;

import org.sonar.api.batch.fs.InputFile;

import com.kt.advance.api.CFile;

public interface SonarResourceLocator {
    public InputFile getResource(CFile cfile);
}
