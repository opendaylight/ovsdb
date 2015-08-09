/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import java.net.URI;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class InstanceIdentifierCodec extends AbstractModuleStringInstanceIdentifierCodec
    implements SchemaContextListener {

    private DataSchemaContextTree dataSchemaContextTree;
    private SchemaContext context;
    private BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    public InstanceIdentifierCodec(SchemaService schemaService,
            BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer) {
        schemaService.registerSchemaContextListener(this);
        this.bindingNormalizedNodeSerializer = bindingNormalizedNodeSerializer;
    }

    @Override
    protected DataSchemaContextTree getDataContextTree() {
        return dataSchemaContextTree;
    }

    @Override
    protected Module moduleForPrefix(final String prefix) {
        return context.findModuleByName(prefix, null);
    }

    @Override
    protected String prefixForNamespace(final URI namespace) {
        final Module module = context.findModuleByNamespaceAndRevision(namespace, null);
        return module == null ? null : module.getName();
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        this.context = context;
        this.dataSchemaContextTree = DataSchemaContextTree.from(context);
    }

    public String serialize(InstanceIdentifier<?> iid) {
        YangInstanceIdentifier normalizedIid = bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid);
        return serialize(normalizedIid);
    }

    public  InstanceIdentifier<?> bindingDeserializer(String iidString) throws DeserializationException {
        YangInstanceIdentifier normalizedYangIid = deserialize(iidString);
        InstanceIdentifier<?> iid = bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(normalizedYangIid);
        return iid;
    }

}
