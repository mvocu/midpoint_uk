import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.Uid

import java.time.ZonedDateTime

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

    def static createPersonSchema() {
        return {
            type BaseScript.PERSON_NAME
            attributes {
                // black magic here - use better name for __UID__
                getBuilder().addAttributeInfo({ ->
                    def aib = new AttributeInfoBuilder(Uid.NAME);
                    aib.setNativeName("cislo_osoby");
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
                    aib.setNativeName("cuni_unique_id");
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
                mail_o365 String.class, MULTIVALUED
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
            }
        }
    }

    def static getOrganizationFieldMap() {
        return [
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
        ]
    }
}
