package org.opendaylight.ovsdb.lib.notation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import org.opendaylight.ovsdb.lib.notation.json.ConditionSerializer;
import org.opendaylight.ovsdb.lib.notation.json.Converter;
import org.opendaylight.ovsdb.lib.notation.json.OvsDBMapSerializer;

import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(converter = Converter.MapConverter.class)
@JsonSerialize(using = OvsDBMapSerializer.class)
public class OvsDBMap<K, V> extends ForwardingMap<K, V> {

    HashMap<K, V> target = Maps.newHashMap();

    @Override
    public Map<K, V> delegate() {
        return target;
    }
}
