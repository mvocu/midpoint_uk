import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.SyncResultsHandler

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
    switch(objectclass) {
        case BaseScript.ORGANIZATION:
            log.debug("Updating organizations")
            UpdateDbScript.updateOrgs(sql)
            log.debug("Organization update complete")
            break

        default:
            log.warn("Objectclass [{0}] is not (yet) supported", objectclass)
            break
    }
    // todo implement
}

Object handleGetLatestSyncToken(Sql sql) {
    int result = 0

    sql.eachRow("select max(x_last_modified) from skunk_cas.ldap_org_struktura",
            {row -> if(row.x_last_modified) > result { result = row.x_last_modified }})

    return result
}