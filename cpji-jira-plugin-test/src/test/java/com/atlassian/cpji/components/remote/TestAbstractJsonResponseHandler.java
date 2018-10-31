package com.atlassian.cpji.components.remote;

import com.atlassian.cpji.components.model.JiraLocation;
import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.rest.model.ErrorBean;
import io.atlassian.fugue.Either;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestAbstractJsonResponseHandler {

    private static class MyResponseType { }

    private static class JsonResponseHandlerImpl extends AbstractJsonResponseHandler<MyResponseType>{

        private final MyResponseType mockedResponse;
        private final NegativeResponseStatus providedResponseStatus;

        JsonResponseHandlerImpl(JiraLocation jiraLocation, MyResponseType mockedResponse, NegativeResponseStatus provideResponseStatus) {
            super(jiraLocation);
            this.mockedResponse = mockedResponse;
            this.providedResponseStatus = provideResponseStatus;
        }

        @Override
        protected MyResponseType parseResponse(Response response) throws ResponseException, JSONException {
            if(providedResponseStatus != null){
                provideResponseStatus(providedResponseStatus);
            }
            return mockedResponse;
        }
    }

    private AbstractJsonResponseHandler<MyResponseType> jsonResponseHandler;

    private final MyResponseType defaultResponse = new MyResponseType();
    private final JiraLocation defaultJiraLocation = new JiraLocation("idIDid", "jiraName");

    @Before
    public void setUp(){
        jsonResponseHandler = new JsonResponseHandlerImpl(defaultJiraLocation, defaultResponse, null);
    }

    @Test
    public void testAuthenticationFailedOn401Response() throws Exception {
        testStandardErrorResponse(401, NegativeResponseStatus.Status.AUTHENTICATION_FAILED);
    }
    @Test
    public void testPluginNotInstalledOn404Response() throws Exception {
        testStandardErrorResponse(404, NegativeResponseStatus.Status.PLUGIN_NOT_INSTALLED);
    }

    @Test
    public void shouldTryParseErrorBeanWhenResponseIsUnsuccesfull() throws Exception {
        Response response = mockUnsuccesfulResponse(500);
        ErrorBean bean = new ErrorBean();
        when(response.getEntity(ErrorBean.class)).thenReturn(bean);

        Either<NegativeResponseStatus, MyResponseType> handledResponse = jsonResponseHandler.handle(response);

        testNegativeResponse(NegativeResponseStatus.Status.ERROR_OCCURRED, handledResponse);
        verify(response).getEntity(ErrorBean.class);
    }

    @Test
    public void shouldReturnCommunicationFailedWhenCannotParseUnsuccesfulResponseToErrorBean() throws Exception{
        Response response = mockUnsuccesfulResponse(500);
        when(response.getEntity(ErrorBean.class)).thenThrow(new ResponseException());

        Either<NegativeResponseStatus, MyResponseType> handledResponse = jsonResponseHandler.handle(response);

        testNegativeResponse(NegativeResponseStatus.Status.COMMUNICATION_FAILED, handledResponse);
    }

    @Test
    public void testReturnParsedOnSuccesfulResponse() throws Exception{
        Response response = mockSuccesfulResponse(200);

        Either<NegativeResponseStatus, MyResponseType> parsedResponse = jsonResponseHandler.handle(response);
        assertTrue(parsedResponse.isRight());
        assertEquals(defaultResponse, parsedResponse.right().get());
    }

    @Test
    public void testReturnResponseStatusWhenProvided() throws Exception{
        Response response = mockSuccesfulResponse(200);
        JsonResponseHandlerImpl myHandler = new JsonResponseHandlerImpl(defaultJiraLocation, defaultResponse, NegativeResponseStatus.errorOccured(defaultJiraLocation, "singleError"));
        Either<NegativeResponseStatus, MyResponseType> parsedResponse = myHandler.handle(response);
        testNegativeResponse(NegativeResponseStatus.Status.ERROR_OCCURRED, parsedResponse);
    }


    private void testStandardErrorResponse(int responseCode, NegativeResponseStatus.Status expectedStatus) throws Exception {
        Response response = mockUnsuccesfulResponse(responseCode);
        Either<NegativeResponseStatus, MyResponseType> result = jsonResponseHandler.handle(response);
        testNegativeResponse(expectedStatus, result);
    }

    private void testNegativeResponse(NegativeResponseStatus.Status expectedStatus, Either<NegativeResponseStatus, ?> givenResponse){
        assertTrue(givenResponse.isLeft());
        NegativeResponseStatus status = givenResponse.left().get();
        assertEquals(expectedStatus, status.getResult());
        assertEquals(defaultJiraLocation, status.getJiraLocation());
    }

    private Response mockUnsuccesfulResponse(int status){
        Response response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(status);
        when(response.isSuccessful()).thenReturn(false);
        return response;
    }

    private Response mockSuccesfulResponse(int status){
        Response response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(status);
        when(response.isSuccessful()).thenReturn(true);
        return response;
    }


}
