import groovy.sql.Sql

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
  x_exportovat_typ IS NULL AND 
  ID_ORG NOT IN (SELECT ID_ORG FROM SKUNK_CAS.ORG_STRUKTURA) 
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

static void updatePeople(Sql sql) {
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
   WHERE lv.ID_VZTAH_WHOIS IS NULL;
''' as String

	sql.execute(mergeStudVztahyQuery);
	sql.commit();
}
