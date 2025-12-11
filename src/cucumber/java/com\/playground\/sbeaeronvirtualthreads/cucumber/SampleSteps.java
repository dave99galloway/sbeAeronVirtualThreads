package com.playground.sbeaeronvirtualthreads.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import static org.assertj.core.api.Assertions.assertThat;

public class SampleSteps {

    @Given("the project is set up")
    public void theProjectIsSetUp() {
        // Setup code
    }

    @When("I run the tests")
    public void iRunTheTests() {
        // Test execution
    }

    @Then("they should pass")
    public void theyShouldPass() {
        assertThat(true).isTrue();
    }
}
