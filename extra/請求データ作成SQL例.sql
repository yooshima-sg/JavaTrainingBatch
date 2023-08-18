-- 請求データ作成用のSELECT文
INSERT INTO T_BILLING_DATA (
  billing_ym,
  member_id,
  mail,
  name,
  address,
  start_date,
  end_date,
  payment_method,
  amount,
  tax_ratio,
  total)
SELECT 
  billing_ym,
  member_id,
  mail,
  name,
  address,
  start_date,
  end_date,
  payment_method,
  sum_amount,
  tax_ratio,
  sum_amount * (1 + tax_ratio) as total
FROM (
  SELECT 
    DATE '2023-08-01' as billing_ym,
    M.member_id,
    M.mail,
    M.name,
    M.address,
    M.start_date,
    M.end_date,
    M.payment_method,
    (SELECT SUM(amount) 
       FROM T_CHARGE C 
      WHERE C.start_date < DATEADD(MONTH, 1, DATE '2023-08-01') AND 
            (C.end_date IS NULL OR C.end_date >= DATE '2023-08-01')) as sum_amount,
    0.1 as tax_ratio,
  FROM T_MEMBER M
  WHERE
    M.start_date < DATEADD(MONTH, 1, DATE '2023-08-01') AND 
    (M.end_date IS NULL OR M.end_date >= DATE '2023-08-01' )
);


-- 請求明細データ作成
INSERT INTO T_BILLING_DETAIL_DATA(
  billing_ym,
  member_id,
  charge_id,
  name,
  amount,
  start_date,
  end_date)
SELECT 
  DATE '2023-08-01' as billng_ym,
  M.member_id,
  C.charge_id,
  C.name,
  C.amount,
  C.start_date,
  C.end_date,
FROM 
  (SELECT * FROM T_MEMBER WHERE start_date < DATEADD(MONTH, 1, DATE '2023-08-01') AND 
            (end_date IS NULL OR end_date >= DATE '2023-08-01')) M,
  (SELECT * FROM T_CHARGE WHERE start_date < DATEADD(MONTH, 1, DATE '2023-08-01') AND 
            (end_date IS NULL OR end_date >= DATE '2023-08-01')) C;