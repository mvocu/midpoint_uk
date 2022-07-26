import groovy.sql.Sql
import groovy.text.SimpleTemplateEngine

import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.StringUtil
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.ResultsHandler
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.spi.SearchResultsHandler
import java.sql.Connection
import java.time.ZonedDateTime
import java.time.ZoneId

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
        ( BaseScript.ORGANIZATION_NAME ): [
                "__UID__"    : "id_org",
                "__NAME__"   : "poid",
                "r_id_org"      : "id_org",
                "fakulta"       : "fakulta",
                "sidlo"         : "sidlo",
                "datum_od"      : "datum_od",
                "datum_do"      : "datum_do",
                "ic"            : "ic",
                "dic"           : "dic",
                "r_poid"        : "poid",
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
                "id_org_nadrizeny" : "id_org_nadrizeny",
		"poid_nadrizeny"   : "poid_nadrizeny"

        ],

	( BaseScript.PERSON_NAME ): [
                "__UID__"     : "cislo_osoby",
                "__NAME__"    : "cuni_unique_id",
		"jmeno"		: "jmeno",
		"prijmeni"	: "prijmeni",		
		"rodne_cislo"	: "rodne_cislo",
		"datum_narozeni" : "datum_narozeni",
		"stat"		: "st",
		"pohlavi"	: "pohlavi",
		"preferred_language" : "preferredlanguage"
		/*
		 preferred_language String.class
		 handicap String.class, MULTIVALUED
		 mail String.class, MULTIVALUED
		 phone String.class, MULTIVALUED
		 mobile String.class, MULTIVALUED
		 identifikace String.class
		 adresa_stat String.class
		 adresa_mesto String.class
		 adresa_ulice String.class
		 adresa_cev String.class
		 adresa_corg String.class
		 adresa_psc String.class
		 uid String.class
 */
 
        ],
	
        ( BaseScript.GROUP_NAME )  : [
                "__UID__"    	: "id",
                "__NAME__"   	: "name"
        ]
]

def attrs = fieldMap[objectClass.objectClassValue].collect([] as HashSet){ entry -> entry.value }
def sqlquery = "" 

switch(objectClass) {
	case BaseScript.ORGANIZATION:
		sqlquery = "SELECT " + attrs.join(",") + ", ROW_NUMBER() OVER (ORDER BY id_org ASC) AS radek FROM SKUNK_CAS.LDAP_ORG_STRUKTURA" as String
		break;

	case BaseScript.PERSON:
		sqlquery = "SELECT " + attrs.join(",") + ",ROW_NUMBER() OVER (ORDER BY cislo_osoby ASC) AS radek FROM (SELECT * FROM SKUNK_CAS.LDAP_OSOBA WHERE x_zaznam_platny = 1 AND cuni_unique_id > 0 AND ou = 'people')" as String 
		break;	
		
	default:
                throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                        objectClass.objectClassValue + " is not supported.")
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
		where = " WHERE " + fieldMap[objectClass.objectClassValue]["__UID__"] + " = :UID"
		whereParams["UID"] = id
	} else {
		// Search
        	def queryTemplate = filter.accept(new SQLFilterVisitor(), whereParams)
        	where =  " WHERE " + queryTemplate
	}
}

log.warn("Search WHERE clause is: {0} with params {1}", where, whereParams)

sqlquerycount = "SELECT COUNT(*) as total FROM (" + sqlquery + where + ")";
sqlquery = "SELECT " + attrs.join(",") + ",radek FROM (" + sqlquery + where + ") " + wherePage

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

    def connectorObject = ICFObjectBuilder.co {
        switch (objectClass) {

            case BaseScript.PERSON:
                uid row.cislo_osoby.toString()
                id row.cuni_unique_id.toString()
		setObjectClass objectClass
		attribute 'jmeno', row.jmeno
		attribute 'prijmeni', row.prijmeni
		attribute 'rodne_cislo', row.rodne_cislo
		attribute 'datum_narozeni', ZonedDateTime.ofInstant(row.datum_narozeni.toInstant(), ZoneId.systemDefault())
		attribute 'stat', row.st
		attribute 'pohlavi', row.pohlavi
		attribute 'preferred_language', row.preferredlanguage
		// handicap
		sql.eachRow(["id" : row.cislo_osoby], "SELECT hodnota FROM skunk_cas.ldap_ruzne WHERE nazev = 'cuniHandicap' AND cislo_osoby = :id", 
			{ subrow -> attribute 'handicap', subrow.hodnota })
		// mail
		sql.eachRow(["id" : row.cislo_osoby], "SELECT vk.EMAIL FROM skunk.PER_OSOBA po JOIN skunk.PER_KONTAKT pk ON pk.ID_OSOBA = po.ID_OSOBA JOIN skunk.VAL_KONTAKT vk ON vk.VAL_KONTAKT = pk.VAL_KONTAKT WHERE pk.KONTAKT_TYP = 7 AND po.CISLO_UK = :id", 
			{ subrow -> attribute 'mail', subrow.email } )
		/*		
		handicap String.class, MULTIVALUED
		mail String.class, MULTIVALUED
		phone String.class, MULTIVALUED
		mobile String.class, MULTIVALUED
		identifikace String.class
		adresa_stat String.class
		adresa_mesto String.class
		adresa_ulice String.class
		adresa_cev String.class
		adresa_corg String.class
		adresa_psc String.class
		uid String.class
*/
                break;
/*
            case ObjectClass.GROUP:
                uid row.id as String
                id row.name
                break;
*/
            case BaseScript.ORGANIZATION:
	        uid row.id_org.toString()
       	     	id row.poid.toString()
		setObjectClass objectClass
            	attribute 'r_id_org', row.id_org
            	attribute 'fakulta', row.fakulta
		attribute 's_fakulta', row.fakulta.toString()
            	attribute 'sidlo', row.sidlo
            	attribute 'datum_od', ZonedDateTime.ofInstant(row.datum_od.toInstant(), ZoneId.systemDefault())
            	attribute 'datum_do', ZonedDateTime.ofInstant(row.datum_do.toInstant(), ZoneId.systemDefault())
            	attribute 'ic', row.ic
            	attribute 'dic', row.dic
            	attribute 'r_poid', row.poid
            	attribute 'nazev', row.nazev
            	attribute 'nazev_dlouhy', row.nazev_dlouhy
            	attribute 'nazev_en', row.nazev_en
            	attribute 'nazev_dlouhy_en', row.nazev_dlouhy_en
            	attribute 'zkratka', row.zkratka
            	attribute 'zkratka_dlouha', row.zkratka_dlouha
            	attribute 'zkratka_en', row.zkratka_en
            	attribute 'zkratka_dlouha_en', row.zkratka_dlouha_en
            	attribute 'soucast', row.soucast
		attribute 's_soucast', row.soucast.toString()
            	attribute 'kod_sims', row.kod_sims
            	attribute 'cas_domena', row.cas_domena
            	attribute 'cas_identifikace', row.cas_identifikace
            	attribute 'id_org_nadrizeny', row.id_org_nadrizeny.split(',').collect { value -> new BigInteger(value) }
		attribute 'poid_nadrizeny', row.poid_nadrizeny.split(',').collect { value-> new BigInteger(value) }  
		attribute 's_poid_nadrizeny', row.poid_nadrizeny.split(',')
                break;

            default:
                throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                        objectClass.objectClassValue + " is not supported.")
        }
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

