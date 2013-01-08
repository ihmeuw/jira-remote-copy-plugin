AJS.$(function () {
    AJS.$(".singleSelect").each(function(){
        new AJS.SingleSelect({
            element: AJS.$(this),
            itemAttrDisplayed: "label",
            width: "250px",
            revertOnInvalid: true
        })
    });
});