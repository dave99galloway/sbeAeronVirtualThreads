package com.playground.sbeaeronvirtualthreads.cucumber;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource(".")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.playground.sbeaeronvirtualthreads.cucumber")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber/cucumber.html, json:build/reports/cucumber/cucumber.json")
public class CucumberTestRunner {
}
