package com.github.alexeyanufriev.advancedenforcerrules.tests;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class AdvancedDependencyConvergenceTest {

    private Properties properties;

    @Before
    public void setup() throws IOException {
        if (this.properties == null) {
            this.properties = new Properties();
            this.properties.load(getClass().getResourceAsStream("/test.properties"));
        }
    }

    @Test
    public void shouldFailWhenDuplicates() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/error-having-duplicates");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.addCliOption("-Drules-version=" + this.properties.getProperty("version"));
        verifier.setAutoclean(false);

        try {
            verifier.executeGoal("validate");
            Assert.fail("Build failure expected");
        }
        catch (Exception e) {
            // expected error
        }

        verifier.verifyTextInLog("[WARNING] Dependency convergence error for 'org.slf4j:slf4j-api:jar:1.7.32:compile'");
        verifier.verifyTextInLog("[WARNING] Dependency convergence error for 'com.fasterxml.jackson.core:jackson-databind:jar:2.12.6.1:compile'");
        verifier.verifyTextInLog("[INFO] BUILD FAILURE");
    }

    @Test
    public void shouldFailWhenDuplicatesInDefaultScopes() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/error-having-duplicates-in-default-scopes");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.addCliOption("-Drules-version=" + this.properties.getProperty("version"));
        verifier.setAutoclean(false);

        try {
            verifier.executeGoal("validate");
            Assert.fail("Build failure expected");
        }
        catch (Exception e) {
            // expected error
        }

        verifier.verifyTextInLog("[WARNING] Dependency convergence error for 'org.slf4j:slf4j-api:jar:1.7.32:compile'");
        verifier.verifyTextInLog("[WARNING] Dependency convergence error for 'com.fasterxml.jackson.core:jackson-databind:jar:2.12.6.1:compile'");
        verifier.verifyTextInLog("[INFO] BUILD FAILURE");
    }

    @Test
    public void shouldNotFailWhenNoDuplicates() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/no-error-having-no-duplicates");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.addCliOption("-Drules-version=" + this.properties.getProperty("version"));
        verifier.setAutoclean(false);

        verifier.executeGoal("validate");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] BUILD SUCCESS");
    }

    @Test
    public void shouldNotFailWhenDuplicatesSuppressed() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/no-error-having-duplicates");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.addCliOption("-Drules-version=" + this.properties.getProperty("version"));
        verifier.setAutoclean(false);

        verifier.executeGoal("validate");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] AdvancedDependencyConvergence rule is running in reporting-only mode");
        verifier.verifyTextInLog("[WARNING] Dependency convergence error for 'org.slf4j:slf4j-api:jar:1.7.32:compile'");
        verifier.verifyTextInLog("[WARNING] Dependency convergence error for 'com.fasterxml.jackson.core:jackson-databind:jar:2.12.6.1:compile'");
        verifier.verifyTextInLog("[INFO] BUILD SUCCESS");
    }

    @Test
    public void shouldNotFailWhenDuplicatesInSuppressedScope() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/no-error-having-duplicates-in-test-scope");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.addCliOption("-Drules-version=" + this.properties.getProperty("version"));
        verifier.setAutoclean(false);

        verifier.executeGoal("validate");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] BUILD SUCCESS");
    }

}
