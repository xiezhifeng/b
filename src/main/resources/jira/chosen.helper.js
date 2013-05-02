AJS.Editor.JiraConnector.Chosen = {
    getSelectedOptionsInOrder: function(selectElId) {
        var selectedJq = AJS.$("#" + selectElId + " > option");
        var chznSelectedJq = AJS.$("#" + selectElId + "_chzn li.search-choice > a");

        var selectedOptions = [];
        for (var i = 0; i < chznSelectedJq.size(); i++) {
            var rel = chznSelectedJq.get(i).rel;
            selectedOptions.push(selectedJq.get(rel).value);
        }

        return selectedOptions;
    }
}