package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({AbstractModuleStringInstanceIdentifierCodec.class, DataSchemaContextTree.class})
@RunWith(PowerMockRunner.class)

public class InstanceIdentifierCodecTest {

    private InstanceIdentifierCodec instanceIdentifierCodec;
    @Mock private DataSchemaContextTree dataSchemaContextTree;
    @Mock private SchemaContext context;
    @Mock private BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;
    @Mock private SchemaService schemaService;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException {
        instanceIdentifierCodec = mock(InstanceIdentifierCodec.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(InstanceIdentifierCodec.class, "dataSchemaContextTree").set(instanceIdentifierCodec, dataSchemaContextTree);
        MemberModifier.field(InstanceIdentifierCodec.class, "context").set(instanceIdentifierCodec, context);
        MemberModifier.field(InstanceIdentifierCodec.class, "bindingNormalizedNodeSerializer").set(instanceIdentifierCodec, bindingNormalizedNodeSerializer);

    }
    @Test
    public void testInstanceIdentifierCodec() throws Exception {
        InstanceIdentifierCodec codec = new InstanceIdentifierCodec(schemaService, bindingNormalizedNodeSerializer);
        verify(schemaService).registerSchemaContextListener(codec);
    }

    @Test
    public void testGetDataContextTree() {
        assertEquals("Error, did not return correct DataSchemaContextTree object", dataSchemaContextTree, instanceIdentifierCodec.getDataContextTree());
    }

    @Test
    public void testModuleForPrefix() {
        Module module = mock(Module.class);
        when(context.findModuleByName(anyString(),any(Date.class))).thenReturn(module);
        assertEquals("Error, did not return correct Module object", module, instanceIdentifierCodec.moduleForPrefix(""));
    }

    @Test
    public void testPrefixForNamespace() throws URISyntaxException {
        Module module = mock(Module.class);
        URI namespace = new URI("");
        when(context.findModuleByNamespaceAndRevision(any(URI.class), any(Date.class))).thenReturn(null).thenReturn(module);
        when(module.getName()).thenReturn("");
        assertEquals("Error, null should have been returned", null, instanceIdentifierCodec.prefixForNamespace(namespace));
        assertEquals("Error, did not return the correct module name", anyString(), instanceIdentifierCodec.prefixForNamespace(namespace));
    }

    @Test
    public void testOnGlobalContextUpdated() {
        PowerMockito.mockStatic(DataSchemaContextTree.class);
        when(DataSchemaContextTree.from(any(SchemaContext.class))).thenReturn(dataSchemaContextTree);
        instanceIdentifierCodec.onGlobalContextUpdated(context);
        verify(instanceIdentifierCodec).onGlobalContextUpdated(context);
    }

    @Test
    public void testSerialize() {
        InstanceIdentifier<?> iid = mock(InstanceIdentifier.class);
        YangInstanceIdentifier yIid = mock(YangInstanceIdentifier.class);
        when(bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid)).thenReturn(yIid);

        when((PowerMockito.mock(AbstractModuleStringInstanceIdentifierCodec.class)).serialize(yIid)).thenReturn("Serialized IID");
        assertEquals("Error, did not return correct string", anyString(), instanceIdentifierCodec.serialize(iid));
    }

    @Test
    public void testBindingDeserializer() throws Exception {
        YangInstanceIdentifier yIid = mock(YangInstanceIdentifier.class);
        when((PowerMockito.mock(AbstractModuleStringInstanceIdentifierCodec.class)).deserialize(anyString())).thenReturn(yIid);

        mock(InstanceIdentifier.class);
        when(bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(yIid)).thenAnswer(new Answer<InstanceIdentifier<?>>() {
            public InstanceIdentifier<?> answer(InvocationOnMock invocation) throws Throwable {
                return (InstanceIdentifier<?>) invocation.getArguments() [0];
            }
        });

        assertEquals("Error, did not return correct InstanceIdentifier<?> object", any(InstanceIdentifier.class), instanceIdentifierCodec.bindingDeserializer(""));
    }
}
