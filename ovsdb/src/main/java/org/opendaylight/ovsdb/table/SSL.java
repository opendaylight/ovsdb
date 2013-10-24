package org.opendaylight.ovsdb.table;

import org.opendaylight.ovsdb.datatype.OvsDBMap;
import org.opendaylight.ovsdb.datatype.OvsDBSet;
import org.opendaylight.ovsdb.table.internal.Table;
import org.opendaylight.ovsdb.table.internal.Table.Name;

public class SSL  extends Table<SSL> {

    public static final Name<SSL> NAME = new Name<SSL>("SSL") {};
    private String ca_cert;
    private Boolean bootstrap_ca_cert;
    private String certificate;
    private String private_key;
    private OvsDBMap<String, String> external_ids;

    public String getCa_cert() {
        return ca_cert;
    }
    public void setCa_cert(String ca_cert) {
        this.ca_cert = ca_cert;
    }
    public OvsDBMap<String, String> getExternal_ids() {
        return external_ids;
    }
    public void setExternal_ids(OvsDBMap<String, String> external_ids) {
        this.external_ids = external_ids;
    }
    public Boolean getBootstrap_ca_cert() {
        return bootstrap_ca_cert;
    }
    public void setBootstrap_ca_cert(Boolean bootstrap_ca_cert) {
        this.bootstrap_ca_cert = bootstrap_ca_cert;
    }
    public String getCertificate() {
        return certificate;
    }
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
    public String getPrivate_key() {
        return private_key;
    }
    public void setPrivate_key(String private_key) {
        this.private_key = private_key;
    }

    @Override
    public Name<SSL> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "SSL [ca_cert=" + ca_cert + ", bootstrap_ca_cert="
                + bootstrap_ca_cert + ", certificate=" + certificate
                + ", private_key=" + private_key + ", external_ids="
                + external_ids + "]";
    }

    public enum Column implements org.opendaylight.ovsdb.table.internal.Column<SSL> {
        ca_cert,
        bootstrap_ca_cert,
        certificate,
        private_key,
        external_ids}
}
