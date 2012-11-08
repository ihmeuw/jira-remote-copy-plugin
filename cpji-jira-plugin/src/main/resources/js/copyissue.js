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
            $.getJSON(copyIssue.settings.contextPath + "/rest/copyissue/1.0/project/destination", copyIssue.getProjectsSuccess);
        },

        convertGroupToOptgroup : function(json){
            var elem = $("<optgroup></optgroup>");
            elem.attr('label', json.name);

            for(var i in json.projects){
                var project = json.projects[i];
                var projElem = $("<option></option>");
                projElem.attr('value', json.id + "|" + project.key);
                projElem.text(project.name + " (" + project.key + ")");
                elem.append(projElem);
            }
            return elem;
        },


        getProjectsSuccess : function(data){
            for(var server in data){
                var serverElem = copyIssue.convertGroupToOptgroup(data[server]);
                copyIssue.settings.projectsSelect.append(serverElem);
            }
            copyIssue.toggleLoadingState(false);
            copyIssue.prepareSelect();


        },
        prepareSelect : function(){
            copyIssue.singleSelect = new AJS.SingleSelect({
                element: copyIssue.settings.projectsSelect,
                showDropdownButton: true,
                itemAttrDisplayed: "label"
            });
            copyIssue.settings.projectsSelect.bind("selected", copyIssue.onValueSelected);
            copyIssue.settings.projectsSelect.bind("unselect", copyIssue.onValueUnselected);
        },

        onValueSelected : function (item) {
            copyIssue.settings.submitButton.prop("disabled", false);
            copyIssue.settings.submitButton.attr("aria-disabled", false);
        },

        onValueUnselected : function (item) {
            copyIssue.settings.submitButton.prop("disabled", true);
            copyIssue.settings.submitButton.attr("aria-disabled", true);
        }

    }

    copyIssue.initSelectProject({
        projectsSelect: $("#targetEntityLink"),
        loader : $("#targetEntityLinkLoader"),
        submitButton : $("#select-project-submit")
    });
});