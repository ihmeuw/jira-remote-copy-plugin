package com.atlassian.cpji.components.model;

import com.atlassian.applinks.api.ApplicationId;
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

    public ApplicationId toApplicationId(){
        return new ApplicationId(id);
    }

    public static JiraLocation fromAppLink(ApplicationLink appLink) {
        return new JiraLocation(appLink.getId().get(), appLink.getName());
    }

}
