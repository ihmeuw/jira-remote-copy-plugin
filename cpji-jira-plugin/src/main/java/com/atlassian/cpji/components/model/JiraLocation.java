package com.atlassian.cpji.components.model;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.google.common.base.Preconditions;

/**
 * @since v3.0
 */
public class JiraLocation {


    private final String id;

    private final String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JiraLocation that = (JiraLocation) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public JiraLocation(final String id, final String name) {
        this.id = Preconditions.checkNotNull(id);
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ApplicationId toApplicationId() {
        return new ApplicationId(id);
    }

    public static JiraLocation fromAppLink(ApplicationLink appLink) {
        return new JiraLocation(appLink.getId().get(), appLink.getName());
    }

}
