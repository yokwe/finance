--
-- table-correlation.sql
--

-- .read data/sqlite/table-drop.sql

.echo ON
.open tmp/sqlite/securities.sqlite3

DROP TABLE IF EXISTS cc03;
DROP TABLE IF EXISTS cc12;
DROP TABLE IF EXISTS cc36;
DROP TABLE IF EXISTS cc60;

CREATE TABLE cc03 (
  a   TEXT  NOT NULL, -- ticker symbol
  b   TEXT  NOT NULL, -- ticker symbol
  cc  REAL  NOT NULL  -- correlation coefficiency
);

CREATE TABLE cc12 (
  a   TEXT  NOT NULL, -- ticker symbol
  b   TEXT  NOT NULL, -- ticker symbol
  cc  REAL  NOT NULL  -- correlation coefficiency
);

CREATE TABLE cc36 (
  a   TEXT  NOT NULL, -- ticker symbol
  b   TEXT  NOT NULL, -- ticker symbol
  cc  REAL  NOT NULL  -- correlation coefficiency
);

CREATE TABLE cc60 (
  a   TEXT  NOT NULL, -- ticker symbol
  b   TEXT  NOT NULL, -- ticker symbol
  cc  REAL  NOT NULL  -- correlation coefficiency
);


.tables

.separator ,

.import tmp/database/correlation-03.csv   cc03
.import tmp/database/correlation-12.csv   cc12
.import tmp/database/correlation-36.csv   cc36
.import tmp/database/correlation-60.csv   cc60

CREATE        INDEX cc03_a               ON cc03(a);
CREATE        INDEX cc03_b               ON cc03(b);
CREATE        INDEX cc03_cc              ON cc03(cc);

CREATE        INDEX cc12_a               ON cc12(a);
CREATE        INDEX cc12_b               ON cc12(b);
CREATE        INDEX cc12_cc              ON cc12(cc);

CREATE        INDEX cc36_a               ON cc36(a);
CREATE        INDEX cc36_b               ON cc36(b);
CREATE        INDEX cc36_cc              ON cc36(cc);

CREATE        INDEX cc60_a               ON cc60(a);
CREATE        INDEX cc60_b               ON cc60(b);
CREATE        INDEX cc60_cc              ON cc60(cc);

select count(*) from cc03;
select count(distinct a) from cc03;

select count(*) from cc12;
select count(distinct a) from cc12;

select count(*) from cc36;
select count(distinct a) from cc36;

select count(*) from cc60;
select count(distinct a) from cc60;
