package com.atlassian.cpji.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.inject.Inject;


/**
 * @since v1.0
 */
public class RESTException extends WebApplicationException
{
    @Inject
    public RESTException(final Response.Status status, final String... errors)
    {
        super(createResponse(status, errors));
    }

    private static Response createResponse(Response.Status status, final String... errors)
    {
        // the issue key is not used yet, but should make it into the entity in the future...
        return Response.status(status).entity(errors).cacheControl(never()).build();
    }

    public static javax.ws.rs.core.CacheControl never()
    {
        javax.ws.rs.core.CacheControl cacheNever = new javax.ws.rs.core.CacheControl();
        cacheNever.setNoStore(true);
        cacheNever.setNoCache(true);

        return cacheNever;
    }
}
