package com.atlassian.cpji.util;

import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @since v3.0
 */
public class ResponseUtil {
    private static final Logger log = LoggerFactory.getLogger(ResponseUtil.class);

    public static final long DEFAULT_MAX_LENGTH = 10*1024*1024;
    public static final int DEFAULT_BUFFER_LENGTH = 10*1024;

    public static String getResponseAsTrimmedString(Response response, final long max_length) throws ResponseException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(response.getResponseBodyAsStream()));
        final int buffer_length = DEFAULT_BUFFER_LENGTH > max_length ? (int)max_length : DEFAULT_BUFFER_LENGTH;
        try{
            final StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[buffer_length];
            int len = 0;
            while(( len = reader.read(buffer)) != -1){
                stringBuilder.append(buffer, 0, len);
                if(stringBuilder.length() > max_length){
                    log.error("Response body exceeded maximum permitted length ({}). Please contact support for assistance.", max_length);
                    return stringBuilder.toString();
                }
            }

            return stringBuilder.toString();
        } catch(IOException e){
            throw new ResponseException(e);
        } finally {
            try{
                reader.close();
            } catch(IOException e){
                log.warn("Cannot close reader");
            }
        }

    }

    public static String getResponseAsTrimmedString(Response response) throws ResponseException {
        return getResponseAsTrimmedString(response, DEFAULT_MAX_LENGTH);
    }

}
