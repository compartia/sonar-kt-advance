package com.kt.advance;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.sonar.plugins.kt.advance.batch.IpoKey;
import org.sonar.plugins.kt.advance.batch.ScanFailedException;

import kt.advance.model.PO;

/**
 * not in use presently XXX: use it or remove it
 * 
 * 
 */
@Deprecated
public class CacheTools {
    ///////////////////////////
    @FunctionalInterface
    public interface InCacheJob {
        void perform() throws JAXBException;
    }

    private static final String PERSISTENT_CACHE_NAME = "persistent-cache";
    private final File baseDir;

    private PersistentCacheManager cacheMan;
    Cache<IpoKey, PO> cache;

    CacheTools(File baseDir) {
        this.baseDir = baseDir;
    }

    public synchronized void closeFile() {
        cacheMan.removeCache(PERSISTENT_CACHE_NAME);
        cacheMan.close();
    }

    public void doInCache(CacheTools.InCacheJob job) {
        try {
            openFile();
            job.perform();
            closeFile();

        } catch (final JAXBException e) {
            throw new ScanFailedException(e);
        }

    }

    public synchronized void openFile() throws JAXBException {

        final CacheConfigurationBuilder<IpoKey, PO> configurationBuilder = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(
                    IpoKey.class,
                    PO.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(2, MemoryUnit.GB)//TODO: should be configurable
                            .disk(20, MemoryUnit.GB, false));

        final File cacheDir = new File(baseDir, ".sonar.kt.data");
        cacheMan = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(cacheDir.getAbsolutePath()))
                .withCache(PERSISTENT_CACHE_NAME, configurationBuilder)
                .build(true);

        cache = cacheMan.getCache(PERSISTENT_CACHE_NAME, IpoKey.class, PO.class);
    }
}
