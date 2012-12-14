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


test("onValueSelected/Unselected", function () {
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

test("preparing select", function () {
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


module("getProjectSuccess", {
    setup: function () {
        copyGeneralTools.init.call(this);
        var container = jQuery("<div></div>")
        this.put.settings.container = container;

        this.prepareSingleServerResponse = function (appId) {
            var result = {
                id: appId,
                projects: []
            };
            for (var i = 1; i < arguments.length; i += 2) {
                result.projects.push({
                    key: arguments[i],
                    name: arguments[i + 1]
                });
            }
            return result;
        };

        this.prepareResponseScheme = function () {
            return {
                projects: [],
                failures: {}
            };
        };

        this.successfulResponse = function () {
            var scheme = this.prepareResponseScheme();
            for (var i = 0; i < arguments.length; i++) {
                scheme.projects.push(arguments[i]);
            }
            return scheme;
        };


    },
    teardown: function () {
    }
});

test("preparing projects list", function () {
    this.put.settings.container.append("<div class='description'>This is some strange description</div>");

    sinon.stub(this.put, "prepareSelect");
    sinon.stub(this.put, "toggleLoadingState");
    sinon.stub(this.put, "convertGroupToOptgroup");
    this.put.convertGroupToOptgroup.returnsArg(0);

    var select = jQuery("<div>mySelect</div>");
    this.put.settings.projectsSelect = select;
    sinon.stub(this.put.settings.projectsSelect, "append");


    var localSrv = this.prepareSingleServerResponse("LOCAL", "TEST", "Testproj", "ABC", "Abc proj");
    var remoteSrv = this.prepareSingleServerResponse("2w3459832", "TEST", "Testproj", "ABC", "Abc proj");
    var response = this.successfulResponse(localSrv, remoteSrv);

    this.put.getProjectsSuccess(response);

    equal("", this.put.settings.container.html(), "Container shoud be empty");
    ok(this.put.convertGroupToOptgroup.calledWith(localSrv), "Conversion was run");
    ok(this.put.convertGroupToOptgroup.calledWith(remoteSrv));
    ok(this.put.settings.projectsSelect.append.calledWith(localSrv), "Conversion result was appended to selection");
    ok(this.put.settings.projectsSelect.append.calledWith(remoteSrv));
    ok(this.put.toggleLoadingState.calledWith(false), "UI should leave loading state");
    ok(this.put.prepareSelect.calledOnce, "Moder singleSelect should be prepared");


    this.put.convertGroupToOptgroup.restore();
    this.put.prepareSelect.restore();
    this.put.toggleLoadingState.restore();
    this.put.settings.projectsSelect.append.restore();
});


test("displaying error message on zero active projects", function () {
    var response = this.successfulResponse();

    var fakeLoader = jQuery("<div>loader</div>");
    var fakeSelect = jQuery("<div>select</div>");

    this.put.settings.loader = fakeLoader;
    this.put.settings.projectsSelect = fakeSelect;

    sinon.stub(RIC.Templates, "warningMsg");
    RIC.Templates.warningMsg.returns("Rendered template");

    sinon.stub(this.put, "onValueUnselected");


    this.put.getProjectsSuccess(response);

    equal("Rendered template", this.put.settings.container.html(), "Warning is appended to message");
    ok(this.put.onValueUnselected.calledOnce, "Treat it as unselection");
    ok(fakeLoader.hasClass("hidden"), "hide loader");
    ok(fakeSelect.hasClass("hidden"), "hide singleSelect");


    RIC.Templates.warningMsg.restore();
    this.put.onValueUnselected.restore();

});


test("displaying failures", function () {
    var response = this.successfulResponse(this.prepareSingleServerResponse("LOCAL", "KEY", "Project"));
    response.failures.firstFailure = true;
    response.failures.secondFailure = true;
    sinon.stub(RIC.Templates, "remoteDestinationsNotAvailable");
    RIC.Templates.remoteDestinationsNotAvailable.returns("Rendered template");


    this.put.getProjectsSuccess(response);

    equal('<div class="description">Rendered template</div>', this.put.settings.container.html());


    RIC.Templates.remoteDestinationsNotAvailable.restore();
});


test("converting groups to optGroups and selecting default project", function () {
    var localSrv = this.prepareSingleServerResponse("LOCAL", "KEY", "FirstProject", "OTH", "Other project");
    var remoteSrv = this.prepareSingleServerResponse("REMOTE", "KEY", "FirstProject", "OTH", "Other project");
    this.put.settings.defaultProject = "KEY";

    var result = this.put.convertGroupToOptgroup(localSrv);
    equal('<option value="LOCAL|KEY" selected="selected">FirstProject (KEY)</option><option value="LOCAL|OTH">Other project (OTH)</option>', result.html());


    result = this.put.convertGroupToOptgroup(remoteSrv);
    equal('<option value="REMOTE|KEY">FirstProject (KEY)</option><option value="REMOTE|OTH">Other project (OTH)</option>', result.html());
});