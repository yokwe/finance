#! /usr/bin/awk -f

# http://www.etf.com/SPY
{
  EXCHANGE = $1
  ETF      = $2
  CATEGORY = $3
  SIZE     = $4
  SYMBOL   = $5
  
  if (ETF != "Y") next

  printf("%s/%-11s http://www.etf.com/%s\n", DIR_OUTPUT, (SYMBOL ".html"), SYMBOL)
}
