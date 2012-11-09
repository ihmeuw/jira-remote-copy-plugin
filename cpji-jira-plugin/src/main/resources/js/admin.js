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
                if(dialog != copyAdmin.dialog)
                    return;

                $('#select-issue-type').change(copyAdmin.onIssueTypeChange);
                $('#select-issue-type').trigger('change');

				new AJS.MultiSelect({
					element: AJS.$("#groups"),
					stallEventBind: false,
					itemAttrDisplayed: "label"
				});
            },


            onIssueTypeChange : function(){
                $('.cpji-loading').show();
                $('#fields').empty();
                var projectKey = $('#project-key').val();
                var issueType = $('#select-issue-type :selected').val();
                $.get(contextPath + '/GetFieldsHtmlAction.jspa?projectKey=' + projectKey + '&selectedIssueTypeId=' + issueType, function (data) {
                    $('#fields').append($(data));
                    $('.cpji-loading').hide();
                });
            }

        }

        $(document).bind("dialogContentReady", copyAdmin.prepareDialog);
    })(AJS.$);
});