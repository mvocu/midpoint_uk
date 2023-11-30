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
                mail_o365_primary String.class
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
                phone_whois String.class, MULTIVALUED
                mail_whois String.class, MULTIVALUED
                mobile_whois String.class, MULTIVALUED
                pager_whois String.class, MULTIVALUED
            }
        }
    }

    def static createRelationSchema() {
        return {
            type BaseScript.RELATION_NAME
            attributes {
                // black magic here - use better name for __UID__
                getBuilder().addAttributeInfo({ ->
                    def aib = new AttributeInfoBuilder(Uid.NAME);
                    aib.setNativeName("id");
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
                    aib.setNativeName("id");
                    aib.setType(String.class);
                    aib.setRequired(true);
                    aib.build()
                }.call())

                r_id BigInteger.class
                typ String.class
                zdroj String.class
                id_org BigInteger.class
                zdroj_identifikator BigInteger.class
                cislo_osoby String.class // string, to match Person
                studyprogram String.class
                studysubject1 String.class
                studysubject2 String.class
                jazyk_vyuky String.class
                id_org_whois String.class // string, to be used in association
                id_vztah_whois BigInteger.class
                hrany String.class, MULTIVALUED
                vztah_typ BigInteger.class
                id_funkce BigInteger.class
                studium_typ String.class
                zamestnani_typ BigInteger.class
                clenstvi_typ BigInteger.class
                externista_typ BigInteger.class
            }
        }
    }

    def static createFunctionSchema() {
        return {
            type BaseScript.FUNCTION_NAME
            attributes {
                // black magic here - use better name for __UID__
                getBuilder().addAttributeInfo({ ->
                    def aib = new AttributeInfoBuilder(Uid.NAME);
                    aib.setNativeName("id_kod");
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
                    aib.setNativeName("kod");
                    aib.setType(String.class);
                    aib.setRequired(true);
                    aib.build()
                }.call())

                r_kod BigInteger.class
                typ_kod BigInteger.class
                nazev String.class
                dulezitost BigInteger.class
                samospravni_funkce BigInteger.class
                datum_od ZonedDateTime.class
                datum_do ZonedDateTime.class
            }
        }
    }

    def static getOrganizationFieldMap() {
        return [
                "__UID__"          : "id_org",
                "__NAME__"         : "poid",
                "r_id_org"         : "id_org",
                "fakulta"          : "fakulta",
                "sidlo"            : "sidlo",
                "datum_od"         : "datum_od",
                "datum_do"         : "datum_do",
                "ic"               : "ic",
                "dic"              : "dic",
                "r_poid"           : "poid",
                "nazev"            : "nazev",
                "nazev_dlouhy"     : "nazev_dlouhy",
                "nazev_en"         : "nazev_en",
                "nazev_dlouhy_en"  : "nazev_dlouhy_en",
                "zkratka"          : "zkratka",
                "zkratka_dlouha"   : "zkratka_dlouha",
                "zkratka_en"       : "zkratka_en",
                "zkratka_dlouha_en": "zkratka_dlouha_en",
                "soucast"          : "soucast",
                "kod_sims"         : "kod_sims",
                "cas_domena"       : "cas_domena",
                "cas_identifikace" : "cas_identifikace",
                "id_org_nadrizeny" : "id_org_nadrizeny",
                "poid_nadrizeny"   : "poid_nadrizeny"
        ]
    }

    def static getPersonFieldMap() {
        return [
                "__UID__"           : "cislo_osoby",
                "__NAME__"          : "cuni_unique_id",
                "jmeno"             : "jmeno",
                "prijmeni"          : "prijmeni",
                "rodne_cislo"       : "rodne_cislo",
                "datum_narozeni"    : "datum_narozeni",
                "stat"              : "st",
                "pohlavi"           : "pohlavi",
                "preferred_language": "preferredlanguage",
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
        ]
    }

    def static getRelationFieldMap() {
        return [
                "__UID__"            : "lv.id",
                "__NAME__"           : "lv.id",
                "typ"                : "lv.typ",
                "zdroj"              : "lv.zdroj",
                "id_org"             : "lv.id_org",
                "zdroj_identifikator": "lv.zdroj_identifikator",
                "cislo_osoby"        : "lv.cislo_osoby",
                "studyprogram"       : "lv.studyprogram",
                "studysubject1"      : "lv.studysubject1",
                "studysubject2"      : "lv.studysubject2",
                "jazyk_vyuky"        : "lv.jazyk_vyuky",
                "id_org_whois"       : "lv.id_org_whois",
                "id_vztah_whois"     : "lv.id_vztah_whois",
                "hrany"              : "lv.hrany",
                "vztah_typ"          : "rv.vztah_typ",
                "id_funkce"          : "rv.id_funkce",
                "studium_typ"        : "rv.studium_typ",
                "zamestnani_typ"     : "rv.zamestnani_typ",
                "clenstvi_typ"       : "rv.clenstvi_typ",
                "externista_typ"     : "rv.externista_typ"
        ]
    }

    def static getFunctionFieldMap() {
        return [
                "__UID__"            : "id_kod",
                "__NAME__"           : "kod",
                "r_kod"              : "kod",
                "typ_kod"            : "funkce_typ",
                "nazev"              : "nazev", 
                "dulezitost"         : "dulezitost",
                "samospravni_funkce" : "samospravni_funkce",
                "datum_od"         : "datum_od",
                "datum_do"         : "datum_do"
        ]
    }


    def static mapOrganizationToIcfObject(GroovyResultSet row, Sql sql) {
        return ICFObjectBuilder.co {
            uid row.id_org.toString()
            id row.poid.toString()
            setObjectClass BaseScript.ORGANIZATION
            attribute 'r_id_org', row.id_org.toBigInteger()
            attribute 'fakulta', row.fakulta?.toBigInteger()
            attribute 's_fakulta', row.fakulta?.toString()
            attribute 'sidlo', row.sidlo?.toBigInteger()
            attribute 'datum_od', ZonedDateTime.ofInstant(row.datum_od.toInstant(), ZoneId.systemDefault())
            attribute 'datum_do', ZonedDateTime.ofInstant(row.datum_do.toInstant(), ZoneId.systemDefault())
            attribute 'ic', row.ic
            attribute 'dic', row.dic
            attribute 'r_poid', row.poid?.toBigInteger()
            attribute 'nazev', row.nazev
            attribute 'nazev_dlouhy', row.nazev_dlouhy
            attribute 'nazev_en', row.nazev_en
            attribute 'nazev_dlouhy_en', row.nazev_dlouhy_en
            attribute 'zkratka', row.zkratka
            attribute 'zkratka_dlouha', row.zkratka_dlouha
            attribute 'zkratka_en', row.zkratka_en
            attribute 'zkratka_dlouha_en', row.zkratka_dlouha_en
            attribute 'soucast', row.soucast?.toBigInteger()
            attribute 's_soucast', row.soucast.toString()
            attribute 'kod_sims', row.kod_sims
            attribute 'cas_domena', row.cas_domena
            attribute 'cas_identifikace', row.cas_identifikace
            attribute 'id_org_nadrizeny', row.id_org_nadrizeny.split(',').collect { value -> new BigInteger(value) }
            attribute 'poid_nadrizeny', row.poid_nadrizeny.split(',').collect { value -> new BigInteger(value) }
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

            // get attributes from ldap_ruzne
            def ruzne = [:]
            def orderedRuzne = ['phone_whois', 'mail_whois', 'mobile_whois', 'pager_whois'];
            sql.eachRow(["id": row.cislo_osoby],
                    "SELECT DISTINCT nazev, hodnota, poradi FROM skunk_cas.ldap_ruzne WHERE cislo_osoby = :id AND x_zaznam_platny = 1 ORDER BY poradi",
                    {
                        if (ruzne.containsKey(it.nazev)) {
                            if(orderedRuzne.contains(it.nazev)) {
                                (ruzne[it.nazev] as List).add(it.poradi + ":" + it.hodnota)
                            } else {
                                if(!(ruzne[it.nazev] as List).contains(it.hodnota)) {
                                    (ruzne[it.nazev] as List).add(it.hodnota)
                                }
                            }
                        } else {
                            if(orderedRuzne.contains(it.nazev)) {
                                ruzne[it.nazev] = [ it.poradi + ":" + it.hodnota ]
                            } else {
                                ruzne[it.nazev] = [it.hodnota]
                            }
                        }
                    }
            )

            // handicap
            attribute 'handicap', ruzne['cuniHandicap']
            // mail
            attribute 'mail', ruzne['mail']
            // mail_o365
            attribute 'mail_o365', ruzne['mail_o365']
            // mail_o365_primary
            attribute 'mail_o365_primary', ruzne['mail_o365_primary']?.first()
            // phone
            attribute 'phone', ruzne['telephoneNumber']
            // mobile
            attribute 'mobile', ruzne['mobile']
            // phone_whois
            attribute 'phone_whois', ruzne['phone_whois']
            // mail_whois
            attribute 'mail_whois', ruzne['mail_whois']
            // mobile_whois
            attribute 'mobile_whois', ruzne['mobile_whois']
            // pager_whois
            attribute 'pager_whois', ruzne['pager_whois']
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

    def static mapRelationToIcfObject(GroovyResultSet row, Sql sql) {
        return ICFObjectBuilder.co {
            uid row.id.toString()
            id row.id.toString()
            setObjectClass BaseScript.RELATION
            attribute 'r_id', row.id
            attribute 'typ', row.typ
            attribute 'zdroj', row.zdroj
            attribute 'id_org', row.id_org
            attribute 'zdroj_identifikator', row.zdroj_identifikator
            attribute 'cislo_osoby', row.cislo_osoby.toString()
            attribute 'studyprogram', row.studyprogram
            attribute 'studysubject1', row.studysubject1
            attribute 'studysubject2', row.studysubject2
            attribute 'jazyk_vyuky', row.jazyk_vyuky
            attribute 'id_org_whois', row.id_org_whois.toString()
            attribute 'id_vztah_whois', row.id_vztah_whois

            /*
            def hrany = []

            if(row.id_vztah_whois) {
                sql.eachRow(["id_vztah": row.id_vztah_whois],
                        "SELECT id_org, souvislost FROM skunk.rel_hrana WHERE id_vztah = :id_vztah AND sysdate BETWEEN datum_od AND datum_do",
                        {
                            hrany.add(it.id_org.toString() + ":" + it.souvislost.toString())
                        }
                )
            }
            */

            attribute 'hrany', row.hrany?.split(";")
            attribute "vztah_typ", row.vztah_typ
            attribute "id_funkce", row.id_funkce
            attribute "studium_typ", row.studium_typ
            attribute "zamestnani_typ", row.zamestnani_typ
            attribute "clenstvi_typ", row.clenstvi_typ
            attribute "externista_typ", row.externista_typ
        }
    }

    def static mapFunctionToIcfObject(GroovyResultSet row, Sql sql) {
        return ICFObjectBuilder.co {
            uid row.id_kod.toString()
            id row.kod.toString()
            setObjectClass BaseScript.FUNCTION

            attribute 'r_kod', row.kod
            attribute 'typ_kod', row.funkce_typ
            attribute 'dulezitost', row.dulezitost
            attribute 'samospravni_funkce', row.samospravni_funkce
            attribute 'nazev', row.nazev
            attribute 'datum_od', ZonedDateTime.ofInstant(row.datum_od.toInstant(), ZoneId.systemDefault())
            attribute 'datum_do', ZonedDateTime.ofInstant(row.datum_do.toInstant(), ZoneId.systemDefault())

            /*
            nazev String.class
             */

        }
    }
}
