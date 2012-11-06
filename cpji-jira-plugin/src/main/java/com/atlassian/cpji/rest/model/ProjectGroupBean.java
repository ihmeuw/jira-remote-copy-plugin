package com.atlassian.cpji.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.2
 */
@XmlRootElement(name = "projectGroup")
public class ProjectGroupBean {

    @XmlElement
    private String name;

    @XmlElement
    private String id;

    @XmlElement
    private List<ProjectBean> projects;

    public ProjectGroupBean() {
    }

    public ProjectGroupBean(final String name, final String id, final List<ProjectBean> projects) {
        this.name = name;
        this.id = id;
        this.projects = projects;
    }

    public List<ProjectBean> getProjects() {
        return projects;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
