AJS.test.require("com.atlassian.cpji.cpji-jira-plugin-test:ajs-test-walkaround");
AJS.test.require("com.atlassian.cpji.cpji-jira-plugin:selectTargetProjectAction");

var copyGeneralTools = {
    init: function () {
        this.put = JIRA.plugins.cpjiPlugin.selectTarget;
        this.fixture = jQuery("#qunit-fixture");
    }
};


module("simple functions", {
    setup: function () {
        copyGeneralTools.init.call(this);
        sinon.stub(this.put, "getProjects");
    },
    teardown: function () {
        this.put.getProjects.restore();
    }
});


test("initialization", function () {
    sinon.stub(this.put, "toggleLoadingState");

    this.put.initSelectProject({
        defaultProject: "tstPROJ"
    });

    equal("LOCAL", this.put.settings.defaultInstance, "Preserving default settings");
    equal("tstPROJ", this.put.settings.defaultProject, "Applying custom settings");

    ok(this.put.getProjects.calledOnce, "Loading projects")
    ok(this.put.toggleLoadingState.calledWith(true), "Changing state to loading");

    this.put.toggleLoadingState.restore();
});


test("toggleLoadingState", function () {
    var loader = jQuery("<div class='hidden'>myLoader</div>");
    var select = jQuery("<div>mySelect</div>");
    this.fixture.append(loader).append(select);

    this.put.initSelectProject({
        loader: loader,
        projectsSelect: select
    });

    ok(!loader.hasClass("hidden"), "Init shows loader");
    ok(select.hasClass("hidden"), "... and hides select");

    this.put.toggleLoadingState(false);

    ok(loader.hasClass("hidden"), "Loader is hidden");
    ok(!select.hasClass("hidden"), "... and select is shown");
});


test("onValueSelected/Unselected", function() {
    var button = jQuery("<input type='Button' value='button'/>");
    this.fixture.append(button);

    this.put.initSelectProject({
        submitButton: button
    });

    this.put.onValueUnselected();
    equal(true, button.attr("disabled"));
    equal("true", button.attr("aria-disabled"));

    this.put.onValueSelected();
    equal(false, button.attr("disabled"));
    equal("false", button.attr("aria-disabled"));
});

test("preparing select", function(){
    var select = jQuery("<div>fakeSelect</div>");
    sinon.stub(AJS, "SingleSelect");
    this.put.settings.projectsSelect = select;
    sinon.stub(select, "bind");


    this.put.prepareSelect();

    ok(AJS.SingleSelect.calledOnce);
    ok(this.put.settings.projectsSelect.bind.calledWith("selected", this.put.onValueSelected));
    ok(this.put.settings.projectsSelect.bind.calledWith("unselect", this.put.onValueUnselected));


    AJS.SingleSelect.restore();
    select.bind.restore();
});