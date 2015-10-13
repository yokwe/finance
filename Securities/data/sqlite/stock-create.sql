--
-- stock-create.sql
--

.read data/sqlite/stock-drop.sql

.echo ON
.open tmp/sqlite/stock.sqlite3

CREATE TABLE stock_yahoo_daily (
  symbol    TEXT     NOT NULL, -- ticker symbol
  date      TEXT     NOT NULL, -- date is YYYY-MM-DD
  open      REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  high      REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  low       REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  close     REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  volume    INTEGER  NOT NULL
);

CREATE TABLE stock_yahoo_dividend (
  symbol    TEXT  NOT NULL, -- ticker symbol
  date      TEXT  NOT NULL, -- date is YYYY-MM-DD
  dividend  REAL  NOT NULL  -- in 1000th of value 1.234 = 1.234
);

.tables

.separator ,

.import tmp/sqlite/stock-yahoo-daily.csv    stock_yahoo_daily
.import tmp/sqlite/stock-yahoo-dividend.csv stock_yahoo_dividend

CREATE UNIQUE INDEX stock_yahoo_daily_symbol_date    ON stock_yahoo_daily(symbol, date);
CREATE UNIQUE INDEX stock_yahoo_dividend_symbol_date ON stock_yahoo_dividend(symbol, date);

select count(*) from stock_yahoo_daily;
select count(*) from stock_yahoo_dividend;
select date, count(*) from stock_yahoo_daily group by date order by date desc limit 5;