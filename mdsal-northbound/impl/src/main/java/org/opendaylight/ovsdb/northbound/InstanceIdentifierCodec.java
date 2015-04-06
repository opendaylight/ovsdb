package org.opendaylight.ovsdb.northbound;

import java.net.URI;

import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class InstanceIdentifierCodec  extends AbstractModuleStringInstanceIdentifierCodec
    implements SchemaContextListener{

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected Module moduleForPrefix(String prefix) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected DataSchemaContextTree getDataContextTree() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String prefixForNamespace(URI namespace) {
        // TODO Auto-generated method stub
        return null;
    }

}
