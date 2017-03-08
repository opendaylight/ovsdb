/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.field;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AbstractModuleStringInstanceIdentifierCodec.class, DataSchemaContextTree.class })
public class InstanceIdentifierCodecTest {

    private InstanceIdentifierCodec instanceIdCodec;
    @Mock
    private DataSchemaContextTree dataSchemaContextTree;
    @Mock
    private SchemaContext context;
    @Mock
    private BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;
    @Mock
    private SchemaService schemaService;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException {
        instanceIdCodec = mock(InstanceIdentifierCodec.class, Mockito.CALLS_REAL_METHODS);
        field(InstanceIdentifierCodec.class, "dataSchemaContextTree").set(instanceIdCodec, dataSchemaContextTree);
        field(InstanceIdentifierCodec.class, "context").set(instanceIdCodec, context);
        field(InstanceIdentifierCodec.class, "bindingNormalizedNodeSerializer").set(instanceIdCodec,
                bindingNormalizedNodeSerializer);
    }

    @Test
    public void testInstanceIdentifierCodec() throws Exception {
        InstanceIdentifierCodec codec = new InstanceIdentifierCodec(schemaService, bindingNormalizedNodeSerializer);
        verify(schemaService).registerSchemaContextListener(codec);
    }

    @Test
    public void testGetDataContextTree() {
        assertEquals("Error, did not return correct DataSchemaContextTree object", dataSchemaContextTree,
                instanceIdCodec.getDataContextTree());
    }

    @Test
    public void testModuleForPrefix() {
        Module module = mock(Module.class);
        when(context.findModuleByName(anyString(), any(Date.class))).thenReturn(module);
        assertEquals("Error, did not return correct Module object", module, instanceIdCodec.moduleForPrefix(""));
    }

    @Test
    public void testPrefixForNamespace() throws URISyntaxException {
        Module module = mock(Module.class);
        URI namespace = new URI("");
        when(context.findModuleByNamespaceAndRevision(any(URI.class), any(Date.class))).thenReturn(null)
                .thenReturn(module);
        when(module.getName()).thenReturn("");
        assertEquals("Error, null should have been returned", null, instanceIdCodec.prefixForNamespace(namespace));
        assertEquals("Error, did not return the correct module name", anyString(),
                instanceIdCodec.prefixForNamespace(namespace));
    }

    @Test
    public void testOnGlobalContextUpdated() {
        PowerMockito.mockStatic(DataSchemaContextTree.class);
        when(DataSchemaContextTree.from(any(SchemaContext.class))).thenReturn(dataSchemaContextTree);
        instanceIdCodec.onGlobalContextUpdated(context);
        verify(instanceIdCodec).onGlobalContextUpdated(context);
    }

    @Test
    public void testSerialize() {
        InstanceIdentifier<?> iid = mock(InstanceIdentifier.class);
        YangInstanceIdentifier yiid = mock(YangInstanceIdentifier.class);
        when(bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid)).thenReturn(yiid);

        when((PowerMockito.mock(AbstractModuleStringInstanceIdentifierCodec.class)).serialize(yiid))
                .thenReturn("Serialized IID");
        assertEquals("Error, did not return correct string", anyString(), instanceIdCodec.serialize(iid));
    }

    @Test
    public void testBindingDeserializer() throws Exception {
        YangInstanceIdentifier yiid = mock(YangInstanceIdentifier.class);
        when((PowerMockito.mock(AbstractModuleStringInstanceIdentifierCodec.class)).deserialize(anyString()))
                .thenReturn(yiid);

        mock(InstanceIdentifier.class);
        when(bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(yiid)).thenAnswer(
                (Answer<InstanceIdentifier<?>>) invocation -> (InstanceIdentifier<?>) invocation.getArguments()[0]);

        assertEquals("Error, did not return correct InstanceIdentifier<?> object", any(InstanceIdentifier.class),
                instanceIdCodec.bindingDeserializer(""));
    }
}
