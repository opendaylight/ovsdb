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
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;

import org.opendaylight.ovsdb.lib.notation.json.Converter;
import org.opendaylight.ovsdb.lib.notation.json.OvsDBSetSerializer;

import java.util.Set;

@JsonDeserialize(converter = Converter.SetConverter.class)
@JsonSerialize(using = OvsDBSetSerializer.class)
public class OvsDBSet<T> extends ForwardingSet<T> {

    Set<T> target = Sets.newHashSet();

    @Override
    public Set<T> delegate() {
        return target;
    }
}
