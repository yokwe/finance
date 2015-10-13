--
-- stock-drop.sql
--

.echo ON
.open tmp/sqlite/stock.sqlite3

DROP TABLE IF EXISTS stock_yahoo_daily;
DROP TABLE IF EXISTS stock_yahoo_dividend;

VACCUM;