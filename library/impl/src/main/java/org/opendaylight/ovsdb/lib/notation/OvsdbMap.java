/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ForwardingMap;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.json.Converter;
import org.opendaylight.ovsdb.lib.notation.json.OvsdbMapSerializer;

@JsonDeserialize(converter = Converter.MapConverter.class)
@JsonSerialize(using = OvsdbMapSerializer.class)
public class OvsdbMap<K, V> extends ForwardingMap<K, V> {

    private final Map<K, V> target = new HashMap<>();

    public OvsdbMap() {}

    public OvsdbMap(Map<K, V> value) {
        this.target.putAll(value);
    }

    @Override
    public Map<K, V> delegate() {
        return target;
    }

    public static <K,V> OvsdbMap<K,V> fromMap(Map<K, V> value) {
        return new OvsdbMap<>(value);
    }
}
