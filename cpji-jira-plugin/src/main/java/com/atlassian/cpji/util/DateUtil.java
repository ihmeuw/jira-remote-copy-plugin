package com.atlassian.cpji.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @since v5.0
 */
public class DateUtil
{
    private DateUtil() {}

    public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public static Date parseString(final String s)
    {
        try
        {
            return new SimpleDateFormat(TIME_FORMAT).parse(s);
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("Error parsing time: " + s, e);
        }
    }

    public static boolean isValidDate(final String s)
    {
        try
        {
            parseString(s);
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    public static String toString(final Date date)
    {
       return new SimpleDateFormat(TIME_FORMAT).format(date);
    }
}
