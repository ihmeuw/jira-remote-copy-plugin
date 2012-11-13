package it.com.atlassian.cpji.pages;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.jira.pageobjects.project.summary.ProjectSummaryPageTab;
import com.atlassian.jira.pageobjects.project.summary.SettingsPanel;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.webdriver.WebDriverQueryFunctions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.openqa.selenium.By;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @since v2.1
 */
public class ConfigureCopyIssuesAdminActionPage extends AbstractJiraPage {
	private static final String URI_TEMPLATE = "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=%s";

    protected final String projectKey;

    @ElementBy(id = "cpji-update-button")
	protected PageElement updateButton;

    @ElementBy(className="cpji-loading")
    protected PageElement loadingMarker;

	public ConfigureCopyIssuesAdminActionPage(@Nonnull String projectKey) {
		this.projectKey = projectKey;
    }

	public ConfigureCopyIssuesAdminActionPage setAllowedGroups(@Nullable Iterable<String> groups) {
		final Context cx = Context.enter();
		try {
			final Scriptable scope = cx.initStandardObjects();
			final List<String> items = Lists.newArrayList();
			scope.put("items", scope, items);
			scope.put("select", scope, pageBinder.bind(getMultiSelectClass(), "groups"));

			cx.evaluateString(scope, "select.clear();", "js", 1, null);
			if (groups != null) {
				for (String group : groups) {
					cx.evaluateString(scope, "select.add('" + group + "');", "js", 1, null);
				}
			}
			return this;
		} finally {
			cx.exit();
		}
	}

	@Nonnull
	public Iterable<String> getAllowedGroups() {
		final Context cx = Context.enter();
		try {
//		return Iterables.transform(this.groups.getItems().byDefaultTimeout(), new Function<MultiSelect.Lozenge, String>() {
//			@Override
//			public String apply(@Nullable MultiSelect.Lozenge input) {
			final Scriptable scope = cx.initStandardObjects();
			final List<String> items = Lists.newArrayList();
			scope.put("items", scope, items);
			scope.put("select", scope, pageBinder.bind(getMultiSelectClass(), "groups"));

			cx.evaluateString(scope, "for(var s = select.getItems().size(), i = 0; i<s; ++i) { items.add(select.getItems().get(i).getName()); }", "js", 1, null);

			return items;
		} finally {
			cx.exit();
		}
	}

	private Class getMultiSelectClass() {
		final Class select;
		try {
			select = getClass().getClassLoader().loadClass("com.atlassian.jira.pageobjects.components.MultiSelect");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return select;
	}

	public ConfigureCopyIssuesAdminActionPage submit() {
		updateButton.click();
		return pageBinder.bind(ConfigureCopyIssuesAdminActionPage.class, projectKey);
	}

	@Override
	public TimedCondition isAt() {
        return Conditions.and(updateButton.timed().isVisible(), Conditions.not(loadingMarker.timed().isVisible()));
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

    public static class AsDialog extends ConfigureCopyIssuesAdminActionPage{

        public AsDialog(@Nonnull String projectKey){
            super(projectKey);
        }

        public static AsDialog open(JiraTestedProduct jira, String projectKey){
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
