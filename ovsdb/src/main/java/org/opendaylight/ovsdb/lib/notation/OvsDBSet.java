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
