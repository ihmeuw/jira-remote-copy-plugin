package com.atlassian.cpji.tests.pageobjects.admin;

import com.atlassian.cpji.tests.ScreenshotUtil;
import com.atlassian.cpji.tests.pageobjects.PageElements;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @since v3.0
 */
public class ListApplicationLinksPage extends AbstractJiraPage {
	private static final Logger logger = LoggerFactory.getLogger(ListApplicationLinksPage.class);

    @ElementBy (className = "links-loading")
    private PageElement linksLoading;

	@Override
	public TimedCondition isAt() {
		return linksLoading.timed().isVisible();
	}

	@Override
	public String getUrl() {
		return "/plugins/servlet/applinks/listApplicationLinks";
	}

	public SetApplicationUrlDialog clickAddApplicationLink() {
		elementFinder.find(By.id("add-application-link")).click();
		return pageBinder.bind(SetApplicationUrlDialog.class);
	}

	public DeleteDialog clickDelete(String url) {
		Preconditions.checkNotNull(url);
		ScreenshotUtil.attemptScreenshot(driver.getDriver(), "before click delete");
		final By by = By.cssSelector(String.format("tr[id='ual-row-%s'] .app-delete-link", url));
		driver.waitUntilElementIsVisible(by);
		elementFinder.find(by).click();
		return pageBinder.bind(DeleteDialog.class);
	}

    protected PageElement getApplicationLinksTable() {
        driver.waitUntilElementIsNotVisible(By.className("links-loading"));
        return elementFinder.find(By.id("application-links-table"), TimeoutType.SLOW_PAGE_LOAD);
    }

	public List<ApplicationLinkBean> getApplicationLinks() {
		return ImmutableList.copyOf(Iterables.transform(getApplicationLinksTable().findAll(By.tagName("tr")), new Function<PageElement, ApplicationLinkBean>() {
			@Override
			public ApplicationLinkBean apply(@Nullable PageElement input) {
				return new ApplicationLinkBean(input.find(By.className("application-name")).getText(), input.find(By.className("application-url")).getText());
			}
		}));
	}

	public List<String> getNamesOfApplicationLinks() {
		return ImmutableList.copyOf(Iterables.transform(getApplicationLinks(), new Function<ApplicationLinkBean, String>() {
			@Override
			public String apply(ApplicationLinkBean input) {
				return input.getName();
			}
		}));
	}

	public static class DeleteDialog extends AbstractJiraPage {
		@ElementBy (id = "delete-application-link-dialog")
		private PageElement dialog;

		public DeleteDialog delete() {
			final By locator = By.cssSelector(".button-panel-button.wizard-submit");
			final PageElement deleteButton = Iterables.get(Iterables
					.filter(dialog.findAll(locator),
							PageElements.isVisible()), 0);
			driver.waitUntil(PageElements.isEnabled(locator));
			deleteButton.click();
			return this;
		}

		public ListApplicationLinksPage deleteAndReturn() {
		    this.delete();
			return pageBinder.bind(ListApplicationLinksPage.class);
		}

		@Override
		public TimedCondition isAt() {
			return dialog.timed().isVisible();
		}

		@Override
		public String getUrl() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

	public static abstract class AddDialog extends AbstractJiraPage {
		@ElementBy (id = "add-application-link-dialog")
		protected PageElement dialog;

		@Override
		public String getUrl() {
			throw new UnsupportedOperationException("Not implemented");
		}

		public <T> T next(Class<T> clz) {
			Iterables.get(Iterables.filter(dialog.findAll(By.cssSelector(".button-panel-button.applinks-next-button")), PageElements.isVisible()), 0).click();
			return pageBinder.bind(clz);
		}

	}

	public static class SetApplicationUrlDialog extends AddDialog {
		@ElementBy (id = "application-url", within = "dialog")
		private PageElement applicationUrl;

		public SetApplicationUrlDialog setApplicationUrl(String url) {
			Preconditions.checkNotNull(url);
			applicationUrl.clear();
			applicationUrl.type(url);
			return this;
		}

		@Override
		public TimedCondition isAt() {
			return applicationUrl.timed().isVisible();
		}

		public CreateReciprocalLinkDialog next() {
			return super.next(CreateReciprocalLinkDialog.class);
		}
	}

	public static class CreateReciprocalLinkDialog extends AddDialog {
		@ElementBy (id = "reciprocal-link-back-to-server", within = "dialog")
		private CheckboxElement reciprocalLinkCheckbox;

		@ElementBy (id = "reciprocal-link-username", within = "dialog")
		private PageElement username;

		@ElementBy (id = "reciprocal-link-password", within = "dialog")
		private PageElement password;

		public CreateReciprocalLinkDialog setCreateReciprocalLink(boolean check) {
			if (check) {
				reciprocalLinkCheckbox.check();
			} else {
				reciprocalLinkCheckbox.uncheck();
			}
			return this;
		}

		public CreateReciprocalLinkDialog setUsername(String username) {
			Preconditions.checkNotNull(username);
			this.username.clear();
			this.username.type(username);
			return this;
		}

		public CreateReciprocalLinkDialog setPassword(String password) {
			Preconditions.checkNotNull(password);
			this.password.clear();
			this.password.type(password);
			return this;
		}

		public UsersAndTrustDialog next() {
			return super.next(UsersAndTrustDialog.class);
		}

		@Override
		public TimedCondition isAt() {
			return username.timed().isVisible();
		}
	}

	public static class UsersAndTrustDialog extends AddDialog {
		@ElementBy (id = "differentUser", within = "dialog")
		private PageElement differenUserRadio;

		public UsersAndTrustDialog setUseDifferentUsers() {
			differenUserRadio.click();
			return this;
		}

		public ListApplicationLinksPage next() {
			Iterables.get(Iterables.filter(dialog.findAll(By.cssSelector(".button-panel-button.wizard-submit")), PageElements.isVisible()), 0).click();
			return pageBinder.bind(ListApplicationLinksPage.class);
		}

		@Override
		public TimedCondition isAt() {
			return differenUserRadio.timed().isVisible();
		}
	}

	private class ApplicationLinkBean {
		private final String name;
		private final String url;

		private ApplicationLinkBean(String name, String url) {
			this.name = name;
			this.url = url;
		}

		public String getName() {
			return name;
		}

		public String getUrl() {
			return url;
		}
	}
}