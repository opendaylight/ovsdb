/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getField;

import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@RunWith(MockitoJUnitRunner.class)
public class InstanceIdentifierCodecTest {

    private InstanceIdentifierCodec instanceIdCodec;
    private DataSchemaContextTree dataSchemaContextTree;

    @Mock
    private SchemaContext context;
    @Mock
    private BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;
    @Mock
    private DOMSchemaService schemaService;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException {
        when(context.getQName()).thenReturn(SchemaContext.NAME);
        dataSchemaContextTree = DataSchemaContextTree.from(context);

        instanceIdCodec = mock(InstanceIdentifierCodec.class, Mockito.CALLS_REAL_METHODS);
        getField(InstanceIdentifierCodec.class, "dataSchemaContextTree").set(instanceIdCodec, dataSchemaContextTree);
        getField(InstanceIdentifierCodec.class, "context").set(instanceIdCodec, context);
        getField(InstanceIdentifierCodec.class, "bindingNormalizedNodeSerializer").set(instanceIdCodec,
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
        when(context.findModules("")).thenReturn(ImmutableSet.of(module));
        assertEquals("Found Module", module, instanceIdCodec.moduleForPrefix(""));

        when(context.findModules("foo")).thenReturn(ImmutableSet.of(module, mock(Module.class)));
        assertEquals("Found Module", module, instanceIdCodec.moduleForPrefix("foo"));

        when(context.findModules("bar")).thenReturn(Collections.emptySet());
        assertNull(instanceIdCodec.moduleForPrefix("bar"));
    }

    @Test
    public void testPrefixForNamespace() throws URISyntaxException {
        Module module = mock(Module.class);
        final String prefix = "prefix";
        when(module.getName()).thenReturn(prefix);

        URI namespace = new URI("foo");
        when(context.findModules(namespace)).thenReturn(Collections.emptySet());
        assertNull(instanceIdCodec.prefixForNamespace(namespace));

        when(context.findModules(namespace)).thenReturn(ImmutableSet.of(module));
        assertEquals("Found prefix", prefix, instanceIdCodec.prefixForNamespace(namespace));

        when(context.findModules(namespace)).thenReturn(ImmutableSet.of(module, mock(Module.class)));
        assertEquals("Found prefix", prefix, instanceIdCodec.prefixForNamespace(namespace));
    }

    @Test
    public void testSerialize() {
        InstanceIdentifier<?> iid = mock(InstanceIdentifier.class);
        YangInstanceIdentifier yiid = mock(YangInstanceIdentifier.class);
        when(bindingNormalizedNodeSerializer.toYangInstanceIdentifier(iid)).thenReturn(yiid);
        assertEquals("Error, did not return correct string", "", instanceIdCodec.serialize(iid));
    }

    @Test
    public void testBindingDeserializer() throws Exception {
        YangInstanceIdentifier yiid = mock(YangInstanceIdentifier.class);
        when(bindingNormalizedNodeSerializer.fromYangInstanceIdentifier(yiid)).thenAnswer(
                (Answer<InstanceIdentifier<?>>) invocation -> (InstanceIdentifier<?>) invocation.getArguments()[0]);

        assertNull("Error, did not return correct InstanceIdentifier<?> object",
            instanceIdCodec.bindingDeserializer(""));
    }
}
