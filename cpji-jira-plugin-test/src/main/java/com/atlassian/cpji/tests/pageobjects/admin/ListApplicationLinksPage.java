package com.atlassian.cpji.tests.pageobjects.admin;

import com.atlassian.cpji.tests.pageobjects.PageElements;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.AbstractTimedCondition;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

/**
 * @since v3.0
 */
public class ListApplicationLinksPage extends AbstractJiraPage {
    @ElementBy (className = "links-loading")
    private PageElement linksLoading;

    @ElementBy(id="application-links-table")
    private PageElement applicationLinksTable;

    //this button was in JIRA 6.0 and earlier
    @ElementBy(id = "add-application-link")
    private PageElement addApplicationLink;

    //this field is in JIRA 6.1 and later
    @ElementBy(id = "applinks-url-entered")
    private PageElement applicationUrl;

    //this field is in JIRA 6.1 and later
    @ElementBy(cssSelector = "#createApplicationLink .aui-button")
    private PageElement createNewLink;

    @Inject
    private Timeouts timeouts;

    @Override
	public TimedCondition isAt() {
        return new AbstractTimedCondition(timeouts.timeoutFor(TimeoutType.SLOW_PAGE_LOAD), timeouts.timeoutFor(TimeoutType.EVALUATION_INTERVAL)) {
            @Override
            protected Boolean currentValue() {
                final boolean present = applicationLinksTable.isPresent();
                final boolean visible = linksLoading.isVisible();
                return present && !visible;
            }
        };
	}

	@Override
	public String getUrl() {
		return "/plugins/servlet/applinks/listApplicationLinks";
	}

	@Init
	public void removeDirtyWarning() {
		// get out of any IFrame
		driver.switchTo().defaultContent();
		// just make it WOOOORK
		elementFinder.find(By.tagName("body")).javascript().execute("window.onbeforeunload=null;");
	}

    public ConfirmApplicationUrlDialog setApplicationUrl(String url) {
        Preconditions.checkNotNull(url);
        applicationUrl.clear();
        applicationUrl.type(url);
        Poller.waitUntilTrue(createNewLink.timed().isEnabled());
        createNewLink.click();
        return pageBinder.bind(ConfirmApplicationUrlDialog.class);
    }

	public SetApplicationUrlDialog clickAddApplicationLink() {
        addApplicationLink.click();
		return pageBinder.bind(SetApplicationUrlDialog.class);
	}

    public boolean isAddApplicationLinkPresent() {
        return addApplicationLink.isPresent();
    }

	public DeleteDialog clickDelete(String name) {
		Preconditions.checkNotNull(name);
		final By by = By.xpath(String.format("//td[preceding-sibling::td[. = '%s']]//a[@class='app-delete-link']", name));
		driver.waitUntilElementIsVisible(by);
		elementFinder.find(by).click();
		return pageBinder.bind(DeleteDialog.class);
	}

    protected PageElement getApplicationLinksTable() {
        Poller.waitUntilFalse(linksLoading.timed().isVisible());
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
		@ElementBy (id = "delete-application-link-dialog", timeoutType = TimeoutType.PAGE_LOAD)
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
            Poller.waitUntilFalse(dialog.timed().isVisible());
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
		@ElementBy (id = "differentUser", within = "dialog", timeoutType = TimeoutType.PAGE_LOAD)
		private PageElement differenUserRadio;

		public UsersAndTrustDialog setUseDifferentUsers() {
			differenUserRadio.click();
			return this;
		}

		public ListApplicationLinksPage next() {
			Iterables.get(Iterables.filter(dialog.findAll(By.cssSelector(".button-panel-button.wizard-submit")), PageElements.isVisible()), 0).click();
            Poller.waitUntilFalse(differenUserRadio.timed().isVisible());
			return pageBinder.bind(ListApplicationLinksPage.class);
		}

		@Override
		public TimedCondition isAt() {
			return differenUserRadio.timed().isVisible();
		}
	}

    public static class ConfirmApplicationUrlDialog extends AbstractJiraPage {
        @ElementBy (id = "create-applink-dialog")
        protected PageElement dialog;

        @ElementBy(cssSelector = "button.aui-button-primary")
        protected PageElement continueButton;

        @Override
        public String getUrl() {
            throw new UnsupportedOperationException("Not implemented");
        }

        public JiraLoginPage clickContinue() {
            continueButton.click();
            return pageBinder.bind(JiraLoginPage.class);
        }

        @Override
        public TimedCondition isAt() {
            return dialog.timed().isVisible();
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
