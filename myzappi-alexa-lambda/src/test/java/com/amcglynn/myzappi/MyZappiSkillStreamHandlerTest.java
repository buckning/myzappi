package com.amcglynn.myzappi;

import com.amcglynn.myzappi.core.config.ServiceManager;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * I forgot to register some intent handlers in the SkillStreamHandler before and wasted time figuring out what's wrong.
 * The purpose of this test is to prevent that from happening again.
 * If this test fails, make sure that you add your handler to MyZappiSkillStreamHandler and increment
 * EXPECTED_NUMBER_OF_INTENT_HANDLERS.
 */
class MyZappiSkillStreamHandlerTest {

    private static final int EXPECTED_NUMBER_OF_INTENT_HANDLERS = 13;

    @Test
    void testConstructorDoesNotThrowAnException() {
        new MyZappiSkillStreamHandler(mock(ServiceManager.class));
    }

    @Test
    void testAllIntentHandlersAreRegistered() throws Exception {
        var filesInPackage = getClassNamesInPackage("com.amcglynn.myzappi.handlers")
                .stream().filter(file -> !file.endsWith("Test")).collect(Collectors.toList());
        assertThat(filesInPackage)
                .describedAs("Make sure to register your new RequestHandler in " +
                    "MySkillStreamHandler and then change this count manually in this test once it is done")
                .hasSize(EXPECTED_NUMBER_OF_INTENT_HANDLERS);
    }

    public List<String> getClassNamesInPackage(String packageName) throws IOException {
        var fileList = new ArrayList<String>();
        var classLoader = Thread.currentThread().getContextClassLoader();
        var path = packageName.replace('.', '/');
        var resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            File dir = new File(resources.nextElement().getFile());
            assertThat(dir).isNotNull().isDirectory();
            for (File file : dir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    try {
                        fileList.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)).getName());
                    } catch (ClassNotFoundException e) {
                        // Ignore classes that cannot be loaded
                    }
                }
            }
        }
        return fileList;
    }
}
