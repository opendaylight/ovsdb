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
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;

import org.opendaylight.ovsdb.lib.notation.json.Converter;
import org.opendaylight.ovsdb.lib.notation.json.OvsdbSetSerializer;

import java.util.Set;

@JsonDeserialize(converter = Converter.SetConverter.class)
@JsonSerialize(using = OvsdbSetSerializer.class)
public class OvsdbSet<T> extends ForwardingSet<T> {

    Set<T> target = null;

    public OvsdbSet() {
        this(Sets.<T>newHashSet());
    }

    public OvsdbSet(Set<T> backing) {
        this.target = backing;
    }

    @Override
    public Set<T> delegate() {
        return target;
    }

    public static <D> OvsdbSet<D> fromSet(Set<D> value) {
        return new OvsdbSet<>(value);
    }
}
