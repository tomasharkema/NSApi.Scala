if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

$(function(){
  var searchBar = $("#search");
  if (searchBar) {
    searchBar.keyup(function (e) {
      console.log("KeyUp", searchBar.val());

      jsRoutes.controllers.Api.search(searchBar.val()).ajax({success: function(res){
        var html = "";

        res.stations.forEach(function(item) {
          html += item.station.name + "<br>"
        });

        $("#results").html(html);
      }})
    });
  }
});