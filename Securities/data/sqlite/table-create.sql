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
  name             TEXT     NOT NULL  -- name of securities
);

CREATE TABLE price (
  date      TEXT     NOT NULL, -- date is YYYY-MM-DD
  symbol    TEXT     NOT NULL, -- ticker symbol
  open      REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  high      REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  low       REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
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

.import tmp/sqlite/nasdaq.csv         nasdaq
.import tmp/sqlite/yahoo-dividend.csv dividend
.import tmp/sqlite/google-prices.csv  price

CREATE UNIQUE INDEX nasdaq_etf           ON nasdaq(etf);
CREATE UNIQUE INDEX nasdaq_symbol        ON nasdaq(symbol);

CREATE        INDEX dividend_date        ON dividend(date);
CREATE        INDEX dividend_symbol      ON dividend(symbol);
CREATE UNIQUE INDEX dividend_symbol_date ON dividend(symbol, date);

CREATE        INDEX price_date           ON price(date);
CREATE        INDEX price_symbol         ON price(symbol);
CREATE UNIQUE INDEX price_symbol_date    ON price(symbol, date);

select count(*) from price;
select count(*) from dividend;
select date, count(*) from price group by date order by date desc limit 5;