package com.atlassian.cpji.rest.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @since v1.0
 */
public class DateAdapter extends XmlAdapter<String, Date>
{
    public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";



    @Override
    public Date unmarshal(final String s) throws Exception
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

    @Override
    public String marshal(final Date date) throws Exception
    {
       return new SimpleDateFormat(TIME_FORMAT).format(date);
    }
}
