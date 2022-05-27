import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos
import org.identityconnectors.framework.spi.operations.SearchOp
import java.time.ZonedDateTime

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED

def log = log as Log
def operation = operation as OperationType
def builder = builder as ICFObjectBuilder
def configuration = configuration as ScriptedSQLConfiguration


builder.schema({
    objectClass {
        type BaseScript.ORGANIZATION_NAME
        attributes {
	    // black magic here - use better name for __UID__
	    getBuilder().addAttributeInfo({ -> 
	        def aib = new AttributeInfoBuilder(Uid.NAME);
		aib.setNativeName("id_org");
		aib.setType(String.class);
		aib.setRequired(false); // Must be optional. It is not present for create operations
		aib.setCreateable(false);
		aib.setUpdateable(false);
		aib.setReadable(true);	
		aib.build()
	    }.call())

           // black magic here - use better name for __NAME__
            getBuilder().addAttributeInfo({ -> 
                def aib = new AttributeInfoBuilder(Name.NAME);
                aib.setNativeName("poid");
                aib.setType(String.class);
                aib.setRequired(true);
		aib.build()
            }.call())

	    r_id_org BigInteger.class
	    fakulta BigInteger.class
	    s_fakulta String.class
	    sidlo BigInteger.class
	    datum_od ZonedDateTime.class
	    datum_do ZonedDateTime.class
	    ic String.class
	    dic String.class
	    r_poid BigInteger.class
	    nazev String.class
	    nazev_dlouhy String.class
	    nazev_en String.class
	    nazev_dlouhy_en String.class
	    zkratka String.class
	    zkratka_dlouha String.class
	    zkratka_en String.class
	    zkratka_dlouha_en String.class
	    soucast BigInteger.class
	    s_soucast String.class
	    kod_sims String.class
            cas_domena String.class
	    cas_identifikace String.class
	    id_org_nadrizeny BigInteger.class, MULTIVALUED
	    poid_nadrizeny BigInteger.class, MULTIVALUED
	    s_poid_nadrizeny String.class, MULTIVALUED
        }
    }

    objectClass {
	type BaseScript.PERSON_NAME
        attributes {
            // black magic here - use better name for __UID__
            getBuilder().addAttributeInfo({ -> 
                def aib = new AttributeInfoBuilder(Uid.NAME);
                aib.setNativeName("cunipersonalid");
                aib.setType(String.class);
                aib.setRequired(false); // Must be optional. It is not present for create operations
                aib.setCreateable(false);
                aib.setUpdateable(false);
                aib.setReadable(true);  
                aib.build()
            }.call())

           // black magic here - use better name for __NAME__
            getBuilder().addAttributeInfo({ -> 
                def aib = new AttributeInfoBuilder(Name.NAME);
                aib.setNativeName("unique_identifier");
                aib.setType(String.class);
                aib.setRequired(true);
                aib.build()
            }.call())
	    
	    jmeno String.class
	    prijmeni String.class
	    rodne_cislo String.class
	    datum_narozeni ZonedDateTime.class
	    stat String.class
	    pohlavi String.class
	    preferred_language String.class
	    handicap String.class, MULTIVALUED
	    mail String.class, MULTIVALUED
	    phone String.class, MULTIVALUED
	    mobile String.class
	    identifikace String.class
	    adresa_stat String.class
	    adresa_mesto String.class
	    adresa_ulice String.class
	    adresa_cev String.class
	    adresa_corg String.class
	    adresa_psc String.class
	    uid String.class
        }
    }

    defineOperationOption OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildPageSize(), SearchOp
})

