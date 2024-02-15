--.createTable

drop table if exists bu cascade;
create table bu(a int, b text, c jsonb);

--.insertValues

insert into bu(a, b, c) values (1, 'A', '[1]');
insert into bu(a, b, c) values (2, 'B', '[2]');
insert into bu(a, b, c) values (3, 'C', '[3]');

--.insertValuesAndSelect

insert into bu(a, b, c) values (4, 'D', '[4]'), (5, 'E', '[5]');
select * from bu;

--.insertValuesAndSelectSkippingUpdateCount

insert into bu(a, b, c) values (6, 'F', '[6]');
select * from bu where a = 99;

--.multipleSelectFail

select * from bu;
select * from bu;

--.multipleSelectVoid

select * from bu;
select * from bu;

--.selectSingle

select * from bu where b = 'A';

--.selectEmpty

select * from bu where a = 99;

--.selectSingleColumn

select * from bu where b = 'C';

--.selectFirstColumnIgnoreMore

select * from bu;

--.selectColumns

select b from bu;

--.selectJsonbColumn

select * from bu;

--.dropTable

drop table bu;

--.selectConcatSimple

select :a || :b || :c;

--.selectConcatSpread

select :a || :b || :c;

--.selectConcatSpreadPrefix

select :a || :u.b || :u.c || :d;

--.createTableForBatch

drop table if exists chu;
create table chu(a text, b int);

--.dropTableForBatch

drop table chu;

--.insertBatch

insert into chu(a, b) values (:a, :b);

--.insertBatchSpread

insert into chu(a, b) values (:a, :b);

--.selectFromBatch

select a || '-' || b from chu;

--.insertAndGetJsonb

insert into bu(a, b, c) values (999, 'JSONB', :map::jsonb);

select c from bu where a = 999;

--.selectAutoNamingConvention
select 'A' as a_a, 'B' as b_b_b, 3 as c_cc;

--.selectBuRecords
select * from bu;

--.createTypes

drop table if exists types cascade;
create table if not exists types(
  id uuid not null primary key,
  dt timestamptz not null,
	ts timestamp not null,
  jb jsonb not null
);

--.insertTypes

insert into types(id, dt, ts, jb) values (:id, :dt, :ts, :jb::jsonb);

--.readUuid

select id from types;

--.readFancyTypes

select * from types;

--.addNested

select * from bu where a = :a and b = :b;

--.selectNested

select 1 as a, 'B' as b
