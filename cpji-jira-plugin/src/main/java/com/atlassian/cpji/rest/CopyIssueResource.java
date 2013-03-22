package com.atlassian.cpji.rest;

import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.exceptions.CopyIssueException;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.ErrorBean;
import org.apache.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * @since v1.0
 */
@Path ("copyissue")
@Consumes ({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Produces ({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public class CopyIssueResource
{

    private final CopyIssueService copyIssueService;

    private static final Logger log = Logger.getLogger(CopyIssueResource.class);

    public CopyIssueResource(final CopyIssueService copyIssueService)
    {
        this.copyIssueService = copyIssueService;
    }


    @PUT
    @Path ("copy")
    public Response copyIssue(final CopyIssueBean copyIssueBean)
    {
        try{
            return Response.ok(copyIssueService.copyIssue(copyIssueBean)).cacheControl(RESTException.never()).build();
        }
        catch (CopyIssueException e)
        {
			log.error(String.format("Failed to copy issue: %s", e.getMessage()));
            return Response.serverError().entity(ErrorBean.convertErrorCollection(e.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
    }


    @PUT
    @Path ("fieldPermissions")
    public Response checkFieldPermissions(final CopyIssueBean copyIssueBean)
    {
        try
        {
            return Response.ok(copyIssueService.checkFieldPermissions(copyIssueBean)).cacheControl(RESTException.never()).build();
        }
        catch (CopyIssueException e)
        {
            return Response.serverError().entity(ErrorBean.convertErrorCollection(e.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
        catch (Exception ex)
        {
            log.error(String.format("Failed to check field permissions for source issue '" + copyIssueBean.getOriginalKey() + "': %s", ex.getMessage()));
            return Response.serverError().entity(new ErrorBean(
					"Failed to check field permissions for source issue '" + copyIssueBean.getOriginalKey() + "'. Please contact your administrator."))
					.cacheControl(RESTException.never()).build();
        }
    }

    /**
     * Converts any remote issue links to this JIRA instance into local issue links.
     *
     * @param issueKey the issue key
     * @return no content if successful
     */
    @GET
    @Path ("convertIssueLinks/{issueKey}")
    public Response convertIssueLinks(@PathParam ("issueKey") String issueKey)
    {
        try{
            copyIssueService.convertRemoteLinksToLocal(issueKey);
            return Response.noContent().cacheControl(RESTException.never()).build();
        } catch (CopyIssueException e){
            return Response.serverError().entity(ErrorBean.convertErrorCollection(e.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
    }

    @GET
    @Path ("clearIssueHistory/{issueKey}")
    public Response clearIssueHistory(@PathParam("issueKey") String issueKey){
        try{
            copyIssueService.clearChangeHistory(issueKey);
            return Response.noContent().cacheControl(RESTException.never()).build();
        } catch (CopyIssueException e){
            return Response.serverError().entity(ErrorBean.convertErrorCollection(e.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
    }
}