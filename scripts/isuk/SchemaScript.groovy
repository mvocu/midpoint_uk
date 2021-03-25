import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder
import org.identityconnectors.framework.spi.operations.SearchOp
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos
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
            name()
            description()
	    id_org BigInteger.class
	    fakulta BigInteger.class
	    sidlo String.class
	    datum_od ZonedDateTime.class
	    datum_do ZonedDateTime.class
	    ic String.class
	    dic String.class
	    poid BigInteger.class
	    nazev String.class
	    nazev_dlouhy String.class
	    nazev_en String.class
	    nazev_dlouhy_en String.class
	    zkratka String.class
	    zkratka_dlouha String.class
	    zkratka_en String.class
	    zkratka_dlouha_en String.class
	    soucast BigInteger.class
	    kod_sims BigInteger.class
            cas_domena String.class
	    cas_identifikace String.class
	    id_org_nadrizeny BigInteger.class
        }
    }

    defineOperationOption OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildPageSize(), SearchOp
})

