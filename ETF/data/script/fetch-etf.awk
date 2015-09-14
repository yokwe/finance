#! /usr/bin/awk -f

# http://www.etf.com/SPY
{
  NAME     = $1
  EXCHANGE = $2
  
  printf("%s/%-11s http://www.etf.com/%s\n", DIR_OUTPUT, (NAME ".html"), NAME)
}
