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
  if (index(NAME, ",") != 0) NAME = "\"" NAME "\""
  
  #
  # symbol conversion from nasdaq to yahoo and google
  #
  # NASDAQ                                NASDAQ   YAHOO     GOOGLE
  # X     X                               IBM      IBM       IBM
  # X=    X units                         GRP=     GRP-U     GRP.UN
  # X-    X preferred                     AA-      AA-P      AA-
  # X*    X called                        ARY*     ARY-CL    ARY.CL
  # X#    X when issued                   HPE#     HPE-WI    HPE*
  # X+    X warrants                      AIG+     AIG-WT    ???
  # X-A   X preferred class A             ABR-A    ABR-PA    ABR-A
  # X-A*  X preferred class A called      BIR-A*   BIR-PA.A  BIR-A.CL
  # X.A   X class A                       AKO.A    AKO-A     AKO.A
  # X+A   X warrants class A              GM+A     GM-WTA    ???

  if (TRADED == "Y" && STATUS == "N" && TEST == "N") {
    printf("%s,%s,%s,%s\n", ETF, EXCH_NAME[EXCH], NASDAQ_SYMBOL, NAME)
  }
}