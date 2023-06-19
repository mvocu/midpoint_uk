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

    def static createPersonSchema() {
        return {
            type BaseScript.PERSON_NAME
            attributes {
                // black magic here - use better name for __UID__
                getBuilder().addAttributeInfo({ ->
                    def aib = new AttributeInfoBuilder(Uid.NAME);
                    aib.setNativeName("oident");
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
                    aib.setNativeName("oidos");
                    aib.setType(String.class);
                    aib.setRequired(true);
                    aib.build()
                }.call())

                login String.class
                prijmeni String.class
                jmeno String.class
                pohlavi String.class
                datum_narozeni ZonedDateTime.class
                misto_narozeni_txt String.class
                okres_narozeni String.class
                okres_narozeni_txt String.class
                stat_narozeni String.class
                datum_umrti ZonedDateTime
                obcanstvi String.class, MULTIVALUED
                stav String.class
                cislo_op String.class
                datova_schranka String.class
                rodne_cislo String.class
                mail String.class,MULTIVALUED
                phone String.class, MULTIVALUED
                mobile String.class, MULTIVALUED
            }
        }
    }

    def static getPersonFieldMap() {
        return [
                "__UID__"           : "oident",
                "__NAME__"          : "oidos",
                "login"             : "ologin",
                "prijmeni"          : "oprijmeni",
                "jmeno"             : "ojmeno",
                "pohlavi"           : "opohl",
                "datum_narozeni"    : "odatnar",
                "misto_narozeni_txt": "ormisto",
                "okres_narozeni"    : "orkrok",
                "okres_narozeni_txt": "orkroktxt",
                "stat_narozeni"     : "orstat",
                "datum_umrti"       : "odzemrel",
                "obcanstvi"         : "oobc",
                "stav"              : "orodst",
                "cislo_op"          : "oop",
                "datova_schranka"   : "ods",
                "rodne_cislo"       : "orodc",
                "mail"              : "omail",
                "phone"             : "otelef1",
                "mobile"            : "omobil",
                "obcanstvi2"        : "oobc2",
                "phone2"            : "otelef2",
        ]
    }

    def static mapPersonToIcfObject(GroovyResultSet row, Sql sql) {
        return ICFObjectBuilder.co {
            uid row.oident.toString()
            id row.oidos.toString()
            setObjectClass BaseScript.PERSON
            attribute 'login', row.ologin
            attribute 'jmeno', row.ojmeno
            attribute 'prijmeni', row.oprijmeni
            attribute 'pohlavi', row.opohl
            attribute 'datum_narozeni', row.odatnar ? ZonedDateTime.ofInstant(row.odatnar.toInstant(), ZoneId.systemDefault()) : null
            attribute 'misto_narozeni_txt', row.ormisto
            attribute 'okres_narozeni', row.orkrok
            attribute 'okres_narozeni_txt', row.orkroktxt
            attribute 'stat_narozeni', row.orstat
            attribute 'datum_umrti', row.odzemrel
            attribute 'obcanstvi', [ row.oobc, row.oobc2 ]
            attribute 'stav', row.orodst
            attribute 'cislo_op', row.oop
            attribute 'datova_schranka', row.ods
            attribute 'rodne_cislo', row.orodc
            attribute 'mail', row.omail
            attribute 'phone', [ row.otelef1, row.otelef2 ]
            attribute 'mobile', row.omobil
        }
    }

}
