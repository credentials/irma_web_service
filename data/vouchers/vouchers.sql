DROP TABLE IF EXISTS vouchers;
CREATE TABLE vouchers
(
id int unsigned not null auto_increment primary key,
voucher_code varchar(40),
user_id varchar(40)
);
INSERT INTO vouchers VALUES (1, 'AABBCC', NULL);
INSERT INTO vouchers VALUES (2, 'ZYXZYX', NULL);
