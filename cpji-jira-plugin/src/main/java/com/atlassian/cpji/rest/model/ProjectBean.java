package com.atlassian.cpji.rest.model;

import com.atlassian.jira.rest.client.domain.BasicProject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="project")
public class ProjectBean {

    @XmlElement
    private String name;
    @XmlElement
    private String key;

    public ProjectBean(){

    }

    public ProjectBean(final String name, final String key) {
        this.name = name;
        this.key = key;
    }

    public ProjectBean(BasicProject project){
        this.name = project.getName();
        this.key = project.getKey();
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }




}
