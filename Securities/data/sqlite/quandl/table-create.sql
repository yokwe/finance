--
-- table-create.sql
--

-- .read data/sqlite/quand/table-drop.sql

.echo ON
.open tmp/sqlite/quandl.sqlite3

pragma page_size = 4096;
pragma journal_mode = wal;

CREATE TABLE databases (
  id             INTEGER  NOT NULL, -- id of database
  name           TEXT     NOT NULL, -- name of database
  database_code  TEXT     NOT NULL, -- database code
  description    TEXT     NOT NULL, -- description of database
  datasets_count INTEGER  NOT NULL, -- number of ??
  downloads      INTEGER  NOT NULL, -- number of ??
  premium        TEXT     NOT NULL, -- is this premium contents?
  image          TEXT     NOT NULL, -- url of image file
  favorite       TEXT     NOT NULL, -- ??
  url_name       TEXT     NOT NULL  -- name of ??
);

.tables

.separator ,
select time('now', 'localtime');

.import tmp/database/quand-databases.csv           databases
select time('now', 'localtime');