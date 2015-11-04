--
-- table-create.sql
--

-- .read data/sqlite/table-drop.sql

.echo ON
.open tmp/sqlite/securities.sqlite3

CREATE TABLE nasdaq (
  etf              TEXT     NOT NULL, -- Y for ETF
  exchange         TEXT     NOT NULL, -- name of exchange
  symbol           TEXT     NOT NULL, -- ticker symbol
  yahoo            TEXT     NOT NULL, -- ticker symbol of yahoo
  google           TEXT     NOT NULL, -- ticker symbol of google
  nasdaq           TEXT     NOT NULL, -- ticker symbol of nasdaq
  name             TEXT     NOT NULL  -- name of securities
);

CREATE TABLE finance (
  symbol           TEXT     NOT NULL, -- ticker symbol
  price            REAL     NOT NULL, -- latest price
  vol              INTEGER  NOT NULL, -- latest day trade volume in share
  avg_vol          INTEGER  NOT NULL, -- 30 days average trade volume in share
  mkt_cap          INTEGER  NOT NULL  -- market capitalization
);

CREATE TABLE price (
  date      TEXT     NOT NULL, -- date is YYYY-MM-DD
  symbol    TEXT     NOT NULL, -- ticker symbol
  close     REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  volume    INTEGER  NOT NULL
);

CREATE TABLE dividend (
  date      TEXT  NOT NULL, -- date is YYYY-MM-DD
  symbol    TEXT  NOT NULL, -- ticker symbol
  dividend  REAL  NOT NULL  -- in 1000th of value 1.234 = 1.234
);

.tables

.separator ,

.import tmp/database/nasdaq.csv           nasdaq
.import tmp/database/google-finance.csv   finance
.import tmp/database/yahoo-dividend.csv   dividend
-- .import tmp/database/yahoo-daily.csv      price
-- .import tmp/database/google-getprices.csv price

.import tmp/database/price-1975.csv       price
.import tmp/database/price-1976.csv       price
.import tmp/database/price-1977.csv       price
.import tmp/database/price-1978.csv       price
.import tmp/database/price-1979.csv       price

.import tmp/database/price-1980.csv       price
.import tmp/database/price-1981.csv       price
.import tmp/database/price-1982.csv       price
.import tmp/database/price-1983.csv       price
.import tmp/database/price-1984.csv       price
.import tmp/database/price-1985.csv       price
.import tmp/database/price-1986.csv       price
.import tmp/database/price-1987.csv       price
.import tmp/database/price-1988.csv       price
.import tmp/database/price-1989.csv       price

.import tmp/database/price-1990.csv       price
.import tmp/database/price-1991.csv       price
.import tmp/database/price-1992.csv       price
.import tmp/database/price-1993.csv       price
.import tmp/database/price-1994.csv       price
.import tmp/database/price-1995.csv       price
.import tmp/database/price-1996.csv       price
.import tmp/database/price-1997.csv       price
.import tmp/database/price-1998.csv       price
.import tmp/database/price-1999.csv       price

.import tmp/database/price-2000.csv       price
.import tmp/database/price-2001.csv       price
.import tmp/database/price-2002.csv       price
.import tmp/database/price-2003.csv       price
.import tmp/database/price-2004.csv       price
.import tmp/database/price-2005.csv       price
.import tmp/database/price-2006.csv       price
.import tmp/database/price-2007.csv       price
.import tmp/database/price-2008.csv       price
.import tmp/database/price-2009.csv       price

.import tmp/database/price-2010.csv       price
.import tmp/database/price-2011.csv       price
.import tmp/database/price-2012.csv       price
.import tmp/database/price-2013.csv       price
.import tmp/database/price-2014.csv       price
.import tmp/database/price-2015.csv       price
.import tmp/database/price-2016.csv       price
.import tmp/database/price-2017.csv       price
.import tmp/database/price-2018.csv       price
.import tmp/database/price-2019.csv       price

CREATE        INDEX nasdaq_etf           ON nasdaq(etf);
CREATE UNIQUE INDEX nasdaq_symbol        ON nasdaq(symbol);

CREATE UNIQUE INDEX finance_symbol       ON finance(symbol);

CREATE        INDEX dividend_date        ON dividend(date);
CREATE        INDEX dividend_symbol      ON dividend(symbol);
CREATE UNIQUE INDEX dividend_symbol_date ON dividend(symbol, date);

CREATE        INDEX price_date           ON price(date);
CREATE        INDEX price_symbol         ON price(symbol);
CREATE UNIQUE INDEX price_symbol_date    ON price(symbol, date);

select count(*) from nasdaq;
select count(*) from finance;
select count(*) from price;
select count(*) from dividend;
select date, count(*) from price group by date order by date desc limit 5;