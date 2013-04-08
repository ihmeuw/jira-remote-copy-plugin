AJS.$(function ($) {

	var SingleSelect = AJS.SingleSelect.extend({
		init: function(options) {
			this._super(options);
			this.listController._originalMakeResultDiv = this.listController._makeResultDiv;
			this.listController._makeResultDiv = function (data, context) {
				var query = typeof(context) == "string" ? context : context.query;
				if (query != "") {
					return this._originalMakeResultDiv(data, context);
				} else {
					var first = [ data[0] ], $resultDiv = this._originalMakeResultDiv(first, context);
					$resultDiv.find("ul").append(AJS.$("<li class='no-suggestions'>" + AJS.I18n.getText("cpji.continue.typing.to.see.more") + "</li>"));
					return $resultDiv;
				}
			};
		}
	});

    var copyIssue = {

        settings: {
            contextPath: contextPath,
            loader: null,
            projectsSelect: null,
            submitButton: null,
            defaultInstance: "LOCAL",
            defaultProject: null,
			recentlyUsedProjects: null
        },

		recentlyUsed: [],

        singleSelect: null,

        initSelectProject: function (settings) {
            $.extend(copyIssue.settings, settings);
            copyIssue.toggleLoadingState(true);
            copyIssue.getProjects();
        },


        toggleLoadingState: function (visible) {
            copyIssue.settings.loader.toggleClass("hidden", !visible);
            copyIssue.settings.projectsSelect.toggleClass("hidden", visible);
        },

        getProjects: function () {
            JIRA.SmartAjax.makeRequest({
                url: (copyIssue.settings.contextPath + "/rest/copyissue/1.0/remotes/availableDestinations?issueId=" + AJS.$("input[name=id]").val()),
                success: copyIssue.getProjectsSuccess
            });
        },

		convertRecentlyUsed: function () {
			var elem = $("<optgroup></optgroup>");

			elem.attr('label', AJS.I18n.getText('cpji.recently.used'));

			if (!this.recentlyUsed || this.recentlyUsed.length == 0) {
				return null;
			}

			for (var i in this.recentlyUsed) {
				var project = this.recentlyUsed[i], projElem = $("<option></option>");
				if (project.locationId == copyIssue.settings.defaultInstance && project.key == copyIssue.settings.defaultProject) {
					//for some strange reason setting this attribute through explicit attr call sometimes it is not preserved
					projElem = $("<option selected='selected'></option>");
				}
				projElem.attr('value', project.locationId + "|" + project.key);
				projElem.text(project.name + " (" + project.key + ") [" + project.locationName + "]");
				elem.append(projElem);
			}
			return elem;
		},

        convertGroupToOptgroup: function (json) {
            var elem = $("<optgroup></optgroup>"), locationId = json.id, recentlyUsed = this.settings.recentlyUsedProjects[locationId];
            elem.attr('label', json.name);

            for (var i in json.projects) {
                var project = json.projects[i];

				if (_.indexOf(recentlyUsed, project.key) != -1) {
					project.locationId = json.id;
					project.locationName = json.name;
					this.recentlyUsed.push(project);
				} else {
					var projElem = $("<option></option>");
					if (json.id == copyIssue.settings.defaultInstance && project.key == copyIssue.settings.defaultProject) {
						//for some strange reason setting this attribute through explicit attr call sometimes it is not preserved
						projElem = $("<option selected='selected'></option>");
					}
					projElem.attr('value', json.id + "|" + project.key);
					projElem.text(project.name + " (" + project.key + ")");
					elem.append(projElem);
				}
            }
            return elem;
        },

        getProjectsSuccess: function (data) {
            copyIssue.settings.container.find(".description").remove();

            if (data.projects) {
                var projectCount = _.reduce(data.projects, function (sum, server) {
                    return sum + server.projects.length
                }, 0);
                if (projectCount > 0) {
                    for (var server in data.projects) {
                        var serverElem = copyIssue.convertGroupToOptgroup(data.projects[server]);
                        copyIssue.settings.projectsSelect.append(serverElem);
                    }
					var recentlyUsed = copyIssue.convertRecentlyUsed();
					if (recentlyUsed) {
						copyIssue.settings.projectsSelect.prepend(recentlyUsed);
					}
                    copyIssue.toggleLoadingState(false);
                    copyIssue.prepareSelect();
                } else {
                    copyIssue.settings.container.find(".field-value").hide();
                    copyIssue.settings.container.append(RIC.Templates.formError({id: "noDestinationProjectError",
						msg: AJS.I18n.getText("cpji.you.dont.have.create.issue.permission.for.any.project")}));
                    copyIssue.onValueUnselected();
                    copyIssue.settings.loader.toggleClass("hidden", true);
                    copyIssue.settings.projectsSelect.toggleClass("hidden", true);
                }
            }

            if (data.failures) {
				var errors =  RIC.Templates.remoteDestinationsNotAvailable(data.failures);
				if (errors.trim() != "") {
                	copyIssue.settings.container.append(AJS.$("<div class='description'></div>").append(errors));
				}
            }
        },

        prepareSelect: function () {
            copyIssue.singleSelect = new SingleSelect({
                element: copyIssue.settings.projectsSelect,
                width: "350px",
                showDropdownButton: true,
                itemAttrDisplayed: "label",
				maxInlineResultsDisplayed: 5
            });
            copyIssue.settings.projectsSelect.bind("selected", copyIssue.onValueSelected);
            copyIssue.settings.projectsSelect.bind("unselect", copyIssue.onValueUnselected);
        },

        onValueSelected: function (item) {
            copyIssue.settings.submitButton.attr("disabled", false);
            copyIssue.settings.submitButton.attr("aria-disabled", false);
        },

        onValueUnselected: function (item) {
            copyIssue.settings.submitButton.attr("disabled", true);
            copyIssue.settings.submitButton.attr("aria-disabled", true);
        }

    }

    if (!JIRA.plugins) {
        JIRA.plugins = {
            cpjiPlugin: {}
        };
    } else {
        JIRA.plugins.cpjiPlugin = {};
    }

    JIRA.plugins.cpjiPlugin.selectTarget = copyIssue;


    copyIssue.initSelectProject({
        container: $("#targetEntityLink-container"),
        projectsSelect: $("#targetEntityLink"),
        loader: $("#targetEntityLinkLoader"),
        submitButton: $("#select-project-submit"),
        defaultProject: $("#targetEntityLink-container").data("selected-project-key"),
		recentlyUsedProjects: $("#targetEntityLink-container").data("recently-used-projects")
    });
});