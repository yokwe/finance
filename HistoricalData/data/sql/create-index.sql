--
-- create-index.sql
--
CREATE UNIQUE INDEX etf_info_symbol ON etf_info(symbol);
CREATE UNIQUE INDEX etf_data_daily_symbol_date ON etf_data_daily(symbol, date);
