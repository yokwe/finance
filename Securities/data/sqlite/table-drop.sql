--
-- table-drop.sql
--

.echo ON
.open tmp/sqlite/securities.sqlite3

DROP TABLE IF EXISTS nasdaq;
DROP TABLE IF EXISTS finance;
DROP TABLE IF EXISTS dividend;
DROP TABLE IF EXISTS price;

VACCUM;