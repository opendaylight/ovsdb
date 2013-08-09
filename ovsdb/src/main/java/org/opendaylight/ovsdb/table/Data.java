package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
/*
 * Adding as a root reference,table
 * POJOs will likely be called individually
*/

public class Data {

    @JsonProperty("Port")
    private Port Port;
    @JsonProperty("Controller")
    private Controller Controller;
    @JsonProperty("Interface")
    private Interface Interface;
    @JsonProperty("OpenvSwitch")
    private OvsTable OvsTable;
    @JsonProperty("Manager")
    private Manager Manager;
    @JsonProperty("Bridge")
    private Bridge Bridge;

    private Port getPort() {
        return Port;
    }

    private void setPort(Port port) {
        Port = port;
    }

    private Controller getController() {
        return Controller;
    }

    private void setController(Controller controller) {
        Controller = controller;
    }

    private Interface getInterface() {
        return Interface;
    }

    private void setInterface(Interface anInterface) {
        Interface = anInterface;
    }

    private OvsTable getOvsTable() {
        return OvsTable;
    }

    private void setOvsTable(OvsTable ovsTable) {
        OvsTable = ovsTable;
    }

    private Manager getManager() {
        return Manager;
    }

    private void setManager(Manager manager) {
        Manager = manager;
    }

    private Bridge getBridge() {
        return Bridge;
    }

    private void setBridge(Bridge bridge) {
        Bridge = bridge;
    }

    @Override
    public String toString() {
        return "Data{" +
                "port=" + Port +
                ", Bridge=" + Bridge +
                ", Interface=" + Interface +
                ", OvsTable=" + OvsTable +
                ", Controller=" + Controller +
                ", Manager=" + Manager +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((Bridge == null) ? 0 : Bridge.hashCode());
        result = prime * result
                + ((Controller == null) ? 0 : Controller.hashCode());
        result = prime * result
                + ((Interface == null) ? 0 : Interface.hashCode());
        result = prime * result + ((Manager == null) ? 0 : Manager.hashCode());
        result = prime * result
                + ((OvsTable == null) ? 0 : OvsTable.hashCode());
        result = prime * result + ((Port == null) ? 0 : Port.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Data other = (Data) obj;
        if (Bridge == null) {
            if (other.Bridge != null)
                return false;
        } else if (!Bridge.equals(other.Bridge))
            return false;
        if (Controller == null) {
            if (other.Controller != null)
                return false;
        } else if (!Controller.equals(other.Controller))
            return false;
        if (Interface == null) {
            if (other.Interface != null)
                return false;
        } else if (!Interface.equals(other.Interface))
            return false;
        if (Manager == null) {
            if (other.Manager != null)
                return false;
        } else if (!Manager.equals(other.Manager))
            return false;
        if (OvsTable == null) {
            if (other.OvsTable != null)
                return false;
        } else if (!OvsTable.equals(other.OvsTable))
            return false;
        if (Port == null) {
            if (other.Port != null)
                return false;
        } else if (!Port.equals(other.Port))
            return false;
        return true;
    }

}