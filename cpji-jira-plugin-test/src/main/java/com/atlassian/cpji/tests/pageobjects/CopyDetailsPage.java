package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.cpji.action.RemoteIssueLinkType;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Step 2 for doing a Remote Copy.
 *
 * @since v2.1
 */
public class CopyDetailsPage extends AbstractJiraPage {
    private static final String URI_TEMPLATE = "/secure/CopyDetailsAction.jspa?id=%d&targetEntityLink=%s";

    private final Long issueId;
    private final String url;

    @ElementBy(className = "submit")
    PageElement nextButton;

    @ElementBy(id = "create-issue-links-group")
    PageElement createIssueLinkGroup;

    @ElementBy(id = "copy-comments")
    CheckboxElement copyComments;

    @ElementBy(id = "copy-comments-group")
    PageElement copyCommentsGroup;

    @ElementBy(tagName = "div", within = "copyCommentsGroup")
    PageElement copyCommentsNotice;

    @ElementBy(id = "copy-issue-links")
    CheckboxElement copyIssueLinks;

    @ElementBy(id = "copy-issue-links-group")
    PageElement copyIssueLinksGroup;

    @ElementBy(tagName = "div", within = "copyIssueLinksGroup")
    PageElement copyIssueLinksNotice;

    @ElementBy(id = "copy-attachments")
    CheckboxElement copyAttachments;

    @ElementBy(id = "copy-attachments-group")
    PageElement copyAttachmentsGroup;

    @ElementBy(tagName = "div", within = "copyAttachmentsGroup")
    PageElement copyAttachmentsNotice;

    @ElementBy(id = "summary")
    PageElement summary;

    SingleSelect remoteIssueLink;

    public CopyDetailsPage(final Long issueId, final String targetEntityLink) {
        this.issueId = issueId;
        this.url = String.format(URI_TEMPLATE, issueId, targetEntityLink);
    }

    @Init
    public void init() {
        final PageElement remoteIssueLinkContainer = elementFinder.find(By.id("remoteIssueLink-single-select"));
        remoteIssueLink = pageBinder.bind(SingleSelect.class, remoteIssueLinkContainer);
    }

    @Override
    public TimedCondition isAt() {
        return Conditions.forMatcher(elementFinder.find(By.className("current")).timed().getText(), Matchers.equalToIgnoringCase("Details"));
    }

    @Override
    public String getUrl() {
        return url;
    }

    public CopyIssueToInstanceConfirmationPage next() {
        nextButton.click();
        return pageBinder.bind(CopyIssueToInstanceConfirmationPage.class);
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


    public List<String> getCreateIssueLinks() {
        remoteIssueLink.clear().triggerSuggestions();
        return remoteIssueLink.getSuggestions();
    }

    public CopyDetailsPage setCreateIssueLink(RemoteIssueLinkType value) {
        final String selectedValue;
        switch (value) {
            case RECIPROCAL:
                selectedValue = "Both";
                break;
            case INCOMING:
                selectedValue = "From copy";
                break;
            case OUTGOING:
                selectedValue = "From original";
                break;
            case NONE:
                selectedValue = "None";
                break;
            default:
                throw new IllegalArgumentException();
        }

        remoteIssueLink.type(selectedValue).clickSuggestion();
        return this;
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

    public void showNotCopiedAttachmentsList() {
        PageElement lozenge = copyAttachmentsNotice.find(By.className("aui-lozenge"));
        lozenge.click();
    }


    public TimedQuery<String> getNotCopiedAttachmentsList() {
        return elementFinder.find(By.cssSelector("#inline-dialog-attachments-popup ul")).timed().getText();
    }

    public CopyDetailsPage enterNewSummary(String summary) {
        this.summary.clear().type(summary);
        return this;
    }
}
