#! /usr/bin/awk -f

/nasdaq_etf_/ {
  pos=match($0,/nasdaq_etf_[0-9]+\.txt</)
  if (pos != 0) {
    NAME=substr($0,RSTART,RLENGTH-1)
    MMDDYYYY=substr(NAME,12,8)
    MM=substr(MMDDYYYY,1,2)
    DD=substr(MMDDYYYY,3,2)
    YYYY=substr(MMDDYYYY,5,4)
    YYYYMMDD = YYYY MM DD
    if (LAST_YYYYMMDD<YYYYMMDD) {
      LAST_YYYYMMDD=YYYYMMDD
      LAST_NAME=NAME
    }
  }
}

END {
  printf("wget -O tmp/fetch/nasdaq/nasdaq-etf.txt ftp://ftp.nasdaqtrader.com/ETFData/%s\n", LAST_NAME)
}