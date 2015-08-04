#! /usr/bin/awk -f

BEGIN {
  #Take CURRENCY_LIST from command line
  #CURRENCY_LIST = "USD-EUR-GBP-AUD"
  
  COUNT=0
  EXPECT_NF=0
  RS="\r\n"
  FS=","
  
  CURRENCY_SIZE = split(CURRENCY_LIST, CURRENCY_NAME, "-")
  #printf("CURRENCY_SIZE = %s\n", CURRENCY_SIZE) >"/dev/stderr"
}

function error(MSG) {
  print MSG>"/dev/stderr"
  exit(-1)
}

function findIndex(VALUE, i) {
  for(i = 0; i < NF; i++) {
    if ($i == VALUE) return i
  }
  error(sprintf("UNEXPTECTED VALUE = %s", VALUE))
}

{
  if (COUNT == 0) EXPECT_NF=NF
  COUNT++;
  # Skip first 2 line
  if (COUNT <= 2) next
  # Sanity check -- NF
  if (NF != EXPECT_NF) error(sprintf("UNEXPECTED NF = %d", NF))
  
  # Build CURRENCY_INDEX
  if (COUNT == 3) {
    for(i = 1; i <= CURRENCY_SIZE; i++) {
      NAME = CURRENCY_NAME[i]
      CURRENCY_INDEX[i] = findIndex(NAME)
    }
    LINE="DATE"
    for(i = 1; i <= CURRENCY_SIZE; i++) {
      LINE = LINE "," CURRENCY_NAME[i]
    }
    print LINE
    next
  }
  
  YMD_SIZE = split($1,YMD,"/")
  if (YMD_SIZE != 3) error(sprintf("UNEXPTECTED YMD_SIZE = %s", YMD_SIZE))
  LINE = sprintf("%4d-%02d-%02d", YMD[1], YMD[2], YMD[3])
  for(i = 1; i <= CURRENCY_SIZE; i++) {
    LINE = LINE "," sprintf("%.2f", $CURRENCY_INDEX[i])
  }
  print LINE
}
