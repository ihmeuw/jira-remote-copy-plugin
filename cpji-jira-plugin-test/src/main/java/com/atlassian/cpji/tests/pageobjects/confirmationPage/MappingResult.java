package com.atlassian.cpji.tests.pageobjects.confirmationPage;

import com.atlassian.cpji.tests.pageobjects.MultiSelectUtil;
import com.atlassian.cpji.tests.pageobjects.SingleSelect;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import org.openqa.selenium.By;

import javax.inject.Inject;

public class MappingResult {
    private final String fieldId;
    private PageElement container;
    private PageElement unmappedNotify;

    @Inject
    private PageElementFinder elementFinder;
    @Inject
    protected PageBinder pageBinder;

    public MappingResult(String fieldId) {
        this.fieldId = fieldId;
    }

    @Init
    public void init() {
        container = elementFinder.find(By.xpath("//label[@for='" + fieldId + "']/.."));
        unmappedNotify = container.find(By.cssSelector("label .icon-info"));
    }

    public TimedQuery<Boolean> hasNotMappedNotify() {
        return unmappedNotify.timed().isVisible();
    }

    public TimedQuery<String> getUnmappedNotifyText() {
        unmappedNotify.click();
        return elementFinder.find(By.id("inline-dialog-unmapped-dialog-" + fieldId)).timed().getText();
    }


    public TimedQuery<String> getMessage(){
        return container.find(By.className("field-value")).timed().getText();
    }


    public MappingResult typeToTextField(CharSequence... value) {
        PageElement textField = elementFinder.find(By.name(fieldId));
        textField.clear();
        if (value != null) {
            textField.type(value);
        }
        return this;
    }

    public MappingResult setMultiSelect(String... items) {
        MultiSelectUtil.setMultiSelect(this.pageBinder, fieldId, items);
        return this;
    }

    public SingleSelect getSingleSelect() {
        return pageBinder.bind(SingleSelect.class, container);
    }

    public SelectElement getSelectElement() {
        return elementFinder.find(By.name(fieldId), SelectElement.class);
    }


}
