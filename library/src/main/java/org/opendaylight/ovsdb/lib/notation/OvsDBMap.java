/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.notation;

import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.json.Converter;
import org.opendaylight.ovsdb.lib.notation.json.OvsDBMapSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

@JsonDeserialize(converter = Converter.MapConverter.class)
@JsonSerialize(using = OvsDBMapSerializer.class)
public class OvsDBMap<K, V> extends ForwardingMap<K, V> {

    Map<K, V> target = Maps.newHashMap();

    public OvsDBMap() {
        this(Maps.<K,V>newHashMap());
    }

    public OvsDBMap(Map<K, V> value) {
        this.target = value;
    }

    @Override
    public Map<K, V> delegate() {
        return target;
    }

    public static<K,V> OvsDBMap<K,V> fromMap(Map<K, V> value) {
        return new OvsDBMap<K,V>(value);
    }
}
