#! /usr/bin/awk -f

BEGIN {
  "date '+%Y'" | getline Y
  
  # Data for at least last 10 years
  
  # From (Y - 11)-01-01
  A = "00"
  B = "01"
  C = "1993" # SPY was started at 1993-01-29
  # To Y-12-31
  D = "30"
  E = "12"
  F = Y
  # G = "d" for daily and "v" for dividend
 }

# http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
{
  NAME     = $1
  EXCHANGE = $2
  
  printf("%s/%-10s http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&g=%s&ignore=.csv\n",
    DIR_OUTPUT, (NAME ".csv"), NAME, A, B, C, D, E, F, G)
}
