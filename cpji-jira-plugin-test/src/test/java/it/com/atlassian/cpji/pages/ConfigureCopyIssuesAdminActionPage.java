package it.com.atlassian.cpji.pages;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.webdriver.WebDriverQueryFunctions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @since v2.1
 */
public class ConfigureCopyIssuesAdminActionPage extends AbstractJiraPage {
	private static final String URI_TEMPLATE = "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=%s";

	private final String projectKey;

	@ElementBy(id = "update-field-defaults")
	private PageElement updateFieldDefaultsButton;

	public ConfigureCopyIssuesAdminActionPage(@Nonnull String projectKey) {
		this.projectKey = projectKey;
	}

	@Override
	public TimedCondition isAt() {
		return updateFieldDefaultsButton.timed().isVisible();
	}

	@Override
	public String getUrl() {
		return String.format(URI_TEMPLATE, projectKey);
	}

	@Nonnull
	public List<String> getRequiredFields() {
		return ImmutableList.copyOf(Iterables.transform(driver.findElements(By.cssSelector("#required-fields div.field-group label")),
				WebDriverQueryFunctions.getText()));
	}
}
