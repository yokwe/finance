--
-- etf-drop.sql
--

.echo ON
.open tmp/sqlite/etf.sqlite3

DROP INDEX IF EXISTS etf_etf_symbol;
DROP INDEX IF EXISTS etf_yahoo_profile_symbol;
DROP INDEX IF EXISTS etf_yahoo_daily_symbol_date;
DROP INDEX IF EXISTS etf_yahoo_dividend_symbol_date;

DROP TABLE IF EXISTS etf_etf;
DROP TABLE IF EXISTS etf_yahoo_profile;
DROP TABLE IF EXISTS etf_yahoo_daily;
DROP TABLE IF EXISTS etf_yahoo_dividend;
