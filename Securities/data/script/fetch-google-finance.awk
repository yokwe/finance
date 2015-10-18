#! /usr/bin/awk -f

# http://www.google.com/finance?q=NASDAQ:AMZN

BEGIN {
  FS = ","
  RS = "[\r\n]+"
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

  printf("%s/%-11s http://www.google.com/finance?q=%s\n",
    DIR_OUTPUT, (SYMBOL ".html"), (EXCH ":" GOOGLE_SYMBOL))
}
