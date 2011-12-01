AJS.toInit(function() {
    (function($) {
     $('#select-issue-type').change(function() {
          $('.cpji-loading').show();
          $('#fields').empty();
          var projectKey = $('#project-key').val();
          var issueType = $('#select-issue-type :selected').val();
          $.get('../GetFieldsHtmlAction.jspa?projectKey=' + projectKey + '&selectedIssueTypeId=' + issueType, function(data) {
            $('#fields').append($(data));
            $('.cpji-loading').hide();
          });
     });
     $('#select-issue-type').trigger('change');
    })(AJS.$);
});