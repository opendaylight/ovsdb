/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.lib.database;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;


public class OvsdbType {
    public enum PortType {
        VLAN("vlan"),
        TUNNEL("Tunnel"),
        BONDING("Bonding"),
        PATCH("patch"),
        INTERNAL("internal");

        private PortType(String name) {
            this.name = name;
        }

        private String name;

        public String toString() {
            return name;
        }
    }
    public OvsdbType(String type){
        this.key = new BaseType(type);
    }

    public OvsdbType(@JsonProperty("key") BaseType key, @JsonProperty("value") BaseType value,
            @JsonProperty("min") Integer min, @JsonProperty("max") Object max){
        this.key = key;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public BaseType key;
    public BaseType value;
    public Integer min;
    public Object max;

    public static class BaseType{

        public BaseType(String type){
            this.type = type;
        }

        public BaseType(@JsonProperty("type") String type, @JsonProperty("enum") Object[] ovsdbEnum,
                @JsonProperty("minInteger") Integer minInteger, @JsonProperty("maxInteger") Integer maxInteger,
                @JsonProperty("minReal") Double minReal, @JsonProperty("maxReal") Double maxReal,
                @JsonProperty("minLength") Integer minLength, @JsonProperty("maxLength") Integer maxLength,
                @JsonProperty("refTable") String refTable, @JsonProperty("refType") String refType){
            this.type = type;
            this.ovsdbEnum = ovsdbEnum;
            this.minInteger = minInteger;
            this.maxInteger = maxInteger;
            this.minReal = minReal;
            this.maxReal = maxReal;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.refTable = refTable;
            this.refType = refType;
        }

        public String type;
        public Object[] ovsdbEnum;
        public Integer minInteger;
        public Integer maxInteger;
        public Double minReal;
        public Double maxReal;
        public Integer minLength;
        public Integer maxLength;
        public String refTable;
        public String refType;
        @Override
        public String toString() {
            return "BaseType [type=" + type + ", ovsdbEnum="
                    + Arrays.toString(ovsdbEnum) + ", minInteger=" + minInteger
                    + ", maxInteger=" + maxInteger + ", minReal=" + minReal
                    + ", maxReal=" + maxReal + ", minLength=" + minLength
                    + ", maxLength=" + maxLength + ", refTable=" + refTable
                    + ", refType=" + refType + "]";
        }
    }

    @Override
    public String toString() {
        return "OvsdbType [key=" + key + ", value=" + value + ", min=" + min
                + ", max=" + max + "]";
    }
}
