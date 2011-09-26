package com.atlassian.cpji.rest.model;

import com.atlassian.cpji.util.DateUtil;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Date;

/**
 * @since v1.0
 */
public class DateAdapter extends XmlAdapter<String, Date>
{
    @Override
    public Date unmarshal(final String s) throws Exception
    {
        return DateUtil.parseString(s);
    }

    @Override
    public String marshal(final Date date) throws Exception
    {
       return DateUtil.toString(date);
    }
}
