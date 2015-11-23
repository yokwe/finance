--
-- table-dividend.sql
--

.echo ON
.open tmp/sqlite/securities.sqlite3

pragma page_size = 4096;
pragma journal_mode = wal;

select time('now', 'localtime');
DELETE FROM dividend;

.separator ,

.separator ,
select time('now', 'localtime');

-- .import tmp/database/yahoo-dividend.csv   dividend
.import tmp/database/dividend-all.csv     dividend
select time('now', 'localtime');

select count(*) from dividend;
select time('now', 'localtime');

select * from dividend order by date desc limit 5;

