#! /usr/bin/awk -f

BEGIN {
  Y = strftime("%Y")
  
  # Data for last 10 years
  
  # From (Y - 10)-01-01
  A = "00"
  B = "01"
  C = Y - 10
  # To Y-12-31
  D = "30"
  E = "12"
  F = Y
  
  CMD_SLEEP = "sleep 0.5 0.$((RANDOM%10))"
 }

# http://ichart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
{
  NAME     = $1
  EXCHANGE = $2
  
  printf("%s; wget -nv -O %s/%s.csv 'http://ichart.finance.yahoo.com/table.csv?s=%s&a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&ignore=.csv'\n",
    CMD_SLEEP, DIR_OUTPUT, NAME, NAME, A, B, C, D, E, F)
}
