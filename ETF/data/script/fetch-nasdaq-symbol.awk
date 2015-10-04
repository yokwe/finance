#! /usr/bin/awk -f

# http://www.nasdaq.com/symbol/spy
{
  NAME     = $1
  EXCHANGE = $2
  
  printf("%s/%-11s http://www.nasdaq.com/symbol/%s\n", DIR_OUTPUT, (NAME ".html"), tolower(NAME))
}
