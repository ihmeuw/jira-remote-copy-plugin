AJS.test.require("com.atlassian.cpji.cpji-jira-plugin:ajs-test-walkaround");
AJS.test.require("com.atlassian.cpji.cpji-jira-plugin:admin-js");

var generalTools = {

    init:function ()
    {
        this.put = JIRA.plugins.cpjiPlugin.admin;
        this.fixture = jQuery("#qunit-fixture");
    }
};


module("prepareDialog", {
    setup:function ()
    {
        generalTools.init.call(this);
        sinon.stub(this.put, "initComponents");
    },
    teardown:function ()
    {
        this.put.initComponents.restore();
    }
});

test("prepareDialog should be fired on dialogContentReady with our dialog", function ()
{
    jQuery(document).trigger("dialogContentReady", [this.put.dialog]);
    ok(this.put.initComponents.calledOnce, "initComponents ws called");
});

test("prepareDialog should not be fired on dialogContentReady with another (fake) dialog", function ()
{
    var dialog = {
        get$popupContent:sinon.stub().returns(this.fixture)
    };
    jQuery(document).trigger("dialogContentReady", [dialog]);
    equal(this.put.initComponents.callCount, 0, "initComponents was not called");
});

module("initComponents", {
    setup:function ()
    {
        generalTools.init.call(this);
        this.fixture.append("<select id='issuetype'><option>a</option><option>b</option></select>");
        this.fixture.append("<select id='groups'><option>a</option><option>b</option></select>");
        sinon.stub(this.put, "onIssueTypeChange");
        sinon.stub(AJS, "MultiSelect");
    },

    teardown:function ()
    {
        this.put.onIssueTypeChange.restore();
        AJS.MultiSelect.restore();
    }
});

test("initialization", function ()
{
    this.put.initComponents();

    ok(AJS.MultiSelect.calledOnce, "MutliSelect is initialized")
    var selectArgs = AJS.MultiSelect.firstCall.args[0];
    equal(selectArgs.element.html(), jQuery("#groups", this.fixture).html(), "MultiSelect uses group field");
    equal(selectArgs.itemAttrDisplayed, "label", "MultiSelect displays labels");
});


test("Reacting for issue type change", function()
{
    this.put.initComponents();

    var obj = jQuery("#issuetype", this.fixture);
    obj.change();
    ok(this.put.onIssueTypeChange.calledOnce, "issue type field onChange was called");
});


module("onIssueTypeChange", {
    setup : function(){
        generalTools.init.call(this);
        var markup = '<div class="cpji-loading" style="display: none;">loader</div>'
                + '<div id="cpji-fields">fields</div>'
                + '<select id="issuetype"><option selected="selected" value="MY_SELECTED_KEY">a</option><option value="NOTSEL">b</option></select>'
                + '<input type="hidden" id="project-key" value="PROJECT_KEY"/>';
        this.fixture.append(markup);
        sinon.stub(jQuery, "get");
    },
    teardown : function(){
        jQuery.get.restore();
    }
});

test("General", function(){

    this.put.onIssueTypeChange();

    ok(jQuery(".cpji-loading").is(":visible"), "loading progress is visible");
    ok(jQuery("#cpji-fields").is(":empty"), "current fields are truncated");
    ok(jQuery.get.calledOnce, "GET request is started");

    var args = jQuery.get.firstCall.args;
    var key = "projectKey=PROJECT_KEY";
    var issueType = "issuetype=MY_SELECTED_KEY"
    notEqual(args[0].indexOf(key), -1, "Query string contains project key");
    notEqual(args[0].indexOf(issueType), -1, "Query string contains issue type");
});


test("AJAX Result", function(){
    var fields = jQuery("#cpji-fields");
    var getResult = '<div>myResult</div>';
    jQuery.get.callsArgWith(1, getResult);
    sinon.stub(JIRA, "trigger");

    this.put.onIssueTypeChange();

    ok(jQuery(".cpji-loading").is(":hidden"), "loading progress is hidden");
    equal(fields.html(), getResult, "our result is inserted");
    ok(JIRA.trigger.firstCall.calledWith(JIRA.Events.NEW_CONTENT_ADDED), "NEW_CONTENT_ADDED event was triggered");

    var eventArgs = JIRA.trigger.firstCall.args[1];
    equal(eventArgs[0].html(), fields.html(), "... with our fields elemenet as context");

    JIRA.trigger.restore();
});