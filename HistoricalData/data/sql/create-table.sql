--
-- create-table.sql
--
CREATE TABLE etf_info (
  symbol    TEXT     NOT NULL, -- ticker symbol
  name      TEXT     NOT NULL, -- name
  inception TEXT     NOT NULL, -- inception date as YYYY-MM-DD
  company   TEXT     NOT NULL, -- name of company issue
  category  TEXT     NOT NULL, -- name of category
  expense   INTEGER  NOT NULL  -- in 100th of percent  12.34% = 123
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
