#! /usr/bin/awk -f

BEGIN {
  FS=","
  
  TICKER_SIZE = split(TICKER_LIST, TICKER_NAME, "-")
   
  CMD = "date -d '-" TICKER_YEAR " years' '+%Y-%m-%d'"
  CMD | getline START_DATE
  close(CMD)
  
  for(i = 1; i <= TICKER_SIZE; i++) {
    NAME = TICKER_NAME[i]
    CMD = sprintf("wget -O tmp/download/google/%s.csv 'https://www.google.com/finance/historical?q=%s&histperiod=daily&startdate=%s&output=csv'", NAME, NAME, START_DATE)
    print CMD
  }
  
  exit(0)
}
