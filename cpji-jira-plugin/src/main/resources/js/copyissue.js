AJS.$(function($){

    var copyIssue = {

        context : contextPath,

        loader : null,
        projectsSelect : null,

        initSelectProject : function(select, loader){
            copyIssue.projectsSelect = select;
            copyIssue.loader = loader;
            loader.show();
            select.hide();
            copyIssue.getProjects();
        },

        getProjects : function(){
            $.getJSON(copyIssue.context + "/rest/copyissue/1.0/copyissue/projects", copyIssue.getProjectsSuccess);
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
                copyIssue.projectsSelect.append(serverElem);
            }
            copyIssue.projectsSelect.show();
            copyIssue.loader.hide();
            copyIssue.prepareSelect();


        },
        prepareSelect : function(){
            new AJS.SingleSelect({
                element: copyIssue.projectsSelect,
                itemAttrDisplayed: "label",
                getDisplayVal : function(){
                    return "aaa";
                }
            });
        }

    }

    copyIssue.initSelectProject($("#targetEntityLink"), $("#targetEntityLinkLoader"));
});