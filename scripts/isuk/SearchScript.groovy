import groovy.sql.Sql
import groovy.text.SimpleTemplateEngine

import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.StringUtil
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.ResultsHandler
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter
import java.sql.Connection

def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def configuration = configuration as ScriptedSQLConfiguration
def filter = filter as Filter
def connection = connection as Connection
def query = query as Closure
def handler = handler as ResultsHandler

log.info("Entering " + operation + " Script")

def sql = new Sql(connection)

def pagedResultsCookie = null as String
def pageSize = 0 as Integer
def pageOffset = 0 as Integer

if( null != options.getPageSize() && options.getPageSize() > 0 ) {
	pageSize = options.getPageSize()
}

if(!StringUtil.isBlank(options.getPagedResultsCookie())) {
	pagedResultsCookie = options.getPagedResultsCookie()
	pageOffset = pagedResultsCookie.toInteger()
}

if(null != options.getPagedResultsOffset()) {
	pageOffset = Math.max(options.getPagedResultsOffset() - 1, 0)
}

log.warn("Page size " + pageSize + ", page offset " + pageOffset +  ", page cookie " + pagedResultsCookie)

//Need to handle the __UID__ and __NAME__ in queries - this map has entries for each objectType, 
//and is used to translate fields that might exist in the query object from the ICF identifier
//back to the real property name.
def fieldMap = [
		( BaseScript.ORGANIZATION_NAME ): SchemaAdapter.getOrganizationFieldMap(),
		( BaseScript.PERSON_NAME ): SchemaAdapter.getPersonFieldMap(),
		( BaseScript.RELATION_NAME )  : SchemaAdapter.getRelationFieldMap(),
		( BaseScript.FUNCTION_NAME ) : SchemaAdapter.getFunctionFieldMap()
]

def attrs = fieldMap[objectClass.objectClassValue].collect({ entry -> entry.value }).unique()
def sqlquery = "" 

switch(objectClass) {
	case BaseScript.ORGANIZATION:
		sqlquery = "SELECT " + attrs.join(",") + ", ROW_NUMBER() OVER (ORDER BY id_org ASC) AS radek FROM SKUNK_CAS.LDAP_ORG_STRUKTURA"	as String
		break;

	case BaseScript.PERSON:
		sqlquery = "SELECT " + attrs.join(",") + ",ROW_NUMBER() OVER (ORDER BY cislo_osoby ASC) AS radek" +
				" FROM (SELECT * FROM SKUNK_CAS.LDAP_OSOBA WHERE x_zaznam_platny = 1 AND cuni_unique_id > 0 AND ou = 'people')" as String
		break;	

	case BaseScript.RELATION:
 		def cols = attrs.join(",")
		def p_cols = attrs.collect({ it -> it.split('[.]')?.last()}).join(",")
		sqlquery = "SELECT " + p_cols + ", ROW_NUMBER() OVER (ORDER BY id ASC) AS radek " +
				" FROM (SELECT " + cols +
                " FROM SKUNK_CAS.LDAP_VZTAH lv"  +
				" LEFT JOIN SKUNK.REL_VZTAH rv ON lv.id_vztah_whois = rv.id_vztah " +
				" WHERE x_zaznam_platny = 1 )" as String
		break;

	case BaseScript.FUNCTION:
                def cols = attrs.findAll({ it != 'nazev' })
		sqlquery = "SELECT " + cols.join(",") + ", nazev, ROW_NUMBER() OVER (ORDER BY id_kod ASC) as radek " +
                                " FROM ( SELECT " + cols.join(",") + ", WAVKY.MULTILANG_PKG.GET_TEXT('cs', nazev) as nazev" +
				"        FROM CIS2.E_REL_FUNKCE_NAZEV " +
                                "        WHERE sysdate BETWEEN datum_od AND datum_do ) " as String
		break;

	default:
		throw new UnsupportedOperationException(operation.name() + " operation of type:"
        	+ objectClass.objectClassValue + " is not supported.")
}
	

def whereParams = [:]
def where = ""
def wherePage = ""

if(pageSize > 0) {

	if(pageOffset == 0) {
		wherePage = " WHERE radek <= " + pageSize
	} else {
        	wherePage = " WHERE radek > " + pageOffset + " AND radek <= " + ( pageOffset + pageSize )
	}
}

if(filter != null) {
	if(filter instanceof EqualsFilter && ((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
		// Read
		def id = AttributeUtil.getStringValue(((EqualsFilter)filter).getAttribute())
		log.warn("Reading object with id [{0}]", id);
		if(id == null || !id.isNumber() ) {
			log.warn("Empty UID parameter");
			return;
		}
		where = " WHERE " + fieldMap[objectClass.objectClassValue]["__UID__"].split('[.]')?.last() + " = :UID"
		whereParams["UID"] = id
	} else {
		// Search
        	def queryTemplate = filter.accept(new SQLFilterVisitor(), whereParams)
        	where =  " WHERE " + queryTemplate
	}
}

log.warn("Search WHERE clause is: {0} with params {1}", where, whereParams)

sqlquerycount = "SELECT COUNT(*) as total FROM (" + sqlquery + where + ")";
sqlquery = "SELECT " + attrs.collect({it -> it.split('[.]').last()}).join(",") + ",radek FROM (" + sqlquery + where + ") " + wherePage

// replace filter parameter names with database column names according to whereParams map
sqlquery = new SimpleTemplateEngine().createTemplate(sqlquery).make(fieldMap[objectClass.objectClassValue])
sqlquerycount = new SimpleTemplateEngine().createTemplate(sqlquerycount).make(fieldMap[objectClass.objectClassValue]) 

log.warn("SQL query: {0}", sqlquery)
log.warn("SQL count query: {0}", sqlquerycount)

int total = 0
sql.eachRow((Map) whereParams, (String) sqlquerycount, { row -> total = row.total })

log.warn("Found {0} total records", total)

// takes parameter values from whereParams map, stored there by SQLFilterVisitor
int lastRowNum = 0
def rowCount = 0
sql.eachRow((Map) whereParams, (String) sqlquery, { row -> 
	def connectorObject = null
        switch (objectClass) {

            case BaseScript.PERSON:
				connectorObject = SchemaAdapter.mapPersonToIcfObject(row, sql)
                break;

            case BaseScript.RELATION:
				connectorObject = SchemaAdapter.mapRelationToIcfObject(row, sql)
                break;

            case BaseScript.ORGANIZATION:
				connectorObject = SchemaAdapter.mapOrganizationToIcfObject(row, sql)
                break;

            case BaseScript.FUNCTION:
				connectorObject = SchemaAdapter.mapFunctionToIcfObject(row, sql)
		break;

            default:
                throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                        objectClass.objectClassValue + " is not supported.")
        }
    	lastRowNum = (int)row.radek
    	rowCount++

   	handler.handle connectorObject
})


if (lastRowNum < pageOffset + pageSize) {
    // There are no more pages left
    pagedResultsCookie = null
} else {
    pagedResultsCookie = lastRowNum.toString()
}

log.warn("Returning page cookie " + pagedResultsCookie)

return new SearchResult(pagedResultsCookie, pageSize > 0 ? (total - lastRowNum) : -1 , lastRowNum >= total)

