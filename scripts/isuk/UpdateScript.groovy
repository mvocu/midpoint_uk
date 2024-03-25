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

def wavky_login = '''
begin
  jadro.wv_login('db_api_whois-midpoint', 'SQL Developer', 'O365 - test - studentske e-maily');
end;
/
'''

def wavky_logout = '''
begin
  jadro.wv_logout;
end;
/
'''

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
/
'''

def sql_remove_mail_stud = '''
BEGIN
   FOR kontakt IN 
     (SELECT id_kontakt FROM skunk.per_kontakt pk
      JOIN skunk.val_kontakt vk ON vk.val_kontakt = pk.val_kontakt
      JOIN skunk.per_osoba po ON pk.id_osoba = po.id_osoba
      WHERE pk.kontakt_typ = 7 AND pk.o365 = 1 AND vk.email IN ( :emails ) AND po.cislo_uk = :cislo_osoby)
   LOOP 
      skunk.API2_CON_MIDPOINT.DEL_EMAIL_O365_STUD(
           p_id_kontakt => kontakt.id_kontakt );
   END LOOP;
END;
/
'''

def sql_remove_all_mail_stud = '''
BEGIN
   FOR kontakt IN 
     (SELECT id_kontakt FROM skunk.per_kontakt pk
      JOIN skunk.val_kontakt vk ON vk.val_kontakt = pk.val_kontakt
      JOIN skunk.per_osoba po ON pk.id_osoba = po.id_osoba
      WHERE pk.kontakt_typ = 7 AND pk.o365 = 1 AND po.cislo_uk = :cislo_osoby)
   LOOP 
      skunk.API2_CON_MIDPOINT.DEL_EMAIL_O365_STUD(
           p_id_kontakt => kontakt.id_kontakt );
   END LOOP;
END;
/
'''

switch (objectClass) {
    case BaseScript.Person:
        return handlePerson(sql)

    default:
        throw new ConnectorException("Update of object class " + objectClass + " not implemented")
}

Uid handlePerson(Sql sql) {

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

        handlePersonMailStudent(sql, uid.getUidValue(), mail_student)

        // ScriptedSqlUtils.buildAndExecuteUpdateQuery(sql, BaseScript.TABLE_USER, params, [id: uid.getUidValue() as Integer])

        sql.execute(wavky_logout)
    }

    return new Uid(uid.getUidValue())
}

void handlePersonMailStudent(Sql sql, String uid, List<Object> mail_student) {
    switch (operation) {
        case OperationType.ADD_ATTRIBUTE_VALUES:
            mail_student.each ({
                sql.execute(sql_add_mail_stud, ['cislo_osoby' : uid, 'email' : String.valueOf(it)])
            })
            break;

        case OperationType.REMOVE_ATTRIBUTE_VALUES:
            sql.execute(sql_remove_mail_stud, ['cislo_osoby' : uid, 'emails' : mail_student.map( { return "'" + String.valueOf(it) + "'"}).join(',')])
            break;

        case OperationType.UPDATE:
            sql.execute(sql_remove_all_mail_stud, ['cislo_osoby' : uid])
            mail_student.each( {
                sql.execute(sql_add_mail_stud, ['cislo_osoby' : uid, 'email' : String.valueOf(it)] )
            })
            break;
    }

}

