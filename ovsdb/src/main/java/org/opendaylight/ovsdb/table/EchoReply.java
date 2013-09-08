package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "result",
        "id"
})
public class EchoReply {

    public EchoReply() {
        this.id = "echo";
    }

    @JsonProperty("result")
    private List<Object> result = new ArrayList<Object>();
    @JsonProperty("id")
    private String id;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("result")
    public List<Object> getResult() {
        return result;
    }

    @JsonProperty("result")
    public void setResult(List<Object> result) {
        this.result = result;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = "echo";
    }

    @Override
    public String toString() {
        return result + id ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EchoReply echoReply = (EchoReply) o;

        if (!additionalProperties.equals(echoReply.additionalProperties)) return false;
        if (!id.equals(echoReply.id)) return false;
        if (!result.equals(echoReply.result)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = result.hashCode();
        result1 = 31 * result1 + id.hashCode();
        result1 = 31 * result1 + additionalProperties.hashCode();
        return result1;
    }
}