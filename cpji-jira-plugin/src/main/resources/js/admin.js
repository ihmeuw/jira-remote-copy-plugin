AJS.toInit(function () {
    (function ($) {

        var copyAdmin = {
            dialog : new JIRA.FormDialog({
                id: "configure-remote-copy-dialog",
                trigger: "#configure_cpji",
                ajaxOptions: JIRA.Dialogs.getDefaultAjaxOptions,
                autoClose : false
            }),

            prepareDialog : function(e, dialog){
                if(dialog == copyAdmin.dialog)
                    copyAdmin.initComponents();

                },

            initComponents : function(){
                if(!$('#issuetype').length){
                    return;
                }
                $('#issuetype').change(copyAdmin.onIssueTypeChange);

                new AJS.MultiSelect({
                    element: $("#groups"),
                    stallEventBind: false,
                    itemAttrDisplayed: "label"
                });

                JIRA.trace("cpji.load.completed");
            },


            onIssueTypeChange : function(){
                $('.cpji-loading').show();
                var fields = $('#cpji-fields');
                fields.empty();
                var projectKey = $('#project-key').val();
                var issueType = $('#issuetype').val();
                $.get(contextPath + '/GetFieldsHtmlAction.jspa?projectKey=' + projectKey + '&issuetype=' + issueType, function (data) {
                    fields.append($(data));
                    $('.cpji-loading').hide();
                    JIRA.trigger(JIRA.Events.NEW_CONTENT_ADDED, [fields]);
                    JIRA.trace("cpji.fields.load.completed");
                });
            }
    
        };

        if(!JIRA.plugins){
            JIRA.plugins = {
                cpjiPlugin : {}
            };
        } else {
            JIRA.plugins.cpjiPlugin = {};
        }



        JIRA.plugins.cpjiPlugin.admin = copyAdmin;
        JIRA.plugins.cpjiPlugin.admin.initComponents();
        $(document).bind("dialogContentReady", JIRA.plugins.cpjiPlugin.admin.prepareDialog);
    })(AJS.$);
});