--
-- table-correlation.sql
--

-- .read data/sqlite/table-drop.sql

.echo ON
.open tmp/sqlite/securities.sqlite3

DROP TABLE IF EXISTS correlation;

CREATE TABLE correlation (
  month INTEGER NOT NULL, -- month
  a     TEXT    NOT NULL, -- ticker symbol
  b     TEXT    NOT NULL, -- ticker symbol
  cc    REAL    NOT NULL  -- correlation coefficiency
);

.tables

.separator ,

.import tmp/database/correlation-03.csv   correlation
.import tmp/database/correlation-12.csv   correlation
.import tmp/database/correlation-36.csv   correlation
.import tmp/database/correlation-60.csv   correlation

CREATE        INDEX correlation_month           ON correlation(month);
CREATE        INDEX correlation_a               ON correlation(a);
CREATE        INDEX correlation_b               ON correlation(b);
CREATE        INDEX correlation_cc              ON correlation(cc);

select count(*)              from correlation;
select count(distinct month) from correlation;
select count(distinct a)     from correlation;
select count(distinct b)     from correlation;

select month, count(*)       from correlation group by month;
