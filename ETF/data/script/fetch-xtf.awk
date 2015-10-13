#! /usr/bin/awk -f

# http://www.xtf.com/ETF-Ratings/SLYV
{
  EXCHANGE = $1
  ETF      = $2
  CATEGORY = $3
  SIZE     = $4
  SYMBOL   = $5
  
  printf("%s/%-11s http://www.xtf.com/ETF-Ratings/%s\n", DIR_OUTPUT, (SYMBOL ".html"), SYMBOL)
}
