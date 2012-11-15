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
                if(!$('#select-issue-type').length){
                    return;
                }
                $('#select-issue-type').change(copyAdmin.onIssueTypeChange);

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
                var issueType = $('#select-issue-type :selected').val();
                $.get(contextPath + '/GetFieldsHtmlAction.jspa?projectKey=' + projectKey + '&selectedIssueTypeId=' + issueType, function (data) {
                    fields.append($(data));
                    $('.cpji-loading').hide();
                    JIRA.trigger(JIRA.Events.NEW_CONTENT_ADDED, [fields]);
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