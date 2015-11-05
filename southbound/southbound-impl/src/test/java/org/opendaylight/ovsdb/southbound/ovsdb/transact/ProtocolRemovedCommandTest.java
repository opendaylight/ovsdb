package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;


@PrepareForTest({ProtocolRemovedCommand.class, TyperUtils.class,TransactUtils.class})
@RunWith(PowerMockRunner.class)
public class ProtocolRemovedCommandTest {


	@Mock private static ProtocolRemovedCommand  protocolRemovedCommand;
	@Mock private Set<InstanceIdentifier<ProtocolEntry>> removed;
	@Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
	@Mock private Map<InstanceIdentifier<ProtocolEntry>, ProtocolEntry> operationalProtocolEntries;
	@Mock private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updatedBridges;
	@Mock private ProtocolEntry protocolEntry;
	@Mock private OvsdbBridgeAugmentation ovsdbBridge;
	@Mock private Bridge bridge;
	@Mock private TyperUtils typerUtils;


	@Before
	public  void setUpBeforeClass() throws Exception {
		ProtocolRemovedCommand protocolRemovedCommand = mock (ProtocolRemovedCommand.class, Mockito.CALLS_REAL_METHODS);


	}

	@Test
	public void setProtocolRemovedCommand() {

		PowerMockito.mockStatic(TransactUtils.class);
		when(TransactUtils.extractRemoved(changes, ProtocolEntry.class)).thenReturn(removed);
		when(TransactUtils.extractOriginal(changes, ProtocolEntry.class)).thenReturn(operationalProtocolEntries);
		when(TransactUtils.extractCreatedOrUpdatedOrRemoved(changes, OvsdbBridgeAugmentation.class)).thenReturn(updatedBridges);
	}

	@Test
	public void testExecute() {

		TransactionBuilder transaction = mock (TransactionBuilder.class, Mockito.RETURNS_MOCKS);
		ovsdbBridge = mock(OvsdbBridgeAugmentation.class);
		Optional<ProtocolEntry> protocolEntryOptional = mock(Optional.class);
		when (protocolEntryOptional.isPresent()).thenReturn(true);
		protocolEntry = mock(ProtocolEntry.class);
		when(protocolEntryOptional.get()).thenReturn(protocolEntry);
		PowerMockito.mockStatic(TyperUtils.class);
		PowerMockito.when(TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class)).thenReturn(bridge);

	}
}
