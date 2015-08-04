BEGIN {
  FS = ","
  OFS = ","
  TICKER_SIZE = split(TICKER_LIST, TICKER_NAME, "-")
  
  for(i = 1; i <= TICKER_SIZE; i++) {
    NAME = TICKER_NAME[i]
    FILE_IN  = sprintf("tmp/download/google/%s.csv", NAME)
    FILE_OUT = sprintf("tmp/security/google-%s.csv", NAME)
    COUNT = 0
    for(;;) {
      if ((getline <FILE_IN) <= 0) break
      
      COUNT++
      
      // sanity check
      if (NF != 6) unexpect($0)
      if ($6 == "Volume") {
        print "DATE,OPEN,HIGH,LOW,CLOSE,VOLUE" >FILE_OUT
        continue
      }
      
      CMD = "date -d '" $1 "' '+%Y-%m-%d'"
      CMD | getline YYYYMMDD
      close(CMD)
      
      $1 = YYYYMMDD
      print >FILE_OUT
    }
    
    close(FILE_IN)
    close(FILE_OUT)
    
    printf("%-8s %5d\n", NAME, COUNT)
  }
}


function error(MSG) {
  print MSG>"/dev/stderr"
  exit(-1)
}

function unexpect(VAL) {
  error("Unexpet " VAL "!")
}