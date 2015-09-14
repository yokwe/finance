--
-- etf-drop.sql
--

.echo ON
.open tmp/sqlite/etf.sqlite3

DROP INDEX IF EXISTS etf_symbol;
DROP INDEX IF EXISTS yahoo_profile_symbol;
DROP INDEX IF EXISTS yahoo_daily_symbol_date;
DROP INDEX IF EXISTS yahoo_dividend_symbol_date;

DROP TABLE IF EXISTS etf;
DROP TABLE IF EXISTS yahoo_profile;
DROP TABLE IF EXISTS yahoo_daily;
DROP TABLE IF EXISTS yahoo_dividend;
