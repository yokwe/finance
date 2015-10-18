#! /usr/bin/awk -f

# http://www.etf.com/SPY

BEGIN {
  FS = ","
  RS = "[\r\n]+"
}

{
  ETF      = $1
  EXCH     = $2
  SYMBOL   = $3
  
  if (ETF == "Y") {
    printf("%s/%-11s http://www.etf.com/%s\n", DIR_OUTPUT, (SYMBOL ".html"), SYMBOL)
  }
}
