import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.SyncResultsHandler
import org.identityconnectors.framework.common.objects.SyncToken

def configuration = configuration as ScriptedSQLConfiguration
def operation = operation as OperationType
def objectClass = objectClass as ObjectClass
def log = log as Log

log.info("Entering " + operation + " Script");

def sql = new Sql(connection)

switch (operation) {
    case OperationType.SYNC:
        def token = token as Object
        def handler = handler as SyncResultsHandler

        handleSync(sql, token, handler)
        break
	
    case OperationType.GET_LATEST_SYNC_TOKEN:
        return handleGetLatestSyncToken(sql)
}


void handleSync(Sql sql, Object tokenObject, SyncResultsHandler handler) {

    def attrs
    def sqlquery = ""
    Integer token

    if (tokenObject == null) {
        tokenObject = getLatestSyncToken(objectClass);
    }
    def fromTokenValue = tokenObject.getValue()
    if (fromTokenValue instanceof Integer) {
        token = (Integer)fromTokenValue
    } else {
        log.warn("Synchronization token is not integer, ignoring")
    }

    def finalToken = token

    switch(objectClass) {

        case BaseScript.ORGANIZATION:
            log.info("Updating organizations")
            UpdateDb.updateOrgs(sql)
            log.info("Organization update complete")
            break

        case BaseScript.PERSON:
            log.info("Updating people")
            log.info("People update complete")
            log.info("Reading update people records")
            attrs = SchemaAdapter.getPersonFieldMap().collect([] as HashSet) { entry -> entry.value }
            sqlquery = "SELECT " + attrs.join(",") + " FROM SKUNK_CAS.LDAP_OSOBA WHERE x_zaznam_platny = 1 AND cuni_unique_id > 0 AND ou = 'people'" as String
            log.info("People sync complete")
            break

        case ObjectClass.ALL:
            log.info("Updating organizations")
            UpdateDb.updateOrgs(sql)
            log.info("Organization update complete")
	    break

        default:
            log.warn("Objectclass [{0}] is not (yet) supported", objectClass)
            break
    }
    // todo implement
}


Object handleGetLatestSyncToken(Sql sql) {
    Integer result = 0

    sql.eachRow("select max(x_last_modified) as last from skunk_cas.ldap_org_struktura",
            {row -> if(row.last > result) { result = row.last }})

    return new SyncToken(result)
}
