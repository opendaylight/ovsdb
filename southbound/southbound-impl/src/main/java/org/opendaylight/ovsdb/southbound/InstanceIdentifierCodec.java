/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextListener;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceIdentifierCodec extends AbstractModuleStringInstanceIdentifierCodec
        implements EffectiveModelContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceIdentifierCodec.class);

    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    private DataSchemaContextTree dataSchemaContextTree;
    private SchemaContext context;

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
        return context.findModules(prefix).stream().findFirst().orElse(null);
    }

    @Override
    protected String prefixForNamespace(final XMLNamespace namespace) {
        return context.findModules(namespace).stream().map(Module::getName).findFirst().orElse(null);
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

    public InstanceIdentifier<?> bindingDeserializer(final String iidString) throws DeserializationException {
        YangInstanceIdentifier normalizedYangIid = deserialize(iidString);
        return bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(normalizedYangIid);
    }

    public InstanceIdentifier<?> bindingDeserializer(final YangInstanceIdentifier yangIID) {
        return bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(yangIID);
    }

    public InstanceIdentifier<?> bindingDeserializerOrNull(final String iidString) {
        try {
            return bindingDeserializer(iidString);
        } catch (DeserializationException e) {
            LOG.warn("Unable to deserialize iidString", e);
        }
        return null;
    }
}
