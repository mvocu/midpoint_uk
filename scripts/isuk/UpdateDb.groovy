import groovy.sql.Sql

static void updatePeople(Sql sql) {
	def deleteContactsQuery = '''\
UPDATE LDAP_RUZNE lr
SET 
  x_zaznam_platny = 0,
  x_last_modified = sysdate,
  x_dtime_delete = sysdate,
  x_modification_type = 'D'
WHERE lr.x_zaznam_platny = 1 AND lr.nazev IN ('phone_whois', 'mobile_whois', 'mail_whois', 'pager_whois') AND NOT EXISTS (
SELECT * 
FROM
(
SELECT 
  po.CISLO_UK AS cislo_osoby,
  decode(pk.KONTAKT_TYP, 1, 'phone_whois', 2, 'mobile_whois', 7, 'mail_whois', 20, 'pager_whois', NULL) AS nazev,
  decode(pk.KONTAKT_TYP, 1, 'skunk.telefon', 2, 'skunk.mobil', 7, 'skunk.email', 20, 'skunk.sms', NULL) AS zdroj,
  decode(pk.KONTAKT_TYP, 1, '+'||substr(vk.telefon,1,12), 2, '+'||substr(vk.telefon,1,12), 7, vk.EMAIL, null) AS hodnota,
  row_number() OVER (PARTITION BY po.CISLO_UK, pk.KONTAKT_TYP ORDER BY pk.PORADI) AS poradi,
  pk.CTX_ORG AS id_org,
  pk.ID_KONTAKT AS zdroj_identifikator
FROM 
  skunk.PER_OSOBA po 
  JOIN skunk.PER_KONTAKT pk ON po.ID_OSOBA = pk.ID_OSOBA 
  JOIN skunk.VAL_KONTAKT vk ON pk.VAL_KONTAKT = vk.VAL_KONTAKT
WHERE pk.KONTAKT_TYP IN (1,2,7,20)
) src
WHERE 
  lr.nazev = src.nazev 
  AND lr.zdroj = src.zdroj
  AND lr.poradi = src.poradi
  AND lr.cislo_osoby = src.cislo_osoby
)''' as String

	def updateContactsQuery = '''\
MERGE INTO LDAP_RUZNE lr 
USING (
SELECT 
  po.CISLO_UK AS cislo_osoby,
  decode(pk.KONTAKT_TYP, 1, 'phone_whois', 2, 'mobile_whois', 7, 'mail_whois', 20, 'pager_whois', NULL) AS nazev,
  decode(pk.KONTAKT_TYP, 1, 'skunk.telefon', 2, 'skunk.mobil', 7, 'skunk.email', 20, 'skunk.sms', NULL) AS zdroj,
  decode(pk.KONTAKT_TYP, 1, '+'||substr(vk.telefon,1,12), 2, '+'||substr(vk.telefon,1,12), 7, vk.EMAIL, 20, '+'||substr(vk.telefon,1,12), null) AS hodnota,
  row_number() OVER (PARTITION BY po.CISLO_UK, pk.KONTAKT_TYP ORDER BY pk.PORADI) AS poradi,
  pk.CTX_ORG AS id_org,
  pk.ID_KONTAKT AS zdroj_identifikator
FROM 
  LDAP_OSOBA lo
  JOIN skunk.PER_OSOBA po ON po.cislo_uk = lo.cislo_osoby AND lo.x_zaznam_platny = 1
  JOIN skunk.PER_KONTAKT pk ON po.ID_OSOBA = pk.ID_OSOBA 
  JOIN skunk.VAL_KONTAKT vk ON pk.VAL_KONTAKT = vk.VAL_KONTAKT 
WHERE pk.KONTAKT_TYP IN (1,2,7,20)
) src
ON (
  lr.cislo_osoby = src.cislo_osoby AND
  lr.nazev = src.nazev AND 
  lr.zdroj = src.zdroj AND 
  lr.poradi = src.poradi
)
WHEN MATCHED THEN 
UPDATE SET 
  lr.hodnota = src.hodnota,
  lr.zdroj_identifikator = src.zdroj_identifikator,
  lr.id_org = src.id_org,
  lr.x_modification_type = decode(lr.x_zaznam_platny, 0, 'C', 'U'),
  lr.x_zaznam_platny = 1,
  lr.x_last_modified = sysdate,
  lr.x_dtime_update = sysdate
WHERE 
  lr.x_zaznam_platny = 0 OR
  lr.hodnota <> src.hodnota OR 
  lr.zdroj_identifikator <> src.zdroj_identifikator OR 
  lr.id_org <> src.id_org
WHEN NOT MATCHED THEN  
INSERT ( 
  id, 
  nazev, 
  zdroj,
  id_org,
  zdroj_identifikator,
  cislo_osoby,
  hodnota,
  poradi,
  x_zaznam_platny,
  x_exportovat_typ,
  x_dtime_create,
  x_dtime_update,
  x_dtime_delete,
  x_last_modified,
  x_modification_type)
VALUES (
  skunk_cas.seq_ldap_id.nextval,
  src.nazev,
  src.zdroj,
  src.id_org,
  src.zdroj_identifikator,
  src.cislo_osoby,
  src.hodnota,
  src.poradi,
  '1',
  'C',
  sysdate,
  NULL,
  NULL,
  sysdate,
  'C'
)''' as String

	def markPeopleUpdateQuery = '''\
MERGE INTO LDAP_OSOBA lo 
USING (
  SELECT CISLO_OSOBY, max(X_LAST_MODIFIED) AS x_last_modified
  FROM LDAP_RUZNE lr
  WHERE lr.nazev IN ('phone_whois', 'mobile_whois', 'mail_whois', 'pager_whois')
  GROUP BY CISLO_OSOBY
  ) src
ON (
  src.CISLO_OSOBY = lo.CISLO_OSOBY AND lo.X_ZAZNAM_PLATNY = 1
)
WHEN MATCHED THEN
UPDATE SET 
  lo.X_LAST_MODIFIED = src.X_LAST_MODIFIED,
  lo.X_MODIFICATION_TYPE = decode(lo.X_MODIFICATION_TYPE, 'D', 'D', 'C', 'C', 'U')
WHERE src.X_LAST_MODIFIED > lo.X_LAST_MODIFIED
''' as String

	sql.execute(deleteContactsQuery);
	sql.execute(updateContactsQuery);
	sql.commit();
	sql.execute(markPeopleUpdateQuery);
	sql.commit();
}

static void updateOrgs(Sql sql) {

	def deleteQuery = '''\
UPDATE skunk_cas.LDAP_ORG_STRUKTURA  
SET
  x_exportovat_typ = 'D', 
  x_zaznam_platny = 0, 
  x_dtime_delete = sysdate,
  X_LAST_MODIFIED = SYSDATE,
  X_MODIFICATION_TYPE = 'D'
WHERE
  x_zaznam_platny = 1 AND 
  ( ID_ORG NOT IN (SELECT ID_ORG FROM SKUNK_CAS.ORG_STRUKTURA) OR sysdate > DATUM_DO )
''' as String

	def updateQuery = '''\
MERGE INTO SKUNK_CAS.LDAP_ORG_STRUKTURA dst
USING
( SELECT 
  ID_ORG,
  FAKULTA,
  SIDLO,
  DATUM_OD,
  DATUM_DO,
  IC,
  DIC,
  POID,
  SOUCAST,
  NAZEV,
  NAZEV_DLOUHY,
  NAZEV_EN,
  NAZEV_DLOUHY_EN,
  ZKRATKA,
  ZKRATKA_DLOUHA,
  ZKRATKA_EN,
  ZKRATKA_DLOUHA_EN,
  KOD_SIMS,
  CAS_DOMENA,
  CAS_IDENTIFIKACE,
  LISTAGG(os.ID_ORG_NADRIZENY, ',') WITHIN GROUP ( ORDER BY ID_ORG_NADRIZENY ) AS ID_ORG_NADRIZENY, 
  LISTAGG(os.POID_NADRIZENY, ',') WITHIN GROUP ( ORDER BY ID_ORG_NADRIZENY ) AS POID_NADRIZENY
FROM SKUNK_CAS.ORG_STRUKTURA os
GROUP BY 
  ID_ORG,
  FAKULTA,
  SIDLO,
  DATUM_OD,
  DATUM_DO,
  IC,
  DIC,
  POID,
  SOUCAST,
  NAZEV,
  NAZEV_DLOUHY,
  NAZEV_EN,
  NAZEV_DLOUHY_EN,
  ZKRATKA,
  ZKRATKA_DLOUHA,
  ZKRATKA_EN,
  ZKRATKA_DLOUHA_EN,
  KOD_SIMS,
  CAS_DOMENA,
  CAS_IDENTIFIKACE
) src
ON ( dst.ID_ORG = src.ID_ORG )
WHEN MATCHED THEN 
UPDATE SET 
  dst.FAKULTA = src.FAKULTA,
  dst.SIDLO = src.SIDLO,
  dst.DATUM_OD = src.DATUM_OD,
  dst.DATUM_DO = src.DATUM_DO,
  dst.IC = src.IC,
  dst.DIC = src.DIC,
  dst.POID = src.POID,
  dst.SOUCAST = src.SOUCAST,
  dst.NAZEV = src.NAZEV,
  dst.NAZEV_DLOUHY = src.NAZEV_DLOUHY,
  dst.NAZEV_EN = src.NAZEV_EN,
  dst.NAZEV_DLOUHY_EN = src.NAZEV_DLOUHY_EN,
  dst.ZKRATKA = src.ZKRATKA,
  dst.ZKRATKA_DLOUHA = src.ZKRATKA_DLOUHA,
  dst.ZKRATKA_EN = src.ZKRATKA_EN,
  dst.ZKRATKA_DLOUHA_EN = src.ZKRATKA_DLOUHA_EN,
  dst.KOD_SIMS = src.KOD_SIMS,
  dst.CAS_DOMENA = src.CAS_DOMENA,
  dst.CAS_IDENTIFIKACE = src.CAS_IDENTIFIKACE,
  dst.ID_ORG_NADRIZENY = src.ID_ORG_NADRIZENY,
  dst.POID_NADRIZENY = src.POID_NADRIZENY,
  dst.X_ZAZNAM_PLATNY = 1,
  dst.X_EXPORTOVAT_TYP = DECODE(dst.X_ZAZNAM_PLATNY, 1, 'U', 'C'),
  dst.X_DTIME_UPDATE = SYSDATE,
  dst.X_LAST_MODIFIED  = SYSDATE,
  dst.X_MODIFICATION_TYPE = DECODE(dst.X_ZAZNAM_PLATNY, 1, 'U', 'C')
WHERE 
  dst.X_ZAZNAM_PLATNY = 0 OR 
  DECODE(src.FAKULTA , dst.FAKULTA , 0, 1) = 1 OR 
  DECODE(src.SIDLO , dst.SIDLO , 0, 1) = 1 OR
  DECODE(src.DATUM_OD , dst.DATUM_OD , 0, 1) = 1 OR
  DECODE(src.DATUM_DO , dst.DATUM_DO , 0, 1) = 1 OR
  DECODE(src.IC , dst.IC , 0, 1) = 1 OR
  DECODE(src.DIC , dst.DIC , 0, 1) = 1 OR
  DECODE(src.POID , dst.POID , 0, 1) = 1 OR
  DECODE(src.SOUCAST , dst.SOUCAST , 0, 1) = 1 OR
  DECODE(src.NAZEV , dst.NAZEV , 0, 1) = 1 OR
  DECODE(src.NAZEV_DLOUHY , dst.NAZEV_DLOUHY , 0, 1) = 1 OR
  DECODE(src.NAZEV_EN , dst.NAZEV_EN , 0, 1) = 1 OR
  DECODE(src.NAZEV_DLOUHY_EN , dst.NAZEV_DLOUHY_EN , 0, 1) = 1 OR
  DECODE(src.ZKRATKA , dst.ZKRATKA , 0, 1) = 1 OR
  DECODE(src.ZKRATKA_DLOUHA , dst.ZKRATKA_DLOUHA , 0, 1) = 1 OR
  DECODE(src.ZKRATKA_EN , dst.ZKRATKA_EN , 0, 1) = 1 OR
  DECODE(src.ZKRATKA_DLOUHA_EN , dst.ZKRATKA_DLOUHA_EN , 0, 1) = 1 OR
  DECODE(src.KOD_SIMS , dst.KOD_SIMS , 0, 1) = 1 OR
  DECODE(src.CAS_DOMENA , dst.CAS_DOMENA , 0, 1) = 1 OR
  DECODE(src.CAS_IDENTIFIKACE , dst.CAS_IDENTIFIKACE , 0, 1) = 1 OR
  DECODE(src.ID_ORG_NADRIZENY , dst.ID_ORG_NADRIZENY , 0, 1) = 1 OR
  DECODE(src.POID_NADRIZENY , dst.POID_NADRIZENY, 0, 1) = 1
WHEN NOT MATCHED THEN 
INSERT (
  dst.ID_ORG,
  dst.FAKULTA,
  dst.SIDLO,
  dst.DATUM_OD,
  dst.DATUM_DO,
  dst.IC,
  dst.DIC,
  dst.POID,
  dst.SOUCAST,
  dst.NAZEV,
  dst.NAZEV_DLOUHY,
  dst.NAZEV_EN,
  dst.NAZEV_DLOUHY_EN,
  dst.ZKRATKA,
  dst.ZKRATKA_DLOUHA,
  dst.ZKRATKA_EN,
  dst.ZKRATKA_DLOUHA_EN,
  dst.KOD_SIMS,
  dst.CAS_DOMENA,
  dst.CAS_IDENTIFIKACE,
  dst.ID_ORG_NADRIZENY,
  dst.POID_NADRIZENY,
  dst.X_ZAZNAM_PLATNY,
  dst.X_EXPORTOVAT_TYP,
  dst.X_DTIME_CREATE,
  dst.X_DTIME_UPDATE,
  dst.X_DTIME_DELETE,
  dst.X_LAST_MODIFIED,
  dst.X_MODIFICATION_TYPE 
)
VALUES (
  src.ID_ORG,
  src.FAKULTA,
  src.SIDLO,
  src.DATUM_OD,
  src.DATUM_DO,
  src.IC,
  src.DIC,
  src.POID,
  src.SOUCAST,
  src.NAZEV,
  src.NAZEV_DLOUHY,
  src.NAZEV_EN,
  src.NAZEV_DLOUHY_EN,
  src.ZKRATKA,
  src.ZKRATKA_DLOUHA,
  src.ZKRATKA_EN,
  src.ZKRATKA_DLOUHA_EN,
  src.KOD_SIMS,
  src.CAS_DOMENA,
  src.CAS_IDENTIFIKACE,
  src.ID_ORG_NADRIZENY,
  src.POID_NADRIZENY,
  1,
  'C',
  SYSDATE,
  NULL,
  NULL,
  SYSDATE,
  'C'
)''' as String

	sql.execute(deleteQuery);
	sql.execute(updateQuery);
	sql.commit();
}

static void updateRelations(Sql sql) {
	def mergeStudVztahyQuery = '''
MERGE INTO LDAP_VZTAH lv 
USING (
    SELECT po.CISLO_UK , rv.ID_VZTAH, rv.ID_ORG , rs.STUD_ID  
    FROM skunk.PER_OSOBA po 
    JOIN skunk.REL_VZTAH rv ON rv.ID_OSOBA = po.ID_OSOBA AND sysdate BETWEEN rv.DATUM_OD AND rv.DATUM_DO
    JOIN skunk.REL_STUDIUM rs ON rs.ID_VZTAH = rv.ID_VZTAH AND rv.VZTAH_TYP IN (2, 7)
  ) r
ON (lv.X_ZAZNAM_PLATNY = 1 
  AND lv.CISLO_OSOBY = r.CISLO_UK 
  AND lv.ID_ORG_WHOIS = r.ID_ORG 
  AND lv.zdroj LIKE 'studium.%' 
  AND lv.zdroj_identifikator = r.STUD_ID)
WHEN MATCHED THEN 
   UPDATE SET lv.ID_VZTAH_WHOIS = r.ID_VZTAH
   WHERE lv.ID_VZTAH_WHOIS IS NULL
''' as String

	def mergeCZVVztahyQuery = '''
MERGE INTO LDAP_VZTAH LV
USING (
  SELECT * FROM 
  ( 
    SELECT ROW_NUMBER() OVER (PARTITION BY rv.ID_OSOBA , rv.ID_ORG  ORDER BY rv.DATUM_OD DESC) AS rn, po.CISLO_UK , rv.*
    FROM skunk.PER_OSOBA po 
    JOIN skunk.REL_VZTAH rv  ON rv.ID_OSOBA = po.ID_OSOBA 
    WHERE rv.VZTAH_TYP = 7 AND sysdate BETWEEN rv.DATUM_OD AND rv.DATUM_DO 
    ORDER BY rv.ID_VZTAH, po.CISLO_UK 
  )
  WHERE rn = 1 
) r 
ON (
    lv.X_ZAZNAM_PLATNY = 1
    AND lv.CISLO_OSOBY = r.CISLO_UK
    AND lv.ID_ORG_WHOIS = r.ID_ORG
    AND lv.zdroj = 'studium.czv_aktivni'
)
WHEN MATCHED THEN 
  UPDATE SET lv.ID_VZTAH_WHOIS = r.ID_VZTAH
  WHERE lv.ID_VZTAH_WHOIS IS NULL
  ''' as String

	def mergeHranyQuery = '''
MERGE INTO LDAP_VZTAH lv2 
USING (
SELECT DISTINCT r.id, LISTAGG(r.id_org || ':' || r.souvislost || ':' || r.funkce, ';') WITHIN GROUP (ORDER BY r.funkce) AS hrany
 FROM
   (
    SELECT DISTINCT lv.ID, rh.ID_ORG, rh.SOUVISLOST, LISTAGG(rf.NAZEV_FUNKCE_KOD, ',') WITHIN GROUP (ORDER BY rf.ID_FUNKCE) AS funkce
    FROM LDAP_VZTAH lv 
    JOIN skunk.REL_HRANA rh ON rh.ID_VZTAH = lv.ID_VZTAH_WHOIS AND sysdate BETWEEN rh.DATUM_OD AND rh.DATUM_DO  
    LEFT JOIN skunk.REL_FUNKCE rf ON rf.ID_VZTAH_HRANA = rh.ID_VZTAH_HRANA AND sysdate BETWEEN rf.DATUM_OD AND rf.DATUM_DO 
    WHERE lv.X_ZAZNAM_PLATNY = 1 
    GROUP BY lv.ID, rh.ID_ORG, rh.SOUVISLOST
   ) r
  GROUP BY r.id
 ) r2
ON (
   lv2.X_ZAZNAM_PLATNY = 1
   AND lv2.id = r2.id
)
WHEN MATCHED THEN 
   UPDATE SET 
     lv2.HRANY = r2.hrany,
     lv2.X_LAST_MODIFIED = sysdate,
     lv2.X_MODIFICATION_TYPE = decode(lv2.X_MODIFICATION_TYPE, 'C', 'C', 'D', 'D', 'U')
   WHERE decode(r2.hrany, lv2.hrany, 0, 1) = 1
''' as String

	sql.execute(mergeStudVztahyQuery);
	sql.execute(mergeCZVVztahyQuery);
	sql.execute(mergeHranyQuery);
	sql.commit();
}
