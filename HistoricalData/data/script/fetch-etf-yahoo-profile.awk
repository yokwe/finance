#! /usr/bin/awk -f

# http://finance.yahoo.com/q/pr?s=SPY
{
  NAME=$1
  EXCHANGE = $2
  
  printf("%s/%-11s http://finance.yahoo.com/q/pr?s=%s\n", DIR_OUTPUT, (NAME ".html"), NAME)
}
