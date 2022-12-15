import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.Uid

import java.time.ZonedDateTime

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED

class SchemaAdapter {

    def static createOrganizationSchema() {
        return {
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
}
