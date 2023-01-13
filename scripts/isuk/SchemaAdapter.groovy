import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.Uid

import java.time.ZoneId
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

    def static getPersonFieldMap() {
        return [
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
        ]
    }

    def static getGroupFieldMap() {
        return [
                "__UID__"    	: "id",
                "__NAME__"   	: "name"
        ]
    }


    def static mapOrganizationToIcfObject(GroovyResultSet row, Sql sql) {
        return ICFObjectBuilder.co {
                    uid row.id_org.toString()
                    id row.poid.toString()
                    setObjectClass BaseScript.ORGANIZATION
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
        }
    }


    def static mapPersonToIcfObject(GroovyResultSet row, Sql sql) {
        return ICFObjectBuilder.co {
                    uid row.cislo_osoby.toString()
                    id row.cuni_unique_id.toString()
                    setObjectClass BaseScript.PERSON
                    attribute 'jmeno', row.jmeno
                    attribute 'prijmeni', row.prijmeni
                    attribute 'rodne_cislo', row.rodne_cislo
                    attribute 'datum_narozeni', row.datum_narozeni ? ZonedDateTime.ofInstant(row.datum_narozeni.toInstant(), ZoneId.systemDefault()) : null
                    attribute 'stat', row.st
                    attribute 'pohlavi', row.pohlavi
                    attribute 'preferred_language', row.preferredlanguage
                    // handicap
                    attribute 'handicap', sql.rows(["id" : row.cislo_osoby],
                            "SELECT DISTINCT hodnota FROM skunk_cas.ldap_ruzne WHERE nazev = 'cuniHandicap' AND cislo_osoby = :id"
                    )*.hodnota

                    // mail
                    attribute 'mail', sql.rows(["id" : row.cislo_osoby],
                            "SELECT DISTINCT hodnota FROM skunk_cas.ldap_ruzne WHERE nazev = 'mail' AND cislo_osoby = :id"
                    )*.hodnota
                    // mail_o365
                    attribute 'mail_o365', sql.rows(["id" : row.cislo_osoby],
                            "SELECT DISTINCT hodnota FROM skunk_cas.ldap_ruzne WHERE nazev = 'mail_o365' AND cislo_osoby = :id"
                    )*.hodnota
                    // phone
                    attribute 'phone', sql.rows(["id" : row.cislo_osoby],
                            "SELECT DISTINCT hodnota FROM skunk_cas.ldap_ruzne WHERE nazev = 'telephoneNumber' AND cislo_osoby = :id"
                    )*.hodnota
                    // mobile
                    attribute 'mobile', sql.rows(["id" : row.cislo_osoby],
                            "SELECT DISTINCT hodnota FROM skunk_cas.ldap_ruzne WHERE nazev = 'mobile' AND cislo_osoby = :id"
                    )*.hodnota
                    /*
                    identifikace String.class
                    adresa_stat String.class
                    adresa_mesto String.class
                    adresa_ulice String.class
                    adresa_cev String.class
                    adresa_corg String.class
                    adresa_psc String.class
                    uid String.class
                     */
        }
    }

}
