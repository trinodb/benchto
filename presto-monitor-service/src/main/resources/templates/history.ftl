<!doctype html>
<html ng-app="benchmarkServiceUI" xmlns="http://www.w3.org/1999/html">
<head>
    <meta charset="utf-8">
    <title>Presto monitor service</title>

    <script src="/js/lib/jquery.js"></script>
    <script src="/js/lib/jquery-ui.js"></script>

    <link rel="stylesheet" href="/css/monitor-service.css">

    <link rel="stylesheet" href="/css/lib/jquery-ui.css">
    <link rel="stylesheet" href="/css/lib/jquery-ui.structure.css">
    <link rel="stylesheet" href="/css/lib/jquery-ui.theme.css">
</head>
<body>

<div id="header">
  <p>
    <label for="timestamp">Date:</label>
    <input type="text" id="timestamp" readonly style="border:0; color:#f6931f; font-weight:bold;">
  </p>

  <div id="timestamp-slider"></div>
</div>
<iframe id="snapshot-page" src="${pageUrl}?snapshotId=${firstDocument.snapshotId}"></iframe>

</body>
<script>
  $(function() {
    $("#timestamp-slider").slider({
      range: "min",
      min: new Date("${firstDocument.documentTimestamp}").getTime(),
      max: new Date("${lastDocument.documentTimestamp}").getTime(),
      value: 60,
      slide: function(event, ui) {
        $("#timestamp").val(new Date(ui.value).toISOString());
      },
      change: function(event, ui) {
        if (event.originalEvent) {
          reloadSnapshotPage();
        }
      }
    });
    $("#timestamp").val(getSliderDate().toISOString());
  });

  function reloadSnapshotPage() {
    $.ajax("/version/${environment}/" + getSliderDate().toISOString() + "${documentName}")
    .done(function (data) {
      var currentUrl = $("#snapshot-page").get(0).contentWindow.location.href
      $("#snapshot-page").attr('src', replaceUrlParam(currentUrl, 'snapshotId', data.snapshotId))
    })
    .fail(function () {
    })
  }

  $('#snapshot-page').load(function(){
    var url = $("#snapshot-page").get(0).contentWindow.location.href
    $.ajax("/timestamp/snapshot/" + getSnapshotId(url))
    .done(function (data) {
      $("#timestamp-slider").slider("value", new Date(data).getTime());
    })
  });

  function getSnapshotId(url) {
    var url = $("#snapshot-page").get(0).contentWindow.location.href
    var exp = /.*snapshotId=(\d+).*/g;
    var match = exp.exec(url);
    return match[1];
  }

  function getSliderDate() {
    return new Date($("#timestamp-slider").slider("value"))
  }

  function replaceUrlParam(url, paramName, paramValue){
      var pattern = new RegExp('('+paramName+'=).*?(&|$)')
      var newUrl=url
      if(url.search(pattern)>=0){
          newUrl = url.replace(pattern,'$1' + paramValue + '$2');
      }
      else{
          newUrl = newUrl + (newUrl.indexOf('?')>0 ? '&' : '?') + paramName + '=' + paramValue
      }
      return newUrl
  }

  $(function() {
      $(window).resize(function() {
          $('#snapshot-page').height($(window).height() - $('#snapshot-page').offset().top - 20);
      });
      $(window).resize();
  });
</script>
</html>