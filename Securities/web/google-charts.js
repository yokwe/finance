google.charts.load('45', {'packages':['timeline', 'controls']}); // Use version 45 of google charts
google.charts.setOnLoadCallback(onLoad);

function onLoad() {
  $.ajax({url: "./charts.do", dataType: "json"})
  .done(function(jsonData) {drawChart(jsonData)})
}

function drawChart(jsonData) {
  var dataTable = new google.visualization.DataTable();
  var dataTable = new google.visualization.DataTable(jsonData);

  // Create dashboard
  var dashboard = new google.visualization.Dashboard(document.getElementById('dashboard'));

  // Create DateRange
  var dateRangeSlider = new google.visualization.ControlWrapper({
    'controlType': 'DateRangeFilter',
    'containerId': 'control',
    'options': {
      'filterColumnIndex': 4, // 3 means begin of period and 4 means end of period
      'ui': {
        'format': { 'pattern' : 'M/d' },
        'step': 'day',
        'label' : 'DateRange',
      }
    },
//    'state': { 'lowValue' : lowValue, 'highValue' : highValue },
   });

  // Create Line chart
  var chart = new google.visualization.ChartWrapper({
    'chartType': 'Line',
    'containerId': 'chart',
     'options': {
      'width':  '100%', // 800
      'height': 300,
      'hAxis': {
        'format': 'M/d',
      },
      'tooltip': {'isHtml': true},
    }
  });

  dashboard.bind(dateRangeSlider, chart);
  dashboard.draw(dataTable);
}
