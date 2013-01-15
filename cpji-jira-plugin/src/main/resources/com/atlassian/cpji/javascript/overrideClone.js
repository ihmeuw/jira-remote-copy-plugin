AJS.$(function(){
    function unassignCloneEvents(){
        if(JIRA && JIRA.Dialogs && JIRA.Dialogs.cloneIssue){
            JIRA.Dialogs.cloneIssue._unassignEvents("trigger", "a.issueaction-clone-issue");
        } else {
            return false;
        }
    }

    if(!unassignCloneEvents()){
        setTimeout(unassignCloneEvents, 0);
    }
});