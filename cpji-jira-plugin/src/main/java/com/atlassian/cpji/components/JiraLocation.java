package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;

/**
 * @since v3.0
 */
public class JiraLocation {


    private final String id;

    private final String name;

    public JiraLocation(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static JiraLocation fromAppLink(ApplicationLink appLink) {
        return new JiraLocation(appLink.getId().get(), appLink.getName());
    }

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
}
