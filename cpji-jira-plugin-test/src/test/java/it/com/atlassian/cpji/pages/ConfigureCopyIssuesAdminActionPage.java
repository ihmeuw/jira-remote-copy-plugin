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

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

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
        waitUntilFalse(loadingMarker.timed().isVisible());
		return ImmutableList.copyOf(Iterables.transform(driver.findElements(By.cssSelector("#cpji-required-fields div.field-group label")),
				WebDriverQueryFunctions.getText()));
	}

    public static class AsDialog extends ConfigureCopyIssuesAdminActionPage{
        private static final String URI_SUMMARY_TEMPLATE = "/plugins/servlet/project-config/%s/summary";

        @ElementBy(id="configure_cpji")
        private PageElement configureLink;

        public AsDialog(@Nonnull String projectKey){
            super(projectKey);
        }

        @Override
        public TimedCondition isAt() {
            waitUntilTrue(configureLink.timed().isVisible());
            configureLink.click();
            return super.isAt();
        }

        @Override
        public String getUrl() {
            return String.format(URI_SUMMARY_TEMPLATE, projectKey);
        }
    }


}
