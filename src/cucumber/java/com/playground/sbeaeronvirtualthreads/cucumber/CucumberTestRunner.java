package com.playground.sbeaeronvirtualthreads.cucumber;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * Cucumber test runner using JUnit Platform Suite
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource(".")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber/cucumber.html, json:build/reports/cucumber/cucumber.json")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.playground.sbeaeronvirtualthreads.cucumber")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/cucumber/resources")
public class CucumberTestRunner {
}
