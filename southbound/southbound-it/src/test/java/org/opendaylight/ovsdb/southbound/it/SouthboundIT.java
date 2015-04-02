package org.opendaylight.ovsdb.southbound.it;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class SouthboundIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundIT.class);
    private static final String SOUTHBOUND = "org.opendaylight.ovsdb.southbound-impl";
    static Boolean writeStatus = false;
    static Boolean readStatus = false;
    static Boolean deleteStatus = false;

    @Configuration
    public Option[] config() {
        return super.config();
    }

    @Override
    public String getModuleName() {
        return "southbound-impl";
    }

    @Override
    public String getInstanceName() {
        return "southbound-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("southbound-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-southbound-impl-ui";
    }

    @Test
    public void addRemoveOvsdbNodeTest() throws InterruptedException {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName("192.168.120.31");
        } catch (UnknownHostException e) {
            fail("Could not allocate InetAddress: " + e);
        }

        IpAddress address = SouthboundMapper.createIpAddress(inetAddress);
        PortNumber port = new PortNumber(6640);

        OvsdbClientKey ovsdbClientKey = new OvsdbClientKey(address, port);

        // Write OVSDB node to configuration
        DataBroker dataBroker = getSession().getSALService(DataBroker.class);
        final ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbClientKey.toInstanceIndentifier(),
                SouthboundMapper.createNode(ovsdbClientKey));
        Futures.addCallback(transaction.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("success writing node to configuration: " + transaction);
                writeStatus = true;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                fail("failed writing node to configuration: " + transaction);
            }
        });

        Thread.sleep(1000);

        assertTrue("Failed to write node to configuration", writeStatus);

        // Read from operational to verify if the OVSDB node is connected
        final ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        ListenableFuture<Optional<Node>> dataFuture = readOnlyTransaction.read(
                LogicalDatastoreType.OPERATIONAL, ovsdbClientKey.toInstanceIndentifier());
        Futures.addCallback(dataFuture, new FutureCallback<Optional<Node>>() {
            @Override
            public void onSuccess(final Optional<Node> result) {
                LOG.info("success reading node from operational: " + readOnlyTransaction);
                readStatus = true;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                fail("failed reading node from operational: " + readOnlyTransaction);
            }
        });

        Thread.sleep(1000);

        assertTrue("Failed to read node from operational", readStatus);

        // Delete OVSDB node from configuration
        final ReadWriteTransaction deleteTransaction = dataBroker.newReadWriteTransaction();
        deleteTransaction.delete(LogicalDatastoreType.CONFIGURATION, ovsdbClientKey.toInstanceIndentifier());
        Futures.addCallback(deleteTransaction.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("success deleting node from configuration: " + deleteTransaction);
                deleteStatus = true;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                fail("failed deleting node from configuration: " + deleteTransaction);
            }
        });

        Thread.sleep(1000);

        assertTrue("Failed to delete node from configuration", deleteStatus);
    }
}
