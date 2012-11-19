package it.com.atlassian.cpji.pages;

import com.atlassian.cpji.tests.pageobjects.MultiSelectUtil;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.jira.pageobjects.project.summary.ProjectSummaryPageTab;
import com.atlassian.jira.pageobjects.project.summary.SettingsPanel;
import com.atlassian.jira.pageobjects.util.TraceContext;
import com.atlassian.jira.pageobjects.util.Tracer;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.webdriver.WebDriverQueryFunctions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.openqa.selenium.By;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

/**
 * @since v2.1
 */
public class ConfigureCopyIssuesAdminActionPage extends AbstractJiraPage {
	private static final String URI_TEMPLATE = "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=%s";

    protected final String projectKey;

    @ElementBy(id = "cpji-update-button")
	protected PageElement updateButton;

    @Inject
    private TraceContext traceContext;


	public ConfigureCopyIssuesAdminActionPage(@Nonnull String projectKey) {
		this.projectKey = projectKey;
    }

	public ConfigureCopyIssuesAdminActionPage setAllowedGroups(@Nullable Iterable<String> groups) {
		MultiSelectUtil.setMultiSelect(this.pageBinder, "groups", groups);
		return this;
	}

	@Nonnull
	public Iterable<String> getAllowedGroups() {
		return MultiSelectUtil.getMultiSelect(this.pageBinder, "groups");
	}

	public ConfigureCopyIssuesAdminActionPage submit() {
		final Context cx = Context.enter();
		try {
			final Scriptable scope = cx.initStandardObjects();
			scope.put("traceContext", scope, new TraceContext());
			Tracer checkpoint = (Tracer) cx.evaluateString(scope, "traceContext.checkpoint();", "js", 1, null);
			updateButton.click();
			traceContext.waitFor(checkpoint, "cpji.load.completed");
		} finally {
			cx.exit();
		}
		return pageBinder.bind(ConfigureCopyIssuesAdminActionPage.class, projectKey);
	}

    public DefaultValuesFragment getDefaultValues() {
        return pageBinder.bind(DefaultValuesFragment.class);
    }

    public boolean hasGeneralErrorsMessage() {
        return elementFinder.find(By.id("cpji-general-errors")).timed().isVisible().byDefaultTimeout();
    }

	@Override
	public TimedCondition isAt() {
        return updateButton.timed().isVisible();
	}

	@Override
	public String getUrl() {
        return String.format(URI_TEMPLATE, projectKey);
	}

	@Nonnull
	public List<String> getRequiredFields() {
		return ImmutableList.copyOf(
				Iterables.transform(driver.findElements(By.cssSelector("#cpji-required-fields div.field-group label")),
						WebDriverQueryFunctions.getText()));
	}


    public List<String> getErrors() {
        return ImmutableList.copyOf(Iterables.transform(driver.findElements(By.className("error")), WebDriverQueryFunctions.getText()));
    }

    public List<String> getConfigChanges() {
		assert elementFinder.find(By.cssSelector("#cpji-general-success #configChanges li")).timed().isVisible().byDefaultTimeout();
        final List<PageElement> messages = elementFinder.findAll(By.cssSelector("#cpji-general-success #configChanges li"));
        return ImmutableList.copyOf(Iterables.transform(messages, PageElements.getText()));
    }

    public static class AsDialog extends ConfigureCopyIssuesAdminActionPage {

        public AsDialog(@Nonnull String projectKey) {
            super(projectKey);
        }

        public static AsDialog open(JiraTestedProduct jira, String projectKey) {
            ProjectSummaryPageTab summary = jira.visit(ProjectSummaryPageTab.class, projectKey);
            final List<PageElement> plugins = summary.openPanel(SettingsPanel.class).getPluginElements();
            PageElement configureLink = Iterables.find(plugins, new Predicate<PageElement>() {
                @Override
                public boolean apply(@Nullable final PageElement input) {
                    return input.getText().startsWith("Remote Issue Copy");
                }
            });
            configureLink.find(By.id("configure_cpji")).click();
            return jira.getPageBinder().bind(AsDialog.class, projectKey);
        }
	}


}
