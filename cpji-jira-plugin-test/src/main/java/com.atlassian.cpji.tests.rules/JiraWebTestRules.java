package com.atlassian.cpji.tests.rules;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.config.junit4.rule.DirtyWarningTerminatorRule;
import com.atlassian.jira.pageobjects.config.junit4.rule.RestoreDataMethodRule;
import com.atlassian.jira.pageobjects.config.junit4.rule.RuleChainBuilder;
import com.atlassian.jira.webtest.webdriver.setup.JiraWebDriverScreenshotRule;
import com.atlassian.jira.webtest.webdriver.setup.JiraWebTestLogger;
import com.atlassian.webdriver.testing.rule.LogPageSourceRule;
import com.atlassian.webdriver.testing.rule.SessionCleanupRule;
import com.atlassian.webdriver.testing.rule.WindowSizeRule;
import org.junit.rules.RuleChain;

public final class JiraWebTestRules
{
    private JiraWebTestRules()
    {
        throw new AssertionError("Don't instantiate me");
    }

    public static RuleChain forJira(JiraTestedProduct jira)
    {
        return RuleChainBuilder.forProduct(jira)
                // before-test
                .around(WindowSizeRule.class)
                .around(SessionCleanupRule.class)
                .around(RestoreDataMethodRule.class)
                .around(new WebSudoRule(jira))
                // after-test
                .around(JiraWebDriverScreenshotRule.class)
                .around(DirtyWarningTerminatorRule.class)
                .around(new LogPageSourceRule(jira.getTester().getDriver(), JiraWebTestLogger.LOGGER))
                .build();
    }
}
