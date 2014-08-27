package org.opendaylight.ovsdb.ovssfc;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInputBuilder;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunction;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
//import org.opendaylight.yangtools.yang.common.RpcResult;
//import org.opendaylight.yangtools.yang.binding.DataObject;
//import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import ch.qos.logback.classic.Level;

import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.when;

public class EventHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(EventHandler.class);
    DataBroker dataBroker;
    DataChangeEvent dataChangeEvent;
    OvsSfcProvider sfcProvider;

    @Before
    public void setUp() {
        dataBroker = mock(DataBroker.class);
        dataChangeEvent = mock(DataChangeEvent.class);
        sfcProvider = new OvsSfcProvider(dataBroker);

        //ch.qos.logback.classic.Logger rootLogger =
        //        (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        //rootLogger.setLevel(Level.toLevel("trace"));
    }

    @After
    public void tearDown() throws Exception {
        //System.out.println(">>>>>Running test stop 1");
        //sfcProvider.eventHandler.shutdownNow();
        sfcProvider.eventHandler.stop();
        //System.out.println(">>>>>Running test stop 2");
    }

    @Ignore
    @Test(timeout=1000)
    public void testSfpUpdate() throws Exception {
        //ch.qos.logback.classic.Logger rootLogger =
        //        (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        //rootLogger.setLevel(Level.toLevel("trace"));

        List<SfDataPlaneLocator> sfDpLList = new ArrayList<>();
        sfDpLList.add(putSf("dpi-1", "dpi", "10.1.1.1", "10.1.1.1", 10000).build());
        sfDpLList.add(putSf("napt44-1", "napt44", "10.1.1.2", "10.1.1.2", 10000).build());
/* model changed and broke tests
        SfpServiceFunctionBuilder sfBuilder = new SfpServiceFunctionBuilder();
        List<SfpServiceFunction> sfList = new ArrayList<>();
        sfBuilder.setName("sf1");
        sfBuilder.setServiceIndex((short)3);
        sfBuilder.setServiceFunctionForwarder("sff1");
        sfList.add(sfBuilder.build());
        sfBuilder.setName("sf2");
        sfBuilder.setServiceIndex((short)2);
        sfBuilder.setServiceFunctionForwarder("sff1");
        sfList.add(sfBuilder.build());
*/
        ServiceFunctionPathBuilder sfpBuilder = new ServiceFunctionPathBuilder();
        sfpBuilder.setName("sfp1");
        sfpBuilder.setServiceChainName("sfp1");
        sfpBuilder.setPathId(1L);
        //sfpBuilder.setStartingIndex((short) (sfList.size() + 1));
        //sfpBuilder.setSfpServiceFunction(sfList);

        ServiceFunctionPathsBuilder builder = new ServiceFunctionPathsBuilder();
        List<ServiceFunctionPath> sfpList = new ArrayList<>();
        sfpList.add(sfpBuilder.build());
        builder.setServiceFunctionPath(sfpList);

        //sfcProvider.eventHandler.enqueueSfpEvent(builder.build());
        Thread.sleep(100);
        System.out.println(">>>>>Running test end");
    }

    private SfDataPlaneLocatorBuilder putSf(String name, String type,
                                            String ipMgmt, String ipLocator, int portLocator) {
        logger.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        // Build Locator Type (ip and port)
        IpAddress ipAddress = new IpAddress(ipLocator.toCharArray());
        PortNumber portNumber = new PortNumber(portLocator);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder = ipBuilder.setIp(ipAddress).setPort(portNumber);

        // Build Data Plane Locator and populate with Locator Type

        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder = sfDataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        return sfDataPlaneLocatorBuilder;

/*
        // Build ServiceFunctionBuilder and set all data constructed above
        PutServiceFunctionInputBuilder putServiceFunctionInputBuilder = new PutServiceFunctionInputBuilder();
        putServiceFunctionInputBuilder = putServiceFunctionInputBuilder.setName(name).setType(type).
                setIpMgmtAddress(new IpAddress(ipMgmt.toCharArray())).
                setSfDataPlaneLocator(sfDataPlaneLocatorBuilder.build());

        try {
            Future<RpcResult<Void>> fr = sfService.putServiceFunction(putServiceFunctionInputBuilder.build());
            RpcResult<Void> result = fr.get();
            if (result != null) {
                logger.info("\n####### {} result: {}", Thread.currentThread().getStackTrace()[1], result);
                if (result.isSuccessful()) {
                    logger.info("\n####### {}: successfully finished", Thread.currentThread().getStackTrace()[1]);
                } else {
                    logger.warn("\n####### {}: not successfully finished", Thread.currentThread().getStackTrace()[1]);
                }
                return result.isSuccessful();
            } else {
                logger.warn("\n####### {} result is NULL", Thread.currentThread().getStackTrace()[1]);
                return Boolean.FALSE;
            }

        } catch (Exception e) {
            logger.warn("\n####### {} Error occurred: {}", Thread.currentThread().getStackTrace()[1], e);
            e.printStackTrace();
            return Boolean.FALSE;
        }
    }
*/
    }
}
