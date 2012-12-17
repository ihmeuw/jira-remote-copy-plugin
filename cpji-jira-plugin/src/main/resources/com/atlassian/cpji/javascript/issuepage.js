(function ($) {

    var cloneStyleClass = "issueaction-clone-issue";


    var removeElements = function (context) {
        var $clone = $("." + cloneStyleClass, context);
        if ($clone.length) {
            $clone.parent().remove();
        }
    };

    $(document).ajaxSuccess(function (e, req, ajaxOptions) {
        var operationsUrl = /.*\/rest\/api\/.*\/[0-9]+\/ActionsAndOperations/;
        if (operationsUrl.test(ajaxOptions.url)) {
            removeElements(e.target);
        }
    });

    $(function () {
        removeElements(document);
        var formatResponse = JIRA.Dialogs.issueActions._formatActionsResponse;

        JIRA.Dialogs.issueActions._formatActionsResponse = function (response) {
            if (response && response.operations) {
                response.operations = _.filter(response.operations, function (elem) {
                    if (elem && elem.styleClass) {
                        if (elem.styleClass == cloneStyleClass) {
                            return false;
                        }
                    }
                    return true;
                });
            }
            return formatResponse(response);
        };

    });


})(AJS.$);


