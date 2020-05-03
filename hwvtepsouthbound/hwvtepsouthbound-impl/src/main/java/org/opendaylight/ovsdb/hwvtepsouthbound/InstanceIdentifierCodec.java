/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.net.URI;
import java.util.Optional;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextListener;
import org.opendaylight.yangtools.yang.model.api.Module;

public class InstanceIdentifierCodec extends AbstractModuleStringInstanceIdentifierCodec
    implements EffectiveModelContextListener {

    private DataSchemaContextTree dataSchemaContextTree;
    private EffectiveModelContext context;
    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    public InstanceIdentifierCodec(final DOMSchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer) {
        schemaService.registerSchemaContextListener(this);
        this.bindingNormalizedNodeSerializer = bindingNormalizedNodeSerializer;
    }

    @Override
    protected DataSchemaContextTree getDataContextTree() {
        return dataSchemaContextTree;
    }

    @Override
    protected Module moduleForPrefix(final String prefix) {
        return context != null ? context.findModule(prefix, Optional.empty()).orElse(null) : null;
    }

    @Override
    protected String prefixForNamespace(final URI namespace) {
        return context != null ? context.findModule(namespace, Optional.empty()).map(Module::getName).orElse(null)
                : null;
    }

    @Override
    public void onModelContextUpdated(final EffectiveModelContext schemaContext) {
        this.context = schemaContext;
        this.dataSchemaContextTree = DataSchemaContextTree.from(schemaContext);
    }

    public String serialize(final InstanceIdentifier<?> iid) {
        YangInstanceIdentifier normalizedIid = bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid);
        return serialize(normalizedIid);
    }

    public YangInstanceIdentifier getYangInstanceIdentifier(final InstanceIdentifier<?> iid) {
        return bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid);
    }

    public  InstanceIdentifier<?> bindingDeserializer(final String iidString) throws DeserializationException {
        YangInstanceIdentifier normalizedYangIid = deserialize(iidString);
        return bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(normalizedYangIid);
    }

    public InstanceIdentifier<?> bindingDeserializer(final YangInstanceIdentifier yangIID) {
        return bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(yangIID);
    }
}
