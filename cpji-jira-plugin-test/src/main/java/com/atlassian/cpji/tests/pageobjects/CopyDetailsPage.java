package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

/**
 * Step 2 for doing a Remote Copy.
 *
 * @since v2.1
 */
public class CopyDetailsPage extends AbstractJiraPage
{
    private static final String URI_TEMPLATE = "/secure/CopyDetailsAction.jspa?id=%d&targetEntityLink=%s";

    private final Long issueId;
    private final String url;

    @ElementBy(className = "submit")
    PageElement nextButton;

	@ElementBy(id = "create-issue-links-group")
	PageElement createIssueLinkGroup;

	@ElementBy (id = "copy-comments")
	CheckboxElement copyComments;

	@ElementBy (id = "copy-comments-group")
	PageElement copyCommentsGroup;

	@ElementBy (className = "notice", within = "copyCommentsGroup")
	PageElement copyCommentsNotice;

	@ElementBy (id = "remoteIssueLink")
	SelectElement remoteIssueLink;

	@ElementBy (id = "copy-issue-links")
	CheckboxElement copyIssueLinks;

	@ElementBy (id = "copy-issue-links-group")
	PageElement copyIssueLinksGroup;

	@ElementBy (className = "notice", within = "copyIssueLinksGroup")
	PageElement copyIssueLinksNotice;

	@ElementBy (id = "copy-attachments")
	CheckboxElement copyAttachments;

	@ElementBy (id = "copy-attachments-group")
	PageElement copyAttachmentsGroup;

	@ElementBy (className = "notice", within = "copyAttachmentsGroup")
	PageElement copyAttachmentsNotice;

    public CopyDetailsPage(final Long issueId, final String targetEntityLink)
    {
        this.issueId = issueId;
        this.url = String.format(URI_TEMPLATE, issueId, targetEntityLink);
    }

    @Override
    public TimedCondition isAt()
    {
        return Conditions.forMatcher(elementFinder.find(By.className("current")).timed().getText(), Matchers.equalToIgnoringCase("Select Issue Type"));
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    public PermissionChecksPage next()
    {
        nextButton.click();
        return pageBinder.bind(PermissionChecksPage.class);
    }

	public boolean isCreateIssueLinksGroupVisible() {
		return createIssueLinkGroup.timed().isVisible().now();
	}

	public PageElement getCopyCommentsGroup() {
		return copyCommentsGroup;
	}

	public PageElement getCopyIssueLinksGroup() {
		return copyIssueLinksGroup;
	}

	public PageElement getCopyAttachmentsGroup() {
		return copyAttachmentsGroup;
	}

	public SelectElement getCreateIssueLinks() {
		return remoteIssueLink;
	}

	public CheckboxElement getCopyIssueLinks() {
		return copyIssueLinks;
	}

	public CheckboxElement getCopyAttachments() {
		return copyAttachments;
	}

	public CheckboxElement getCopyComments() {
		return copyComments;
	}

	public PageElement getCopyCommentsNotice() {
		return copyCommentsNotice;
	}

	public PageElement getCopyIssueLinksNotice() {
		return copyIssueLinksNotice;
	}

	public PageElement getCopyAttachmentsNotice() {
		return copyAttachmentsNotice;
	}
}
