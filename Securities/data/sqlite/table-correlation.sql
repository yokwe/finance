--
-- table-correlation.sql
--

.echo ON
.open tmp/sqlite/correlation.sqlite3

pragma page_size = 4096;
pragma journal_mode = wal;

CREATE TABLE cc (
  month INTEGER NOT NULL, -- month
  a     TEXT    NOT NULL, -- ticker symbol
  b     TEXT    NOT NULL, -- ticker symbol
  cc    REAL    NOT NULL  -- correlation coefficiency
);

.tables

.separator ,

select time('now', 'localtime');
.import tmp/database/correlation-01.csv   cc
select time('now', 'localtime');
.import tmp/database/correlation-03.csv   cc
select time('now', 'localtime');
.import tmp/database/correlation-12.csv   cc
select time('now', 'localtime');
.import tmp/database/correlation-36.csv   cc
select time('now', 'localtime');
.import tmp/database/correlation-60.csv   cc
select time('now', 'localtime');

CREATE        INDEX cc_month           ON cc(month);
select time('now', 'localtime');
CREATE        INDEX cc_a               ON cc(a);
select time('now', 'localtime');
CREATE        INDEX cc_b               ON cc(b);
select time('now', 'localtime');
CREATE        INDEX cc_a_b             ON cc(a,b);
select time('now', 'localtime');
CREATE        INDEX cc_cc              ON cc(cc);
select time('now', 'localtime');

select count(*)              from cc;
select time('now', 'localtime');
select count(distinct month) from cc;
select time('now', 'localtime');
select count(distinct a)     from cc;
select time('now', 'localtime');
select count(distinct b)     from cc;
select time('now', 'localtime');

select month, count(*)       from cc group by month;
select time('now', 'localtime');
