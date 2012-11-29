package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.cpji.tests.pageobjects.SingleSelect;
import com.atlassian.jira.pageobjects.util.TraceContext;
import com.atlassian.jira.pageobjects.util.Tracer;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.openqa.selenium.By;

import javax.inject.Inject;

public class DefaultValuesFragment
{

    @Inject
    private TraceContext traceContext;
    @Inject
    protected PageBinder pageBinder;
    @Inject
    protected PageElementFinder elementFinder;

    protected SingleSelect issueType;
    protected SingleSelect reporter;

    @ElementBy(id="cpji-required-fields")
    protected PageElement root;

    @Init
    public void init(){
        issueType = pageBinder.bind(SingleSelect.class, elementFinder.find(By.id("cpji-issuetype-field")));
        reporter = pageBinder.bind(SingleSelect.class, root.find(By.id("cpji-fields")));
    }

    public DefaultValuesFragment typeIntoReporterField(String reporter){
        this.reporter.type(reporter);
        return this;
    }

    public DefaultValuesFragment setReporter(String reporter){
        this.reporter.select(reporter);
        return this;
    }

    public String getReporterText()
    {
        return reporter.getValue();
    }


    public DefaultValuesFragment changeIssueType(String issueType){
        Tracer checkpoint = traceContext.checkpoint();
        this.issueType.select(issueType);
        traceContext.waitFor(checkpoint, "cpji.fields.load.completed");
        return pageBinder.bind(DefaultValuesFragment.class);
    }

}
