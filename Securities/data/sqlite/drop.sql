--
-- etf-drop.sql
--

.echo ON
.open tmp/sqlite/securities.sqlite3

DROP TABLE IF EXISTS etf_etf;
DROP TABLE IF EXISTS etf_yahoo_daily;
DROP TABLE IF EXISTS etf_yahoo_dividend;

VACCUM;