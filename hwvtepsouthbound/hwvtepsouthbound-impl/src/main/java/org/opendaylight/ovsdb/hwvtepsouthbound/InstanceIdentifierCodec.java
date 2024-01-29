/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.Optional;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.util.AbstractStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextListener;
import org.opendaylight.yangtools.yang.model.api.Module;

public final class InstanceIdentifierCodec
        // FIXME: this really wants to be wired as yangtools-data-codec-gson's codecs, because ...
        extends AbstractStringInstanceIdentifierCodec implements EffectiveModelContextListener {

    // FIXME: this is not the only interface exposed from binding-dom-codec-api, something different might be more
    //        appropriate.
    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    private DataSchemaContextTree dataSchemaContextTree;
    private EffectiveModelContext context;

    public InstanceIdentifierCodec(final DOMSchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer) {
        schemaService.registerSchemaContextListener(this);
        this.bindingNormalizedNodeSerializer = bindingNormalizedNodeSerializer;
    }

    // ... all of this is dynamic lookups based on injection. OSGi lifecycle gives us this for free ...
    @Override
    protected DataSchemaContextTree getDataContextTree() {
        return dataSchemaContextTree;
    }

    @Override
    protected Module moduleForPrefix(final String prefix) {
        return context != null ? context.findModule(prefix, Optional.empty()).orElse(null) : null;
    }

    @Override
    protected String prefixForNamespace(final XMLNamespace namespace) {
        return context != null ? context.findModule(namespace, Optional.empty()).map(Module::getName).orElse(null)
                : null;
    }

    @Override
    public void onModelContextUpdated(final EffectiveModelContext schemaContext) {
        this.context = schemaContext;
        this.dataSchemaContextTree = DataSchemaContextTree.from(schemaContext);
    }

    // FIXME: ... and then this is a separate service built on top of dynamic lifecycle.
    public String serialize(final InstanceIdentifier<?> iid) {
        YangInstanceIdentifier normalizedIid = bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid);
        return serialize(normalizedIid);
    }

    public YangInstanceIdentifier getYangInstanceIdentifier(final InstanceIdentifier<?> iid) {
        return bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid);
    }

    public InstanceIdentifier<?> bindingDeserializer(final String iidString) throws DeserializationException {
        YangInstanceIdentifier normalizedYangIid = deserialize(iidString);
        return bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(normalizedYangIid);
    }

    public InstanceIdentifier<?> bindingDeserializer(final YangInstanceIdentifier yangIID) {
        return bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(yangIID);
    }
}
