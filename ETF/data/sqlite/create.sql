--
-- etf.sql
--

.read data/sqlite/drop.sql

.echo ON
.open tmp/sqlite/etf.sqlite3

-- ETF.Field SYMBOL, NAME, INCEPTION_DATE, EXPENSE_RATIO, ISSUER, HOME_PAGE, AUM, INDEX_TRACKED,
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
  fit              INTEGER  NOT NULL  -- rating of fit
);

-- YahooProfile.Field SYMBOL, NAME, CATEGORY, FAMILY, NET_ASSETS, INCEPTION_DATE, EXPENSE_RATIO
CREATE TABLE yahoo_profile (
  symbol           TEXT     NOT NULL, -- ticker symbol
  name             TEXT     NOT NULL, -- name of ETF
  category         TEXT     NOT NULL, -- category of ETF
  family           TEXT     NOT NULL, -- family of ETF
  net_assets       INTEGER  NOT NULL, -- net assets of ETF
  inception_date   TEXT     NOT NULL, -- inception date as YYYY-MM-DD
  expense_ratio    REAL     NOT NULL  -- in 100th of percent  12.34% = 12.34
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
.import tmp/sqlite/yahoo-profile.csv  yahoo_profile
.import tmp/sqlite/yahoo-daily.csv    yahoo_daily
.import tmp/sqlite/yahoo-dividend.csv yahoo_dividend

CREATE UNIQUE INDEX etf_symbol                 ON etf(symbol);
CREATE UNIQUE INDEX yahoo_profile_symbol       ON yahoo_profile(symbol);
CREATE UNIQUE INDEX yahoo_daily_symbol_date    ON yahoo_daily(symbol, date);
CREATE UNIQUE INDEX yahoo_dividend_symbol_date ON yahoo_dividend(symbol, date);

select count(*) from etf;
select count(*) from yahoo_profile;
select count(*) from yahoo_daily;
select count(*) from yahoo_dividend;
select date, count(*) from yahoo_daily group by date order by date desc limit 5;