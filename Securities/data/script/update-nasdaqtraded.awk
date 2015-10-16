#! /usr/bin/awk -f

# Data format
# Nasdaq Traded|Symbol|Security Name|Listing Exchange|Market Category|ETF|Round Lot Size|Test Issue|Financial Status|CQS Symbol|NASDAQ Symbol

BEGIN {
  FS = "|"
  RS = "[\r\n]+"
}

/Security Name/ { next }
/File Creation Time/ { next }

{
  # See link below for defintion of each feild
  #   http://www.nasdaqtrader.com/trader.aspx?id=symboldirdefs
  TRADED        = $1  # Y - trade
  ACT_SYMBOl    = $2
  NAME          = $3
  EXCH          = $4
  CATEGORY      = $5  # Q - Global Select Market, G - Global Market, S - Capital Market,  - can be blank
  ETF           = $6  # Y - ETF, N - STOCK
  LOT_SIZE      = $7
  TEST          = $8  # Y - test issue, N - not a test issue
  STATUS        = $9  # N - Normal Issuer Is NOT Deficient, Delinquent, or Bankrupt
  CQS_SYMBOL    = $10
  NASDAQ_SYMBOL = $11
  
  # See link below for Ticker Symbol Convention of NASDAQ_SYMBOL
  #  http://www.nasdaqtrader.com/trader.aspx?id=CQSsymbolconvention
  # NASDAQ_SYMBOL  BAC-Y should read as BAC-PY  in yahoo finance and BAC.PRY in nasdaq
  # NASDAQ_SYMBOL  BAC+A should read as BAC-WTA in yahoo finance and BAC.WSA in nasdaq
  
  if (STATUS == "") STATUS = "N"
  
  if (CATEGORY == " ") CATEGORY = "-"
  
  if (TRADED == "Y" && STATUS == "N" && TEST == "N") {
#   printf("%s %s %3d %-8s %s\n", EXCH, ETF, LOT_SIZE, NASDAQ_SYMBOL, NAME)
    printf("%s %s %s %3d %-8s %s\n", EXCH, ETF, CATEGORY, LOT_SIZE, NASDAQ_SYMBOL, NAME)
  }
}