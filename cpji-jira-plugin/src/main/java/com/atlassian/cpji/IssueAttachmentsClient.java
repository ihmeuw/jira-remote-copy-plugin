package com.atlassian.cpji;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cpji.rest.ResponseHolder;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFilePart;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since v1.0
 */
public class IssueAttachmentsClient
{
    private final ApplicationLink applicationLink;
    private static final Logger log = Logger.getLogger(IssueAttachmentsClient.class);

    public IssueAttachmentsClient(ApplicationLink applicationLink)
    {
        this.applicationLink = applicationLink;
    }

    public ResponseHolder addAttachment(final String issueKey, Collection<Attachment> attachments)
    {
        if (StringUtils.isEmpty(issueKey))
        {
            throw new IllegalArgumentException("Issue Key cannot be empty");
        }
        ApplicationLinkRequestFactory authenticatedRequestFactory = applicationLink.createAuthenticatedRequestFactory();
        ApplicationLinkRequest request;
        try
        {
            request = authenticatedRequestFactory.createRequest(Request.MethodType.POST, "rest/api/latest/issue/" + issueKey + "/attachments");
        }
        catch (CredentialsRequiredException e)
        {
            throw new RuntimeException("Authentication failed '" + e.getMessage() + "'", e);
        }
        request.addHeader("X-Atlassian-Token", "nocheck");

        List<RequestFilePart> attachmentsToCopy = new ArrayList<RequestFilePart>();
        for (Attachment attachment : attachments)
        {
            File attachmentFile = AttachmentUtils.getAttachmentFile(attachment);
            RequestFilePart requestFilePart = new RequestFilePart(attachment.getMimetype(), attachment.getFilename(), attachmentFile, "file");
            attachmentsToCopy.add(requestFilePart);
        }
        request.setFiles(attachmentsToCopy);

        try
        {
            ResponseHolder success = request.execute(new ApplicationLinkResponseHandler<ResponseHolder>()
            {
                public ResponseHolder credentialsRequired(final Response response) throws ResponseException
                {
                    if (!response.isSuccessful())
                    {
                        log.error("Failed to copy attachments for remote issue key '" + issueKey + "' Response is: " + response.getResponseBodyAsString());
                        return new ResponseHolder(response.isSuccessful(), response.getResponseBodyAsString());
                    }
                    return new ResponseHolder(true);
                }

                public ResponseHolder handle(final Response response) throws ResponseException
                {
                    if (!response.isSuccessful())
                    {
                        log.error("Failed to copy attachments for remote issue key '" + issueKey + "' Response is: " + response.getResponseBodyAsString());
                        return new ResponseHolder(response.isSuccessful(), response.getResponseBodyAsString());
                    }
                   return new ResponseHolder(true);
                }
            });
            return success;
        }
        catch (ResponseException e)
        {
            log.error("Failed to attach attachments to remote issue with key '" + issueKey + "'", e);
            return new ResponseHolder(false, e.getMessage());
        }
    }


}
