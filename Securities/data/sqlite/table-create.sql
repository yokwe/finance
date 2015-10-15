--
-- etf-create.sql
--

.read data/sqlite/etf-drop.sql

.echo ON
.open tmp/sqlite/etf.sqlite3

-- ETF.Field SYMBOL, NAME, INCEPTION_DATE, EXPENSE_RATIO, ISSUER, HOME_PAGE, AUM, ADV, ASP, PRICE, SOCRE, FIT, SEGMENT, NEXT_EX_DIVIDEND_DATE, DISTRIBUTION_YIELD
CREATE TABLE etf (
  symbol           TEXT     NOT NULL, -- ticker symbol
  name             TEXT     NOT NULL, -- name of ETF
  inception_date   TEXT     NOT NULL, -- inception date as YYYY-MM-DD
  expense_ratio    REAL     NOT NULL, -- in 100th of percent  12.34% = 123
  issuer           TEXT     NOT NULL, -- name of issuer of ETF
  home_page        TEXT     NOT NULL, -- URL of ETF
  aum              INTEGER  NOT NULL, -- asset under management
  adv              INTEGER  NOT NULL, -- average daily volume
  asp              REAL     NOT NULL, -- average spread percent
  price            REAL     NOT NULL, -- price
  score            TEXT     NOT NULL, -- overall score
  fit              INTEGER  NOT NULL, -- rating of fit
  segment          TEXT     NOT NULL, -- segment (category)
  next_ex_dividend TEXT     NOT NULL, -- next ex-dividend date
  distribution_yield REAL   NOT NULL  -- distribution yield (of last 12 month)
);

CREATE TABLE yahoo_daily (
  symbol    TEXT     NOT NULL, -- ticker symbol
  date      TEXT     NOT NULL, -- date is YYYY-MM-DD
  open      REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  high      REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  low       REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  close     REAL     NOT NULL, -- in 100th of value  123.45 = 123.45
  volume    INTEGER  NOT NULL
);

CREATE TABLE yahoo_dividend (
  symbol    TEXT  NOT NULL, -- ticker symbol
  date      TEXT  NOT NULL, -- date is YYYY-MM-DD
  dividend  REAL  NOT NULL  -- in 1000th of value 1.234 = 1.234
);

.tables

.separator ,

.import tmp/sqlite/etf.csv            etf
.import tmp/sqlite/yahoo-daily.csv    yahoo_daily
.import tmp/sqlite/yahoo-dividend.csv yahoo_dividend

CREATE UNIQUE INDEX etf_symbol                 ON etf(symbol);
CREATE UNIQUE INDEX yahoo_daily_symbol_date    ON yahoo_daily(symbol, date);
CREATE UNIQUE INDEX yahoo_dividend_symbol_date ON yahoo_dividend(symbol, date);

select count(*) from etf;
select count(*) from yahoo_daily;
select count(*) from yahoo_dividend;
select date, count(*) from yahoo_daily group by date order by date desc limit 5;