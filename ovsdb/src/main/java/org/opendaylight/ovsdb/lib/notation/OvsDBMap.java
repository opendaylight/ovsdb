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
