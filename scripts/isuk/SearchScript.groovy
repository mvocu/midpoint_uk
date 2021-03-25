import groovy.sql.Sql
import groovy.text.SimpleTemplateEngine
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.StringUtil
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.ResultsHandler
import org.identityconnectors.framework.common.objects.SearchResult
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

def pagedResultsCookie = null
def pageSize = 0

if( null != options.getPageSize() && options.getPageSize() > 0 ) {
	pageSize = options.getPageSize()

	if(!StringUtil.isBlank(options.getPagedResultsCookie())) {
		pagedResultsCookie = options.getPagedResultsCookie()
	}
}

//Need to handle the __UID__ and __NAME__ in queries - this map has entries for each objectType, 
//and is used to translate fields that might exist in the query object from the ICF identifier
//back to the real property name.
def fieldMap = [
        ( BaseScript.ORGANIZATION_NAME ): [
                "__UID__"    : "id_org",
                "__NAME__"   : "poid",
                "id_org"        : "id_org",
                "fakulta"       : "fakulta",
                "sidlo"         : "sidlo",
                "datum_od"      : "datum_od",
                "datum_do"      : "datum_do",
                "ic"            : "ic",
                "dic"           : "dic",
                "poid"          : "poid",
                "nazev"         : "nazev",
                "nazev_dlouhy"  : "nazev_dlouhy",
                "nazev_en"      : "nazev_en",
                "nazev_dlouhy_en" : "nazev_dlouhy_en",
                "zkratka"       : "zkratka",
                "zkratka_dlouha": "zkratka_dlouha",
                "zkratka_en"    : "zkratka_en",
                "zkratka_dlouha_en" : "zkratka_dlouha_en",
                "soucast"       : "soucast",
                "kod_sims"      : "kod_sims",
                "cas_domena"    : "cas_domena",
                "cas_identifikace" : "cas_identifikace",
                "id_org_nadrizeny" : "id_org_nadrizeny"

        ],
        "__ACCOUNT__" : [
                "__UID__"     : "id",
                "__NAME__"    : "uid"

        ],
        "__GROUP__"   : [
                "__UID__"    	: "id",
                "__NAME__"   	: "name"
        ]
]

def sqlquery = "SELECT * FROM SKUNK_CAS.ORG_STRUKTURA" as String
def whereParams = fieldMap[objectClass.objectClassValue]
def attrs = whereParams.collect([] as HashSet){ entry -> entry.value }
def where = ""
def wherePage = ""

if(pageSize > 0) {

	if(null == pagedResultsCookie) {
		wherePage = " WHERE ROWNUM <= " + pageSize
	} else {
        	wherePage = " WHERE ROWNUM > " + pagedResultsCookie + " AND ROWNUM <= " + ( pagedResultsCookie.toInteger() + pageSize.toInteger() )
	}
}

if(filter != null) {
        def queryTemplate = filter.accept(new SQLFilterVisitor(), whereParams)
        where =  " WHERE " + queryTemplate
}


log.info("Search WHERE clause is: " + where)
log.warn("Number of parameters in query: " + whereParams.size())


sqlquery = "SELECT " + attrs.join(",") + ", ROWNUM as radek FROM (" + sqlquery + where + " ORDER BY id_org ASC) " + wherePage

// replace filter parameter names with database column names according to whereParams map
sqlquery = new SimpleTemplateEngine().createTemplate(sqlquery).make(whereParams)


// takes parameter values from whereParams map, stored there by SQLFilterVisitor
sql.eachRow((Map) [:], (String) sqlquery, { row -> 

    def connectorObject = ICFObjectBuilder.co {
        switch (objectClass) {
/*
            case ObjectClass.ACCOUNT:
                uid row.id as String
                id row.uid
                break;

            case ObjectClass.GROUP:
                uid row.id as String
                id row.name
                break;
*/
            case BaseScript.ORGANIZATION:
	        uid row.id_org.toString()
       	     	id row.poid.toString()
		setObjectClass objectClass
            	attribute 'description', row.nazev_dlouhy
            	attribute 'id_org', row.id_org
            	attribute 'fakulta', row.fakulta
            	attribute 'sidlo', row.sidlo
            	attribute 'datum_od', row.datum_od.toString()
            	attribute 'datum_do', row.datum_do.toString()
            	attribute 'ic', row.ic
            	attribute 'dic', row.dic
            	attribute 'poid', row.poid
            	attribute 'nazev', row.nazev
            	attribute 'nazev_dlouhy', row.nazev_dlouhy
            	attribute 'nazev_en', row.nazev_en
            	attribute 'nazev_dlouhy_en', row.nazev_dlouhy_en
            	attribute 'zkratka', row.zkratka
            	attribute 'zkratka_dlouha', row.zkratka_dlouha
            	attribute 'zkratka_en', row.zkratka_en
            	attribute 'zkratka_dlouha_en', row.zkratka_dlouha_en
            	attribute 'soucast', row.soucast
            	attribute 'kod_sims', row.kod_sims
            	attribute 'cas_domena', row.cas_domena
            	attribute 'cas_identifikace', row.cas_identifikace
            	attribute 'id_org_nadrizeny', row.id_org_nadrizeny
                break;

            default:
                throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                        objectClass.objectClassValue + " is not supported.")
        }
    }

    if (pageSize > 0) {
        pageSize--
        //Just for simple paging by ID
        pagedResultsCookie = row.radek
    }

    handler.handle connectorObject
})

if (pageSize <= 0) {
    // There are no more page left
    pagedResultsCookie = null
}

return new SearchResult(pagedResultsCookie, -1)

