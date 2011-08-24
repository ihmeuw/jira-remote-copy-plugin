package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v1.0
 */
@XmlRootElement (name = "timeTracking")
public class TimeTrackingBean
{
    @XmlElement
    private Long originalEstimate;

    @XmlElement
    private Long timeSpent;

    @XmlElement
    private Long estimate;

    @SuppressWarnings("unused")
    public TimeTrackingBean() {}

    public TimeTrackingBean(final Long originalEstimate, final Long timeSpent, final Long estimate)
    {
        this.originalEstimate = originalEstimate;
        this.timeSpent = timeSpent;
        this.estimate = estimate;
    }

    public Long getOriginalEstimate()
    {
        return originalEstimate;
    }

    public Long getTimeSpent()
    {
        return timeSpent;
    }

    public Long getEstimate()
    {
        return estimate;
    }
}
