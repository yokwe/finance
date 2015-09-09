--
-- etf-create.sql
--

.read data/sql/etf-drop.sql

.echo ON
.open tmp/sqlite/etf.sqlite3

-- ETF.Field SYMBOL, NAME, INCEPTION_DATE, EXPENSE_RATIO, ISSUER, HOME_PAGE, AUM, INDEX_TRACKED,
CREATE TABLE etf_etf (
  symbol           TEXT     NOT NULL, -- ticker symbol
  name             TEXT     NOT NULL, -- name of ETF
  inception_date   TEXT     NOT NULL, -- inception date as YYYY-MM-DD
  expense_ratio    INTEGER  NOT NULL, -- in 100th of percent  12.34% = 123
  issuer           TEXT     NOT NULL, -- name of issuer of ETF
  home_page        TEXT     NOT NULL, -- URL of ETF
  aum              INTEGER  NOT NULL, -- asset under management
  index_tracked    TEXT     NOT NULL  -- relevant index of ETF
);

-- YahooProfile.Field SYMBOL, NAME, CATEGORY, FAMILY, NET_ASSETS, INCEPTION_DATE, EXPENSE_RATIO
CREATE TABLE etf_yahoo_profile (
  symbol           TEXT     NOT NULL, -- ticker symbol
  name             TEXT     NOT NULL, -- name of ETF
  category         TEXT     NOT NULL, -- category of ETF
  family           TEXT     NOT NULL, -- family of ETF
  net_assets       INTEGER  NOT NULL, -- net assets of ETF
  inception_date   TEXT     NOT NULL, -- inception date as YYYY-MM-DD
  expense_ratio    INTEGER  NOT NULL  -- in 100th of percent  12.34% = 123
);

CREATE TABLE etf_yahoo_daily (
  symbol    TEXT     NOT NULL, -- ticker symbol
  date      TEXT     NOT NULL, -- date is YYYY-MM-DD
  open      INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  high      INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  low       INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  close     INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  volume    INTEGER  NOT NULL
);

CREATE TABLE etf_yahoo_dividend (
  symbol    TEXT     NOT NULL, -- ticker symbol
  date      TEXT     NOT NULL, -- date is YYYY-MM-DD
  dividend  INTEGER  NOT NULL  -- in 1000th of value 1.234 = 1234
);

.tables

.separator ,

.import tmp/sqlite/etf-etf.csv etf_etf
.import tmp/sqlite/etf-yahoo-profile.csv  etf_yahoo_profile
.import tmp/sqlite/etf-yahoo-daily.csv    etf_yahoo_daily
.import tmp/sqlite/etf-yahoo-dividend.csv etf_yahoo_dividend

CREATE UNIQUE INDEX etf_etf_symbol                 ON etf_etf(symbol);
CREATE UNIQUE INDEX etf_yahoo_profile_symbol       ON etf_yahoo_profile(symbol);
CREATE UNIQUE INDEX etf_yahoo_daily_symbol_date    ON etf_yahoo_daily(symbol, date);
CREATE UNIQUE INDEX etf_yahoo_dividend_symbol_date ON etf_yahoo_dividend(symbol, date);
