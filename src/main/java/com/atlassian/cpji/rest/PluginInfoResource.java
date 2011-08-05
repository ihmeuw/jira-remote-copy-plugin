package com.atlassian.cpji.rest;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @since v1.1
 */
@Path ("plugininfo")
@Consumes ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Produces ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public class PluginInfoResource
{
    public static String RESOURCE_PATH = "plugininfo";

    public PluginInfoResource()
    {
    }

    @GET
    @AnonymousAllowed
    public Response pluginInfo()
    {
        return Response.ok("installed").build();
    }
}
