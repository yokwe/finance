--
-- table-drop.sql
--

.echo ON
.open tmp/sqlite/securities.sqlite3

DROP TABLE IF EXISTS nasdaq;
DROP TABLE IF EXISTS etf;
DROP TABLE IF EXISTS yahoo_daily;
DROP TABLE IF EXISTS yahoo_dividend;

VACCUM;