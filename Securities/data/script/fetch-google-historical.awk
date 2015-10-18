#! /usr/bin/awk -f

# http://www.google.com/finance/historical?q=NYSE:IBM&&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv

BEGIN {
  FS = ","
  RS = "[\r\n]+"
  
  "date '+%Y'" | getline Y
}

{
  ETF      = $1
  EXCH     = $2
  SYMBOL   = $3

  GOOGLE_SYMBOL = SYMBOL
# gsub(/\-/, ".PR", GOOGLE_SYMBOL) # Treat "-" as "-"
# gsub(/\+/, ".WS", GOOGLE_SYMBOL) # ???
  gsub(/\*/, ".CL", GOOGLE_SYMBOL) # ARY*  =>  ARY.CL
  gsub(/\=/, ".UN", GOOGLE_SYMBOL) # GRP=  =>  GRP.UN

  printf("%s/%-11s http://www.google.com/finance/historical?q=%s&startdate=Jan%%201,%%202000&enddate=Dec%%2031,%s&output=csv\n",
    DIR_OUTPUT, (SYMBOL ".csv"), (EXCH ":" GOOGLE_SYMBOL), Y)
}
