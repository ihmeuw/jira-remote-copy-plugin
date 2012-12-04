package com.atlassian.cpji.rest;

/**
 * @deprecated used only in unused currently {@link com.atlassian.cpji.IssueAttachmentsClient}
 * @since v1.5
 */
@Deprecated
public class ResponseHolder
{
    private boolean successful;
    private String responseBody;

    public ResponseHolder(final boolean successful, final String responseBody)
    {
        this.successful = successful;
        this.responseBody = responseBody;
    }

    public ResponseHolder(final boolean successful)
    {
        this.successful = successful;
    }

    public boolean isSuccessful()
    {
        return successful;
    }

    public String getResponse()
    {
        return responseBody;
    }
}
