#! /usr/bin/awk -f

# http://www.google.com/finance/getprices?q=IBM&x=NYSE&i=86400&p=15Y&f=d,c,v,o,h,l

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

  printf("%s/%-11s http://www.google.com/finance/getprices?q=%s&x=%s&i=86400&p=%dY&f=d,c,v,o,h,l\n",
    DIR_OUTPUT, (SYMBOL ".csv"), GOOGLE_SYMBOL, EXCH, (Y - 1999))
}
