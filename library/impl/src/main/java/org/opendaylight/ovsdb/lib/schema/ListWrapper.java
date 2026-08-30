/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
final class ListWrapper<E> extends AbstractList<E> {
    private final Set<E> set;

    ListWrapper(final Set<E> set) {
        this.set = requireNonNull(set);
    }

    @Override
    public Iterator<E> iterator() {
        return Iterators.unmodifiableIterator(set.iterator());
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Spliterator<E> spliterator() {
        return set.spliterator();
    }

    @Override
    public Stream<E> parallelStream() {
        return set.parallelStream();
    }

    @Override
    public Stream<E> stream() {
        return set.stream();
    }

    @Override
    public E get(final int index) {
        return Iterables.get(set, index);
    }
}
