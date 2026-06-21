package io.github.connellite.jdbc;

import io.github.connellite.jdbc.isolated.IsolatedPojo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleResultSetBeanMapperCacheTest {

    @AfterEach
    void restoreDefaultCache() {
        SimpleResultSetBeanMapper.resetMetadataCache();
    }

    @Test
    void reusesCachedMetadataForSameBeanClass() throws SQLException {
        SimpleResultSetBeanMapper<CachedPojoA> first = new SimpleResultSetBeanMapper<>(CachedPojoA.class);
        SimpleResultSetBeanMapper<CachedPojoA> second = new SimpleResultSetBeanMapper<>(CachedPojoA.class);

        assertTrue(first.sharesMetadataWith(second));
    }

    @Test
    void usesDistinctMetadataForDifferentBeanClasses() throws SQLException {
        SimpleResultSetBeanMapper<CachedPojoA> first = new SimpleResultSetBeanMapper<>(CachedPojoA.class);
        SimpleResultSetBeanMapper<CachedPojoB> second = new SimpleResultSetBeanMapper<>(CachedPojoB.class);

        assertFalse(first.sharesMetadataWith(second));
    }

    @Test
    void usesDistinctMetadataForSameClassNameFromDifferentClassLoaders() throws Exception {
        Class<?> standardClass = IsolatedPojo.class;

        URL resource = SimpleResultSetBeanMapperCacheTest.class.getResource("/classloader-isolated");
        if (resource == null) {
            throw new IllegalStateException("Missing test resource /classloader-isolated");
        }
        URL classUrl = resource.toURI().toURL();

        ClassLoader customParent = new ClassLoader(ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded != null) {
                        return loaded;
                    }
                    if (name.startsWith("java.") || name.startsWith("jdk.")) {
                        return getParent().loadClass(name);
                    }
                    throw new ClassNotFoundException(name);
                }
            }
        };

        Class<?> customClass;
        try (URLClassLoader customLoader = new URLClassLoader(new URL[]{classUrl}, customParent)) {
            customClass = customLoader.loadClass(standardClass.getName());
        }

        assertEquals(standardClass.getName(), customClass.getName());
        assertNotEquals(standardClass, customClass);
        assertEquals(standardClass.getClassLoader(), SimpleResultSetBeanMapperCacheTest.class.getClassLoader());
        assertNotEquals(standardClass.getClassLoader(), customClass.getClassLoader());

        SimpleResultSetBeanMapper<?> fromStandardLoader = new SimpleResultSetBeanMapper<>(standardClass);
        SimpleResultSetBeanMapper<?> fromCustomLoader = new SimpleResultSetBeanMapper<>(customClass);
        SimpleResultSetBeanMapper<?> fromStandardLoaderAgain = new SimpleResultSetBeanMapper<>(standardClass);

        assertFalse(fromStandardLoader.sharesMetadataWith(fromCustomLoader));
        assertTrue(fromStandardLoader.sharesMetadataWith(fromStandardLoaderAgain));
    }

    @Test
    void rebuildsMetadataAfterCacheReset() throws SQLException {
        SimpleResultSetBeanMapper<CachedPojoA> first = new SimpleResultSetBeanMapper<>(CachedPojoA.class);
        SimpleResultSetBeanMapper.resetMetadataCache();
        SimpleResultSetBeanMapper<CachedPojoA> second = new SimpleResultSetBeanMapper<>(CachedPojoA.class);

        assertFalse(first.sharesMetadataWith(second));
    }

    @Test
    void cacheIsThreadSafeForSameBeanClass() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<SimpleResultSetBeanMapper<CachedPojoA>>> tasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                tasks.add(() -> new SimpleResultSetBeanMapper<>(CachedPojoA.class));
            }
            List<Future<SimpleResultSetBeanMapper<CachedPojoA>>> futures = executor.invokeAll(tasks);
            SimpleResultSetBeanMapper<CachedPojoA> reference = futures.get(0).get();
            for (Future<SimpleResultSetBeanMapper<CachedPojoA>> future : futures) {
                assertTrue(reference.sharesMetadataWith(future.get()));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    static class CachedPojoA {
        private String name;
    }

    static class CachedPojoB {
        private int value;
    }
}
