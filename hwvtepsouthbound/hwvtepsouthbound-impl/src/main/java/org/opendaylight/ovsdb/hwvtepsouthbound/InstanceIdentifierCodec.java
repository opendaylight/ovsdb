/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.Optional;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.util.AbstractStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.util.LeafrefResolver;

// FIXME: this really wants to be wired as yangtools-data-codec-gson's codecs, because ...
public final class InstanceIdentifierCodec extends AbstractStringInstanceIdentifierCodec {
    // FIXME: this is not the only interface exposed from binding-dom-codec-api, something different might be more
    //        appropriate.
    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    private DataSchemaContextTree dataSchemaContextTree;
    private EffectiveModelContext context;

    public InstanceIdentifierCodec(final DOMSchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer) {
        schemaService.registerSchemaContextListener(this::onModelContextUpdated);
        this.bindingNormalizedNodeSerializer = bindingNormalizedNodeSerializer;
    }

    // ... all of this is dynamic lookups based on injection. OSGi lifecycle gives us this for free ...
    @Override
    protected DataSchemaContextTree getDataContextTree() {
        return dataSchemaContextTree;
    }

    @Override
    protected QNameModule moduleForPrefix(final String prefix) {
        return context != null ? context.findModule(prefix, Optional.empty()).map(Module::getQNameModule).orElse(null)
            : null;
    }

    @Override
    protected String prefixForNamespace(final XMLNamespace namespace) {
        return context != null ? context.findModule(namespace, Optional.empty()).map(Module::getName).orElse(null)
                : null;
    }

    @Override
    protected Object deserializeKeyValue(final DataSchemaNode schemaNode, final LeafrefResolver resolver,
            final String value) {
        throw new UnsupportedOperationException("Not implemented");
    }

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
        return bindingDeserializer(deserialize(iidString));
    }

    public InstanceIdentifier<?> bindingDeserializer(final YangInstanceIdentifier yangIID) {
        final var ref = bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(yangIID);
        return ref != null ? ref.toLegacy() : null;
    }
}
