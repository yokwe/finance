#! /usr/bin/awk -f

# http://www.xtf.com/ETF-Ratings/SLYV
{
  NAME     = $1
  EXCHANGE = $2
  
  printf("%s/%-11s http://www.xtf.com/ETF-Ratings/%s\n", DIR_OUTPUT, (NAME ".html"), NAME)
}
