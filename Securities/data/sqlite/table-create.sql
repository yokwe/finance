--
-- table-create.sql
--

-- .read data/sqlite/table-drop.sql

.echo ON
.open tmp/sqlite/securities.sqlite3

pragma page_size = 4096;
pragma journal_mode = wal;

CREATE TABLE nasdaq (
  etf              TEXT     NOT NULL, -- Y for ETF
  exchange         TEXT     NOT NULL, -- name of exchange
  symbol           TEXT     NOT NULL, -- ticker symbol
  base             TEXT     NOT NULL, -- base of symbol
  yahoo            TEXT     NOT NULL, -- ticker symbol of yahoo
  google           TEXT     NOT NULL, -- ticker symbol of google
  nasdaq           TEXT     NOT NULL, -- ticker symbol of nasdaq
  name             TEXT     NOT NULL  -- name of securities
);

CREATE TABLE company (
  symbol           TEXT     NOT NULL, -- ticker symbol
  marketCap        INTEGER  NOT NULL, -- market capacity
  country          TEXT     NOT NULL, -- country of securities
  sector           TEXT     NOT NULL, -- sector of securities
  industry         TEXT     NOT NULL -- industry of securities
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

-- symbol, cpnRate, annAmt, liqPref, callPrice, callDate, maturDate, type, name
CREATE TABLE quantum (
  symbol    TEXT  NOT NULL, -- ticker symbol
  cpnRate   REAL  NOT NULL, -- coupon rate
  annAmt    REAL  NOT NULL, -- annual dividend amount
  liqPref   REAL  NOT NULL, -- liquidation preference
  callPrice REAL  NOT NULL, -- call price
  callDate  TEXT  NOT NULL, -- call date
  maturDate TEXT  NOT NULL, -- mature date
  type      TEXT  NOT NULL, -- security type
  name      TEXT  NOT NULL  -- security name
);

.tables

.separator ,
select time('now', 'localtime');

.import tmp/database/nasdaq.csv           nasdaq
select time('now', 'localtime');
.import tmp/database/nasdaq-company.csv   company
select time('now', 'localtime');

-- .import tmp/database/yahoo-dividend.csv   dividend
.import tmp/database/dividend-all.csv     dividend
select time('now', 'localtime');

-- .import tmp/database/yahoo-daily.csv      price
-- .import tmp/database/google-getprices.csv price
.import tmp/database/price-all.csv        price
select time('now', 'localtime');

.import tmp/database/quantum.csv          quantum
select time('now', 'localtime');

CREATE        INDEX nasdaq_etf           ON nasdaq(etf);
select time('now', 'localtime');
CREATE UNIQUE INDEX nasdaq_symbol        ON nasdaq(symbol);
select time('now', 'localtime');

CREATE UNIQUE INDEX company_symbol       ON company(symbol);
select time('now', 'localtime');

CREATE        INDEX dividend_date        ON dividend(date);
select time('now', 'localtime');
CREATE        INDEX dividend_symbol      ON dividend(symbol);
select time('now', 'localtime');
CREATE UNIQUE INDEX dividend_symbol_date ON dividend(symbol, date);
select time('now', 'localtime');

CREATE        INDEX price_date           ON price(date);
select time('now', 'localtime');
CREATE        INDEX price_symbol         ON price(symbol);
select time('now', 'localtime');
CREATE UNIQUE INDEX price_symbol_date    ON price(symbol, date);
select time('now', 'localtime');

CREATE        INDEX quantum_symbol       ON quantum(symbol);
CREATE        INDEX quantum_type         ON quantum(type);

select count(*) from nasdaq;
select time('now', 'localtime');
select count(*) from company;
select time('now', 'localtime');
select count(*) from price;
select time('now', 'localtime');
select count(*) from dividend;
select time('now', 'localtime');
select date, count(*) from price group by date order by date desc limit 5;
select time('now', 'localtime');
select * from dividend order by date desc limit 5;
select time('now', 'localtime');

select count(*) from quantum;
select count(*), type from quantum group by type order by type;
