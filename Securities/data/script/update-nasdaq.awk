#! /usr/bin/awk -f

# Data format
# Nasdaq Traded|Symbol|Security Name|Listing Exchange|Market Category|ETF|Round Lot Size|Test Issue|Financial Status|CQS Symbol|NASDAQ Symbol

BEGIN {
  FS = "|"
  RS = "[\r\n]+"
  
  EXCH_NAME["A"] = "NYSEMKT"
  EXCH_NAME["N"] = "NYSE"
  EXCH_NAME["P"] = "NYSEARCA"
  EXCH_NAME["Q"] = "NASDAQ"
  EXCH_NAME["Z"] = "BATS"
}

/Security Name/ { next }
/File Creation Time/ { next }

{
  TRADED        = $1
  ACT_SYMBOl    = $2
  NAME          = $3
  EXCH          = $4
  CATEGORY      = $5  # Q - Global Select Market, G - Global Market, S - Capital Market,  - can be blank
  ETF           = $6  # Y - ETF, N - STOCK
  LOT_SIZE      = $7
  TEST          = $8  # Y - test issue, N - not a test issue
  STATUS        = $9
  CQS_SYMBOL    = $10
  NASDAQ_SYMBOL = $11
  
  # See link below for Ticker Symbol Convention of NASDAQ_SYMBOL
  #  http://www.nasdaqtrader.com/trader.aspx?id=CQSsymbolconvention
  # NASDAQ_SYMBOL  BAC-Y should read as BAC-PY  in yahoo finance and BAC.PRY in nasdaq
  # NASDAQ_SYMBOL  BAC+A should read as BAC-WTA in yahoo finance and BAC.WSA in nasdaq
  
  if (CATEGORY == " ") CATEGORY = "-"
   
  if (index(NAME, ",") != 0) NAME = "\"" NAME "\""
  printf("%s,%s,%s,%s\n", ETF, EXCH_NAME[EXCH], NASDAQ_SYMBOL, NAME)
}