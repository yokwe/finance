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
.import tmp/database/price-all.csv        price

-- .import tmp/database/yahoo-daily.csv      price
-- .import tmp/database/google-getprices.csv price

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