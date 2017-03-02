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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
//---
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
//---
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.kt.advance.model.ApiFile;
import org.sonar.plugins.kt.advance.model.HasOriginFile;
import org.sonar.plugins.kt.advance.model.PevFile;
import org.sonar.plugins.kt.advance.model.PpoFile;
import org.sonar.plugins.kt.advance.model.SevFile;
import org.sonar.plugins.kt.advance.model.SpoFile;
import org.sonar.plugins.kt.advance.util.XmlParser;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;

public class FsAbstraction {
    @FunctionalInterface
    public interface CachedDataProvider {
        public void read() throws JAXBException;
    }

    ///////////////////////////
    @FunctionalInterface
    public interface InCacheJob {
        void perform() throws JAXBException;
    }

    @FunctionalInterface
    public interface PpoFileParser {
        void parse(File file);
    }

    /**
     * Cache for type-specific unmarshaller and JAXBContext
     *
     * @author artem
     *
     */
    static class XMLType<T> {
        private final JAXBContext ppoJaxbContext;
        private final Unmarshaller unmarshaller;

        public XMLType(Class<T> classesToBeBound) throws JAXBException {
            ppoJaxbContext = JAXBContext.newInstance(classesToBeBound);
            unmarshaller = ppoJaxbContext.createUnmarshaller();
        }

        @SuppressWarnings("unchecked")
        public T readXml(File file) throws JAXBException {

            if (!file.isFile()) {
                KtAdvanceSensorRunner.LOG.warn("not found " + file.getAbsolutePath());
                return null;
            }

            LOG.debug("reading " + file.getName());
            final XmlParser parser = new XmlParser();
            parser.parse(file);
            final T obj = (T) unmarshaller.unmarshal(parser.getRoot());

            if (obj instanceof HasOriginFile) {
                ((HasOriginFile) obj).setOrigin(file);
            }
            return obj;
        }
    }

    private static final String PERSISTENT_CACHE_NAME = "persistent-cache";

    private static final Logger LOG = Loggers.get(FsAbstraction.class.getName());

    public static final String XML_EXT = "xml";
    public static final String SEV_SUFFIX = "_sev";

    public static final String API_SUFFIX = "_api";

    public static final String SPO_SUFFIX = "_spo";

    public static final String PPO_SUFFIX = "_ppo";
    public static final String PEV_SUFFIX = "_pev";
    static final IOFileFilter ppoFileFilter = new SuffixFileFilter(
            FsAbstraction.xmlSuffix(PPO_SUFFIX),
            IOCase.INSENSITIVE);
    /**
     * lazy JAXBContext & Unmarshaller cache
     */
    static final Map<Class<?>, FsAbstraction.XMLType<?>> xmlTypes = new HashMap<>();
    private static final Map<String, List<String>> fileContentsCache = new MapMaker().softValues().makeMap();

    private final static Map<String, ApiFile> functionNameToApiMap = new HashMap<>();

    final FileSystem fileSystem;
    private final Map<String, InputFile> fsCache = new MapMaker().softValues().makeMap();

    private PersistentCacheManager cacheMan;

    Cache<IpoKey, IssuableProofObligation> cache;

    private final File baseDir;

    private final Set<IpoKey> savedKeys = new HashSet<>();

    private final HashSet<IpoKey> missingKeys = new HashSet<>();

    public FsAbstraction(File baseDir) {
        fileSystem = new DefaultFileSystem(baseDir);
        this.baseDir = baseDir;
    }

    public FsAbstraction(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        this.baseDir = fileSystem.baseDir();
    }

    public static <X> FsAbstraction.XMLType<X> getReader(Class<X> clazz) throws JAXBException {
        XMLType xmlType = xmlTypes.get(clazz);
        if (xmlType == null) {
            xmlType = new FsAbstraction.XMLType<X>(clazz);
            xmlTypes.put(clazz, xmlType);
        }
        return xmlType;
    }

    public static List<String> readInputFile(InputFile inputFile) {
        Preconditions.checkNotNull(inputFile);
        final String key = inputFile.absolutePath();
        Preconditions.checkNotNull(key);
        List<String> lines = fileContentsCache.get(key);

        if (lines == null) {
            try {
                LOG.info("reading  source" + inputFile.absolutePath());
                lines = FileUtils.readLines(inputFile.file(), (String) null);
            } catch (final IOException e) {
                lines = new ArrayList<>();
                LOG.error("cannont read" + inputFile.absolutePath(), e);
            }
            fileContentsCache.put(key, lines);
        }
        return lines;
    }

    public static File replaceSuffix(File file, String oldSuffix, String newuffix) {
        final String name = file.getName();
        final String newName = name.replace(oldSuffix, newuffix);
        return new File(file.getParentFile(), newName);
    }

    public static File xmlFilename(final File file, final String filePattern, String suff) {
        final StringBuilder sb = new StringBuilder()
                .append(filePattern)
                .append(suff)
                .append('.')
                .append(XML_EXT);
        return new File(file.getParentFile(), sb.toString());
    }

    public static String xmlSuffix(String postfix) {
        return postfix + "." + XML_EXT;
    }

    static ApiFile readApiXml(File file) throws JAXBException {
        if (functionNameToApiMap.containsKey(file.getAbsolutePath())) {
            return functionNameToApiMap.get(file.getAbsolutePath());
        }
        return getReader(ApiFile.class).readXml(file);
    }

    static PevFile readPevXml(File file) throws JAXBException {
        return getReader(PevFile.class).readXml(file);
    }

    static PpoFile readPpoXml(File file) throws JAXBException {
        return getReader(PpoFile.class).readXml(file);
    }

    static SevFile readSevXml(File file) throws JAXBException {
        return getReader(SevFile.class).readXml(file);
    }

    static SpoFile readSpoXml(File file) throws JAXBException {
        return getReader(SpoFile.class).readXml(file);
    }

    static <X> X readXml(Class<X> c, File file) throws JAXBException {
        return getReader(c).readXml(file);
    }

    public void cacheApiFile(final File apiXml) {
        try {
            final ApiFile api = FsAbstraction.readApiXml(apiXml);
            functionNameToApiMap.put(apiXml.getAbsolutePath(), api);
            functionNameToApiMap.put(api.function.name, api);
            functionNameToApiMap.put(api.function.cfilename + "::" + api.function.name, api);

        } catch (final JAXBException e) {
            LOG.error("XML parsing failed: " + e.getMessage());
        }
    }

    public void cacheApiFiles(String funcname) {
        forEachApiFile(funcname, this::cacheApiFile);
        LOG.info("cached " + functionNameToApiMap.size() + " function APIs");
    }

    public void doInCache(InCacheJob job) {
        try {
            openFile();
            job.perform();
            closeFile();

        } catch (final JAXBException e) {
            throw new ScanFailedException(e);
        }

    }

    /**
     * iterates over *_ppo.xml files
     *
     * @param handler
     */
    public void forEachPpoFile(PpoFileParser handler) {
        LOG.info("Analysing. Source root: " + baseDir.getAbsolutePath());
        final FilePredicate filePredicate = fileSystem.predicates().matchesPathPattern("**/*_ppo.xml");
        final Iterable<InputFile> files = fileSystem.inputFiles(filePredicate);
        for (final InputFile file : files) {
            handler.parse(file.file());
        }
    }

    @Deprecated
    public ApiFile getApiByFunc(String funcname, String preferableSourceFileName) {
        //XXX: this is completely vague.
        ApiFile apiFile = functionNameToApiMap.get(funcname);
        if (apiFile == null) {
            this.cacheApiFiles(funcname);
        }
        apiFile = functionNameToApiMap.get(preferableSourceFileName + "::" + funcname);
        if (apiFile == null) {
            return functionNameToApiMap.get(funcname);
        } else {
            return apiFile;
        }
    }

    public File getBaseDir() {
        return baseDir;
    }

    public IssuableProofObligation getFromCache(IpoKey key, boolean requred) {
        final IssuableProofObligation ipo = cache.get(key);
        if (requred) {
            Preconditions.checkNotNull(ipo);
        }
        return ipo;
    }

    public IssuableProofObligation getFromCache(IpoKey key, CachedDataProvider reader) {

        if (missingKeys.contains(key)) {
            return null;
        }

        final IssuableProofObligation issuableProofObligation = cache.get(key);

        if (issuableProofObligation == null) {
            missingKeys.add(key);
            if (reader != null) {
                try {
                    reader.read();
                } catch (final JAXBException e) {
                    throw new RuntimeException(e);
                }
            }
            return cache.get(key);
        } else {
            return issuableProofObligation;
        }
    }

    public InputFile getResource(final String file) {
        Preconditions.checkNotNull(file);

        InputFile inputFile = fsCache.get(file);
        if (inputFile == null) {
            final FilePredicate filePredicate = fileSystem.predicates().hasRelativePath(file);
            inputFile = fileSystem.inputFile(filePredicate);
            if (inputFile == null) {
                LOG.error("cannot find " + file + " in " + fileSystem.baseDir().getAbsolutePath());
                return null;
            } else {
                LOG.info("cached " + inputFile.relativePath());
            }
            fsCache.put(file, inputFile);
        }
        return inputFile;
    }

    public Set<IpoKey> getSavedKeys() {
        return Collections.unmodifiableSet(savedKeys);
    }

    public InputFile getXmlAbsoluteResource(final File file) {

        final FilePredicate filePredicate = fileSystem.predicates().is(file);//hasARelativePath(relative);
        final InputFile inputFile = fileSystem.inputFile(filePredicate);

        if (inputFile == null) {
            LOG.error("cannot find '" + file.getAbsolutePath());
        }
        return inputFile;
    }

    public void save(IssuableProofObligation ipo) {
        if (ipo == IssuableProofObligation.MISSING) {
            throw new IllegalStateException();
        }
        final IpoKey key = ipo.getKey();
        savedKeys.add(key);
        cache.put(key, ipo);
        missingKeys.remove(key);

    }

    private synchronized void closeFile() {
        cacheMan.removeCache(PERSISTENT_CACHE_NAME);
        cacheMan.close();
    }

    private synchronized void openFile() throws JAXBException {

        final CacheConfigurationBuilder<IpoKey, IssuableProofObligation> configurationBuilder = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(
                    IpoKey.class,
                    IssuableProofObligation.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(2, MemoryUnit.GB)//TODO: should be configurable
                            .disk(20, MemoryUnit.GB, false));

        final File cacheDir = new File(baseDir, ".sonar.kt.data");
        cacheMan = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(cacheDir.getAbsolutePath()))
                .withCache(PERSISTENT_CACHE_NAME, configurationBuilder)
                .build(true);

        cache = cacheMan.getCache(PERSISTENT_CACHE_NAME, IpoKey.class, IssuableProofObligation.class);
    }

    protected void forEachApiFile(String funcname, PpoFileParser handler) {
        LOG.info("Analysing. Source root: " + baseDir.getAbsolutePath());
        final String inclusionPattern = "**/*" + funcname + "_api.xml";
        final FilePredicate filePredicate = fileSystem.predicates().matchesPathPattern(inclusionPattern);
        final Iterable<InputFile> files = fileSystem.inputFiles(filePredicate);
        for (final InputFile file : files) {
            handler.parse(file.file());
        }
    }

}
