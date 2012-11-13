package com.atlassian.cpji.tests.rules;

import com.atlassian.jira.functest.framework.util.junit.AnnotatedDescription;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.config.EnableWebSudo;
import com.atlassian.jira.testkit.client.Backdoor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class WebSudoRule implements TestRule
{
    private final Backdoor backdoor;

    public WebSudoRule(JiraTestedProduct jira)
    {
        this.backdoor = EnableDarkFeatureRule.getBackdoor(jira);
    }


    @Override
    public Statement apply(final Statement base, final Description description)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                if (shouldEnable(description))
                {
                    backdoor.websudo().enable();
                }
                else
                {
                    backdoor.websudo().disable();
                }
                base.evaluate();
            }

        };
    }


    private boolean shouldEnable(Description description)
    {
        return new AnnotatedDescription(description).hasAnnotation(EnableWebSudo.class);
    }
}
