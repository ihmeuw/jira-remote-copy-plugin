package com.atlassian.cpji.rest;

import com.atlassian.cpji.components.CopyIssueService;
import com.atlassian.cpji.components.exceptions.CopyIssueException;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.ErrorBean;
import org.apache.log4j.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;


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

    @Inject
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
			log.error("Failed to copy issue", e);
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
            log.error(String.format("Failed to check field permissions for source issue '" + copyIssueBean.getOriginalKey()), e);
            return Response.serverError().entity(ErrorBean.convertErrorCollection(e.getErrorCollection())).cacheControl(RESTException.never()).build();
        }
        catch (Exception ex)
        {
            log.error(String.format("Failed to check field permissions for source issue '" + copyIssueBean.getOriginalKey()), ex);
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