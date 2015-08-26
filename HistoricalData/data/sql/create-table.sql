--
-- create-table.sql
--
CREATE TABLE etf_info (
  symbol           TEXT     NOT NULL, -- ticker symbol
  listed_exchange  TEXT     NOT NULL, -- name of exchange NYSEMKT NYSE NYSEARCA NASDAQ BATS
  name             TEXT     NOT NULL, -- name
  category         TEXT     NOT NULL, -- category
  summary          TEXT     NOT NULL, -- summary
  inception_date   TEXT     NOT NULL, -- inception date as YYYY-MM-DD
  expense_ratio    INTEGER  NOT NULL  -- in 100th of percent  12.34% = 123
  dividend-frequency 
);

CREATE TABLE etf_data_daily (
  symbol    TEXT     NOT NULL, -- ticker symbol
  date      TEXT     NOT NULL, -- date is YYYY-MM-DD
  open      INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  high      INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  low       INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  close     INTEGER  NOT NULL, -- in 100th of value  123.45 = 12345
  volume    INTEGER  NOT NULL
);

BEGIN{X["A"]="NYSEMKT";X["N"]="NYSE";X["P"]="NYSEARCA";X["Q"]="NASDAQ";X["Z"]="BATS"