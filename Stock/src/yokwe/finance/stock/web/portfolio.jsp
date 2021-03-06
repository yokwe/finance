<html>
<head>
<title>price</title>
<script src="plotly.min.js"></script>
<%@ page
  import="org.slf4j.Logger"
  import="org.slf4j.LoggerFactory"
  import="java.util.Map"
  import="yokwe.finance.stock.data.Portfolio"
  import="yokwe.finance.stock.data.StockHistoryUtil"
%>

<%!
private static final Logger logger = LoggerFactory.getLogger("price");

private static final String BROKER_FIRSTRADE = "firstrade";
private static final String BROKER_MONEX     = "monex";

private Map<String, Portfolio> portfolioMap;

public void jspInit() {
    logger.info("jspInit XX");
    
    ServletContext servletContext = getServletConfig().getServletContext();
    
    String pathBase = servletContext.getInitParameter("path.base");
    logger.info("pathBase {}", pathBase);
    
    Portfolio firstrade = new Portfolio(pathBase, StockHistoryUtil.PATH_STOCK_HISTORY_FIRSTRADE);
    Portfolio monex     = new Portfolio(pathBase, StockHistoryUtil.PATH_STOCK_HISTORY_MONEX);
    
    logger.info("firstrade {}", firstrade.getActiveMap());
    logger.info("monex     {}", monex.getActiveMap());

    portfolioMap.put(BROKER_FIRSTRADE, firstrade);
    portfolioMap.put(BROKER_MONEX,     monex);
}

public void jspDestory() {
    logger.info("jspDestory XX");
}

%>

<script type="text/javascript">
    var symbol = "IBM";

    var url = "price?filter=date,close&symbol=" + symbol;

    var dataTemplate = {
	type : 'scatter',
	mode : 'lines',
	line : {
	    shape : 'linear'
	},
    }

    var layout = {
	dragmode : 'zoom',
	margin : {
	    r : 10,
	    t : 25,
	    b : 40,
	    l : 60
	},
	showlegend : false,
	xaxis : {
	    autorange : true,
	    domain : [ 0, 1 ],
	    title : 'Date',
	    type : 'date',
	    tickformat : '%y-%m-%d'
	},
	yaxis : {
	    autorange : true,
	    domain : [ 0.2, 0.8 ],
	    type : 'linear'
	}
    };

    function myOnLoad() {
	Plotly.d3.json(url, jsonProcessor);
    }

    function jsonProcessor(jsonError, jsonData) {
	if (jsonError)
	    return console.warn(jsonError);

	var data = Object.assign(dataTemplate, jsonData[symbol]);
	data.x = data.date
	delete data.date
	data.y = data.close
	delete data.close

	Plotly.plot('div1', [ data ], layout)
    }

    window.onload = myOnLoad
</script>
</head>
<body>
    <div id="div1"></div>
</body>
</html>
