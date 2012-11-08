AJS.$(function($){

    var copyIssue = {

        settings : {
            contextPath : contextPath,
            loader : null,
            projectsSelect : null,
            form : null,
            messagesBar : $("#aui-message-bar")
        },

        singleSelect : null,

        initSelectProject : function(settings){
            $.extend(copyIssue.settings, settings);
            copyIssue.toggleLoadingState(true);
            copyIssue.getProjects();
            copyIssue.settings.form.submit(copyIssue.validateForm);
        },

        validateForm : function(){
            var isElementSelected = copyIssue.singleSelect.getSelectedDescriptor() != undefined;
            if(!isElementSelected){
                AJS.messages.error({
                    title:AJS.I18n.getText("cpji.project.validation.not.selected"),
                    body: "<p>" + AJS.I18n.getText("cpji.project.validation.invalid.value") +"</p>"
                });
                return false;
            } else {
                return true;
            }
            return isElementSelected;
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
        },

        onValueSelected : function () {
            copyIssue.settings.messagesBar.empty();
        }

    }

    copyIssue.initSelectProject({
        projectsSelect: $("#targetEntityLink"),
        loader : $("#targetEntityLinkLoader"),
        form : $("#stepped-process form.aui")
    });
});