package com.atlassian.cpji.rest.model;

import com.atlassian.jira.util.ErrorCollection;
import com.google.common.collect.ImmutableList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 *  @since v1.0
 */
@XmlRootElement (name = "error")
public class ErrorBean
{
    @XmlElement (name = "errors")
    private List<String> errors;

    @SuppressWarnings("unused")
    public ErrorBean()
    {
    }

	public ErrorBean(final String...errors) {
		this(ImmutableList.copyOf(errors));
	}

    public ErrorBean(final List<String> errors)
    {
        this.errors = errors;
    }

    public List<String> getErrors()
    {
        return errors;
    }

    public static ErrorBean convertErrorCollection(ErrorCollection errorCollection)
    {
        List<String> errors = new ArrayList<String>();
        for (String error : errorCollection.getErrors().values())
        {
            errors.add(error);
        }
        errors.addAll(errorCollection.getErrorMessages());
        return new ErrorBean(errors);
    }

}
