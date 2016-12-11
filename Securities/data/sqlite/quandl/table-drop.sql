--
-- table-drop.sql
--

.echo ON
.open tmp/sqlite/quandl.sqlite3

DROP TABLE IF EXISTS databases;

VACCUM;