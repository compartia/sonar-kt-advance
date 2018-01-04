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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.batch.PredicateTypes.PredicateKey;

import com.kt.advance.xml.model.ApiFile.ApiAssumption;

public class IssueBuilderTest {

    private static final String SRC_MEMTEST_C = "src/memtest.c";
    private static File BASEDIR;
    private static File MODULE_BASEDIR;

    private static final Logger LOG = Loggers.get(IssueBuilderTest.class.getName());

    static DefaultFileSystem fileSystem;
    static ActiveRules activeRules;
    static Settings settings;
    static DefaultInputFile inputFile;

    @BeforeClass
    public static void setup() throws JAXBException {
        final URL url = DirScanTest.class.getResource("/test_project");
        BASEDIR = new File(url.getFile());
        MODULE_BASEDIR = new File(BASEDIR, "redis/");

        fileSystem = new DefaultFileSystem(MODULE_BASEDIR.toPath());

        inputFile = Factory.makeDefaultInputFile(MODULE_BASEDIR, SRC_MEMTEST_C, 282);
        fileSystem.add(inputFile);

        activeRules = mock(ActiveRules.class);
        final ActiveRule ruleMock = mock(ActiveRule.class);
        when(ruleMock.param(any())).thenReturn("1.0");
        when(activeRules.find(any(RuleKey.class))).thenReturn(ruleMock);

        settings = mock(Settings.class);
        when(settings.getFloat(any())).thenReturn(1f);
    }

    @Test
    public void testAddReferences() {
        final int numberOfRefs = 10;
        final IssuableProofObligation ipo = Factory.createPrimaryPO(new PredicateKey("tag"));

        for (int f = 0; f < numberOfRefs; f++) {
            final IssuableProofObligation targetipo = Factory.createPrimaryPO(new PredicateKey("tag"));
            targetipo.setShortDescription("descr " + f);
            targetipo.setFunctionName("function." + f);
            ipo.addReference(targetipo, new ApiAssumption(), "api");
        }

        final Issuable issuableMock = mock(Issuable.class);
        final IssueBuilder iBuilderMock = mock(IssueBuilder.class);
        final NewIssueLocation primaryLocationMock = mock(NewIssueLocation.class);

        when(iBuilderMock.newLocation()).thenReturn(primaryLocationMock);
        when(iBuilderMock.ruleKey(any())).thenReturn(iBuilderMock);
        when(iBuilderMock.effortToFix(any())).thenReturn(iBuilderMock);
        when(iBuilderMock.at(any())).thenReturn(iBuilderMock);

        when(primaryLocationMock.at(any())).thenReturn(primaryLocationMock);
        when(primaryLocationMock.on(any())).thenReturn(primaryLocationMock);
        when(primaryLocationMock.message(any())).thenReturn(primaryLocationMock);
        when(issuableMock.newIssueBuilder()).thenReturn(iBuilderMock);

        final FsAbstraction fs = mock(FsAbstraction.class);
        when(fs.getResource(any())).thenReturn(inputFile);
        //
        ipo.toIssue(issuableMock, activeRules, settings, fs);
        //
        verify(iBuilderMock, times(1)).build();
        verify(iBuilderMock, times(numberOfRefs)).addLocation(any());
    }

}
