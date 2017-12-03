#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
  
  for (i = 0; i <= 255; i++) {
	ord[sprintf("%c", i)] = i
  }
}

function escape(str, c, len, res) {
    len = length(str)
    res = ""
    for (i = 1; i <= len; i++) {
	c = substr(str, i, 1);
	if (c ~ /[0-9A-Za-z]/)
	    res = res c
	else
	    res = res "%" sprintf("%02X", ord[c])
    }
    return res
}

# http://www.quantumonline.com/search.cfm?tickersymbol=NRF-A&sopt=symbol
{
# symbol,rate,name,price,pricec,sd,hv,rsi,divc,vol5,vol30
  SYMBOL = $1
  
  if (SYMBOL=="symbol") next;
  
  escapedSymbol = escape(SYMBOL)
  printf("%s/%-12s http://www.quantumonline.com/search.cfm?sopt=symbol&tickersymbol=%s\n",
    DIR_OUTPUT, (SYMBOL ".html"), escapedSymbol)
}
