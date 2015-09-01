#! /usr/bin/awk -f

BEGIN {
  CMD_SLEEP = "sleep 0.5 0.$((RANDOM%10))"
}

# http://www.etf.com/SPY
{
  NAME     = $1
  EXCHANGE = $2
  
  printf("%s; wget -nv -O %s/%s.html 'http://www.etf.com/%s'\n",
    CMD_SLEEP, DIR_OUTPUT, NAME, NAME)
}
