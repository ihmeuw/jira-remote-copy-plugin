AJS.$(function($){

    var copyIssue = {

        settings : {
            contextPath : contextPath,
            loader : null,
            projectsSelect : null,
            submitButton : null
        },

        singleSelect : null,

        initSelectProject : function(settings){
            $.extend(copyIssue.settings, settings);
            copyIssue.toggleLoadingState(true);
            copyIssue.getProjects();
        },


        toggleLoadingState : function(visible){
            copyIssue.settings.loader.toggleClass("hidden", !visible);
            copyIssue.settings.projectsSelect.toggleClass("hidden", visible);
        },

        getProjects : function(){
            JIRA.SmartAjax.makeRequest({
				url: (copyIssue.settings.contextPath + "/rest/copyissue/1.0/remotes/availableDestinations"),
				success: copyIssue.getProjectsSuccess
			});
        },

        convertGroupToOptgroup : function(json) {
            var elem = $("<optgroup></optgroup>");
            elem.attr('label', json.name);

            for(var i in json.projects) {
                var project = json.projects[i];
                var projElem = $("<option></option>");
                projElem.attr('value', AJS.escapeHtml(json.id + "|" + project.key));
                projElem.text(project.name + " (" + project.key + ")");
                elem.append(projElem);
            }
            return elem;
        },

        getProjectsSuccess : function(data) {
			if (data.projects) {
				for(var server in data.projects) {
					var serverElem = copyIssue.convertGroupToOptgroup(data.projects[server]);
					copyIssue.settings.projectsSelect.append(serverElem);
				}
			}
			if (data.failures) {

			}
            copyIssue.toggleLoadingState(false);
            copyIssue.prepareSelect();
        },

        prepareSelect : function() {
            copyIssue.singleSelect = new AJS.SingleSelect({
                element: copyIssue.settings.projectsSelect,
                showDropdownButton: true,
                itemAttrDisplayed: "label"
            });
            copyIssue.settings.projectsSelect.bind("selected", copyIssue.onValueSelected);
            copyIssue.settings.projectsSelect.bind("unselect", copyIssue.onValueUnselected);
        },

        onValueSelected : function (item) {
            copyIssue.settings.submitButton.attr("disabled", false);
            copyIssue.settings.submitButton.attr("aria-disabled", false);
        },

        onValueUnselected : function (item) {
            copyIssue.settings.submitButton.attr("disabled", true);
            copyIssue.settings.submitButton.attr("aria-disabled", true);
        }

    }

    copyIssue.initSelectProject({
        projectsSelect: $("#targetEntityLink"),
        loader : $("#targetEntityLinkLoader"),
        submitButton : $("#select-project-submit")
    });
});