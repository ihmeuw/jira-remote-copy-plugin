package com.atlassian.cpji.rest.model;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.4
 */
@XmlRootElement (name = "customField")
public class CustomFieldBean
{
    @XmlElement (name = "customFieldType")
    private String customFieldType;

    @XmlElement (name = "customFieldName")
    private String customFieldName;

    @XmlElement (name = "id")
    private String id;

    @XmlElement (name = "values")
    private List<String> values;


     @SuppressWarnings("unused")
    public CustomFieldBean()
    {
    }

    public CustomFieldBean(final String customFieldType, final String customFieldName, final String id, final List<String> values)
    {
        this.customFieldType = customFieldType;
        this.customFieldName = customFieldName;
        this.id = id;
        this.values = values;
    }

    public String getCustomFieldType()
    {
        return customFieldType;
    }

    public String getCustomFieldName()
    {
        return customFieldName;
    }

    public String getId()
    {
        return id;
    }

    public List<String> getValues()
    {
        return values;
    }
}
