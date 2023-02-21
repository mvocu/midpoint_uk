import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder
import org.identityconnectors.framework.common.objects.SyncDeltaType
import org.identityconnectors.framework.common.objects.SyncResultsHandler
import org.identityconnectors.framework.common.objects.SyncToken
import org.identityconnectors.framework.common.objects.Uid

import java.sql.Timestamp

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

        return handleSync(sql, token, handler)
        break
	
    case OperationType.GET_LATEST_SYNC_TOKEN:
        return handleGetLatestSyncToken(sql)
}


Object handleSync(Sql sql, Object tokenObject, SyncResultsHandler handler) {

    def attrs
    def sqlquery = ""
    Long token

    if (tokenObject == null) {
        tokenObject = getLatestSyncToken(objectClass).getValue();
    }
    if (tokenObject instanceof Long) {
        token = (Long)tokenObject
    } else {
        log.warn("Synchronization token is not integer, ignoring")
    }

    def finalToken = new SyncToken(tokenObject)
    def tokenTimestamp = new Timestamp(token)

    if(objectClass == BaseScript.ORGANIZATION || objectClass == ObjectClass.ALL)
    {
        log.info("Updating organizations")
        UpdateDb.updateOrgs(sql)
        log.info("Organization update complete")
        log.info("Reading updated organization records")
        attrs = SchemaAdapter.getOrganizationFieldMap().collect([] as HashSet) { entry -> entry.value }
        sqlquery = "SELECT " + attrs.join(",") + ", x_last_modified, x_modification_type FROM SKUNK_CAS.LDAP_ORG_STRUKTURA WHERE x_last_modified > ? ORDER BY x_last_modified ASC"
        sql.eachRow(sqlquery, [ tokenTimestamp ], {
            row ->
                {
                    def deltaBuilder = new SyncDeltaBuilder()
                    def deltaToken = new SyncToken(row.x_last_modified.timestampValue().getTime())
                    finalToken = deltaToken
                    deltaBuilder.setToken(deltaToken)


                    switch (row.x_modification_type) {
                        case 'U':
                            deltaBuilder.setDeltaType(SyncDeltaType.UPDATE)
                            deltaBuilder.setObject(SchemaAdapter.mapOrganizationToIcfObject(row, sql))
                            break;

                        case 'D':
                            deltaBuilder.setDeltaType(SyncDeltaType.DELETE)
                            deltaBuilder.setObjectClass(BaseScript.PERSON)
                            uidAttr = SchemaAdapter.getOrganizationFieldMap()['__UID__']
                            deltaBuilder.setUid(new Uid(row.getAt(uidAttr)?.toString()))
                            break;

                        case 'C':
                            deltaBuilder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE)
                            deltaBuilder.setObject(SchemaAdapter.mapOrganizationToIcfObject(row, sql))
                            break;
                    }

                    handler.handle(deltaBuilder.build())
                }
        })
        log.info("Organization sync complete")
    }

    if(objectClass == BaseScript.PERSON || objectClass == ObjectClass.ALL) {
        log.info("Updating people")
        log.info("People update complete")
        log.info("Reading updated people records")
        attrs = SchemaAdapter.getPersonFieldMap().collect([] as HashSet) { entry -> entry.value }
        sqlquery = "SELECT " + attrs.join(",") + ",x_last_modified,x_modification_type FROM SKUNK_CAS.LDAP_OSOBA WHERE cuni_unique_id > 0 AND ou = 'people' AND X_LAST_MODIFIED > ? ORDER BY x_last_modified ASC"
        sql.eachRow(sqlquery, [ tokenTimestamp ], {
            row ->
                {
                    def deltaBuilder = new SyncDeltaBuilder()
                    def deltaToken = new SyncToken(row.x_last_modified.timestampValue().getTime())
                    finalToken = deltaToken
                    deltaBuilder.setToken(deltaToken)


                    switch (row.x_modification_type) {
                        case 'U':
                            deltaBuilder.setDeltaType(SyncDeltaType.UPDATE)
                            deltaBuilder.setObject(SchemaAdapter.mapPersonToIcfObject(row, sql))
                            break;

                        case 'D':
                            deltaBuilder.setDeltaType(SyncDeltaType.DELETE)
                            deltaBuilder.setObjectClass(BaseScript.PERSON)
                            uidAttr = SchemaAdapter.getPersonFieldMap()['__UID__']
                            deltaBuilder.setUid(new Uid(row.getAt(uidAttr)?.toString()))
                            break;

                        case 'C':
                            deltaBuilder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE)
                            deltaBuilder.setObject(SchemaAdapter.mapPersonToIcfObject(row, sql))
                            break;
                    }

                    handler.handle(deltaBuilder.build())
                }
        })
        log.info("People sync complete")
    }

    return finalToken
}


Object handleGetLatestSyncToken(Sql sql) {
    Long result = 0

    if(objectClass == BaseScript.ORGANIZATION || objectClass == ObjectClass.ALL) {
        sql.eachRow("select max(x_last_modified) as last from skunk_cas.ldap_org_struktura",
                { row -> if (row.last?.timestampValue().getTime() > result) { result = row.last.timestampValue().getTime() } })
    }
    if(objectClass == BaseScript.PERSON || objectClass == ObjectClass.ALL) {
        sql.eachRow("select max(x_last_modified) as last from skunk_cas.ldap_osoba",
                { row -> if (row.last?.timestampValue().getTime() > result) { result = row.last.timestampValue().getTime() } })
    }

    return new SyncToken(result)
}
