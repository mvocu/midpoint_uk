import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.*
import java.sql.Connection

def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def attributes = attributes as Set<Attribute>
def uid = uid as Uid
def id = id as Name
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection

log.info("Entering " + operation + " Script")

def sql = new Sql(connection)

switch (objectClass) {
    case BaseScript.PERSON:
        return handlePerson(sql, uid, attributes, operation)

    default:
        throw new UnsupportedOperationException("Update of object class " + objectClass + " not implemented")
}

def static Uid handlePerson(Sql sql, Uid uid, Set<Attribute> attributes, OperationType operation) {

    def wavky_login = '''
begin
  jadro.wv_login('db_api_whois-midpoint', 'SQL Developer', '', 'O365 - test - studentske e-maily');
end;
'''

    def wavky_logout = '''
begin
  jadro.wv_logout;
end;
'''

    sql.withTransaction {
        Map<String, Object> params = [:]

        List skipAttributes = [Uid.NAME]

        sql.execute(wavky_login)

        List<Object> mail_student = []

        for (Attribute attribute : attributes) {

            if (skipAttributes.contains(attribute.getName())) {
                continue
            }

            // special cases:
            switch(attribute.getName()) {
                case 'mail_student':
                    mail_student = attribute.getValue();
                    continue;

                default:
                    break;
            }

            Object value
            if (attribute.getValue() != null && attribute.getValue().size() > 1) {
                value = attribute.getValue()
            } else {
                value = AttributeUtil.getSingleValue(attribute)
            }

            params.put(attribute.getName(), value)
        }

        handlePersonMailStudent(sql, uid.getUidValue(), operation, mail_student)

        // ScriptedSqlUtils.buildAndExecuteUpdateQuery(sql, BaseScript.TABLE_USER, params, [id: uid.getUidValue() as Integer])

        sql.execute(wavky_logout)
    }

    return new Uid(uid.getUidValue())
}

def static void handlePersonMailStudent(Sql sql, String uid, OperationType operation, List<Object> mail_student) {

    def sql_add_mail_stud = '''
DECLARE 
  v_id_kontakt skunk.per_kontakt.id_kontakt%TYPE;
  v_id_osoba skunk.per_osoba.id_osoba%TYPE;
BEGIN
   SELECT id_osoba INTO v_id_osoba
   FROM skunk.per_osoba po 
   WHERE po.cislo_uk = :cislo_osoby ;
   skunk.API2_CON_MIDPOINT.NEW_EMAIL_O365_STUD(
		p_id_kontakt => v_id_kontakt, p_id_osoba => v_id_osoba, p_email => :email, p_verejny => 0);
END;
'''

    def sql_remove_mail_stud = '''
BEGIN
   FOR kontakt IN 
     (SELECT id_kontakt FROM skunk.per_kontakt pk
      JOIN skunk.val_kontakt vk ON vk.val_kontakt = pk.val_kontakt
      JOIN skunk.per_osoba po ON pk.id_osoba = po.id_osoba
      WHERE pk.kontakt_typ = 7 AND pk.o365 = 1 AND pk.ctx_vztah_typ = 2 AND vk.email IN ( :emails ) AND po.cislo_uk = :cislo_osoby)
   LOOP 
      skunk.API2_CON_MIDPOINT.DEL_EMAIL_O365_STUD(
           p_id_kontakt => kontakt.id_kontakt );
   END LOOP;
END;
'''

    def sql_remove_all_mail_stud = '''
BEGIN
   FOR kontakt IN 
     (SELECT id_kontakt FROM skunk.per_kontakt pk
      JOIN skunk.val_kontakt vk ON vk.val_kontakt = pk.val_kontakt
      JOIN skunk.per_osoba po ON pk.id_osoba = po.id_osoba
      WHERE pk.kontakt_typ = 7 AND pk.o365 = 1 AND pk.ctx_vztah_typ = 2 AND po.cislo_uk = :cislo_osoby)
   LOOP 
      skunk.API2_CON_MIDPOINT.DEL_EMAIL_O365_STUD(
           p_id_kontakt => kontakt.id_kontakt );
   END LOOP;
END;
'''

    def sql_sync_changes_1 = '''
    UPDATE LDAP_RUZNE lr
    SET
    x_zaznam_platny = 0,
    x_last_modified = sysdate,
    x_dtime_delete = sysdate,
    x_modification_type = 'D'
    WHERE lr.x_zaznam_platny = 1 AND lr.nazev IN ('mail_o365') AND lr.CISLO_OSOBY = :cislo_osoby AND NOT EXISTS (
         SELECT *
         FROM
            (
            SELECT
              po.CISLO_UK AS cislo_osoby,
              'mail_o365' AS nazev,
              'skunk.osoba' AS zdroj,
              vk.EMAIL AS hodnota,
              row_number() OVER (PARTITION BY po.CISLO_UK, pk.KONTAKT_TYP ORDER BY pk.PORADI) AS poradi,
              pk.CTX_ORG AS id_org,
              pk.ID_KONTAKT AS zdroj_identifikator,
              pk.ctx_vztah_typ AS vztah_typ
            FROM
              skunk.PER_OSOBA po
              JOIN skunk.PER_KONTAKT pk ON po.ID_OSOBA = pk.ID_OSOBA
              JOIN skunk.VAL_KONTAKT vk ON pk.VAL_KONTAKT = vk.VAL_KONTAKT
            WHERE pk.KONTAKT_TYP IN (7) AND pk.o365 = 1
          ) src
         WHERE
            lr.nazev = src.nazev
            AND lr.zdroj = src.zdroj
            AND lr.poradi = src.poradi
            AND lr.cislo_osoby = src.cislo_osoby
    )
    '''

    def sql_sync_changes_2 = '''
    UPDATE LDAP_RUZNE lr
    SET
    x_zaznam_platny = 0,
    x_last_modified = sysdate,
    x_dtime_delete = sysdate,
    x_modification_type = 'D'
    WHERE lr.x_zaznam_platny = 1 AND lr.nazev IN ('mail_whois') AND lr.CISLO_OSOBY = :cislo_osoby AND NOT EXISTS (
         SELECT *
         FROM
            (
            SELECT
              po.CISLO_UK AS cislo_osoby,
              'mail_o365' AS nazev,
              'skunk.osoba' AS zdroj,
              vk.EMAIL AS hodnota,
              row_number() OVER (PARTITION BY po.CISLO_UK, pk.KONTAKT_TYP ORDER BY pk.PORADI) AS poradi,
              pk.CTX_ORG AS id_org,
              pk.ID_KONTAKT AS zdroj_identifikator,
              pk.ctx_vztah_typ AS vztah_typ
            FROM
              skunk.PER_OSOBA po
              JOIN skunk.PER_KONTAKT pk ON po.ID_OSOBA = pk.ID_OSOBA
              JOIN skunk.VAL_KONTAKT vk ON pk.VAL_KONTAKT = vk.VAL_KONTAKT
            WHERE pk.KONTAKT_TYP IN (7)
          ) src
         WHERE
            lr.nazev = src.nazev
            AND lr.zdroj = src.zdroj
            AND lr.poradi = src.poradi
            AND lr.cislo_osoby = src.cislo_osoby
    )
    '''

    def sql_sync_changes_3 = '''
    MERGE INTO LDAP_RUZNE lr
    USING (
            SELECT
              po.CISLO_UK AS cislo_osoby,
              'mail_o365' AS nazev,
              'skunk.osoba' AS zdroj,
              vk.EMAIL AS hodnota,
              row_number() OVER (PARTITION BY po.CISLO_UK, pk.KONTAKT_TYP ORDER BY pk.PORADI) AS poradi,
              pk.CTX_ORG AS id_org,
              pk.ID_KONTAKT AS zdroj_identifikator,
              pk.ctx_vztah_typ AS vztah_typ
            FROM
              LDAP_OSOBA lo
              JOIN skunk.PER_OSOBA po ON po.cislo_uk = lo.cislo_osoby AND lo.x_zaznam_platny = 1
              JOIN skunk.PER_KONTAKT pk ON po.ID_OSOBA = pk.ID_OSOBA
              JOIN skunk.VAL_KONTAKT vk ON pk.VAL_KONTAKT = vk.VAL_KONTAKT
            WHERE pk.KONTAKT_TYP IN (7) AND pk.o365 = 1 AND lo.cislo_osoby = :cislo_osoby
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
      lr.vztah_typ = src.vztah_typ,
      lr.x_modification_type = decode(lr.x_zaznam_platny, 0, 'C', 'U'),
      lr.x_zaznam_platny = 1,
      lr.x_last_modified = sysdate,
      lr.x_dtime_update = sysdate
    WHERE
      lr.x_zaznam_platny = 0 OR
      lr.hodnota <> src.hodnota OR
      lr.zdroj_identifikator <> src.zdroj_identifikator OR
      lr.id_org <> src.id_org OR
      lr.vztah_typ <> src.vztah_typ
    WHEN NOT MATCHED THEN
    INSERT (
            id,
            nazev,
            zdroj,
            id_org,
            zdroj_identifikator,
            vztah_typ,
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
            src.vztah_typ,
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
    )
'''

    def sql_sync_changes_4 = '''
    MERGE INTO LDAP_RUZNE lr
    USING (
            SELECT
              po.CISLO_UK AS cislo_osoby,
              'mail_whois' AS nazev,
              'skunk.email' AS zdroj,
              vk.EMAIL AS hodnota,
              row_number() OVER (PARTITION BY po.CISLO_UK, pk.KONTAKT_TYP ORDER BY pk.PORADI) AS poradi,
              pk.CTX_ORG AS id_org,
              pk.ID_KONTAKT AS zdroj_identifikator,
              pk.ctx_vztah_typ AS vztah_typ
            FROM
              LDAP_OSOBA lo
              JOIN skunk.PER_OSOBA po ON po.cislo_uk = lo.cislo_osoby AND lo.x_zaznam_platny = 1
              JOIN skunk.PER_KONTAKT pk ON po.ID_OSOBA = pk.ID_OSOBA
              JOIN skunk.VAL_KONTAKT vk ON pk.VAL_KONTAKT = vk.VAL_KONTAKT
            WHERE pk.KONTAKT_TYP IN (7) AND lo.cislo_osoby = :cislo_osoby
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
      lr.vztah_typ = src.vztah_typ,
      lr.x_modification_type = decode(lr.x_zaznam_platny, 0, 'C', 'U'),
      lr.x_zaznam_platny = 1,
      lr.x_last_modified = sysdate,
      lr.x_dtime_update = sysdate
    WHERE
      lr.x_zaznam_platny = 0 OR
      lr.hodnota <> src.hodnota OR
      lr.zdroj_identifikator <> src.zdroj_identifikator OR
      lr.id_org <> src.id_org OR
      lr.vztah_typ <> src.vztah_typ
    WHEN NOT MATCHED THEN
    INSERT (
            id,
            nazev,
            zdroj,
            id_org,
            zdroj_identifikator,
            vztah_typ,
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
            src.vztah_typ,
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
    )
'''

    switch (operation) {
        case OperationType.ADD_ATTRIBUTE_VALUES:
            mail_student.each ({
                sql.execute(sql_add_mail_stud, ['cislo_osoby' : uid, 'email' : String.valueOf(it)])
            })
            break;

        case OperationType.REMOVE_ATTRIBUTE_VALUES:
            sql.execute(sql_remove_mail_stud, ['cislo_osoby' : uid, 'emails' : mail_student.collect( { return "'" + String.valueOf(it) + "'"}).join(',')])
            break;

        case OperationType.UPDATE:
            sql.execute(sql_remove_all_mail_stud, ['cislo_osoby' : uid])
            mail_student.each( {
                sql.execute(sql_add_mail_stud, ['cislo_osoby' : uid, 'email' : String.valueOf(it)] )
            })
            break;
    }

    sql.execute(sql_sync_changes_1, ['cislo_osoby' : uid])
    sql.execute(sql_sync_changes_2, ['cislo_osoby' : uid])
    sql.execute(sql_sync_changes_3, ['cislo_osoby' : uid])
    sql.execute(sql_sync_changes_4, ['cislo_osoby' : uid])

}

