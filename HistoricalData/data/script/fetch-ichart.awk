#! /usr/bin/awk -f

BEGIN {
  A = "00"
  B = "01"
  C = "2000"
  D = "30"
  E = "12"
  F = "2049"
}

# http://ichart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
{
  NAME     = $1
  EXCHANGE = $2
  printf("sleep 0.5 %s; wget -nv -O tmp/fetch/ichart/%s.csv 'http://ichart.finance.yahoo.com/table.csv?s=%s&a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&ignore=.csv'\n",
    "0.$((RANDOM%100))", NAME, NAME, A, B, C, D, E, F)
}
