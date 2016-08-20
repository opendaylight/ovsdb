/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.ovsdb.it.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.junit.Assert;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;

/**
 * Run OVS(s) using docker-compose for use in integration tests.
 * For example,
 * <pre>
 * try(DockerOvs ovs = new DockerOvs()) {
 *      ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(
 *                               ovs.getOvsdbAddress(0), ovs.getOvsdbPort(0));
 *      ...
 *       nodeInfo.disconnect();
 *
 * } catch (Exception e) {
 * ...
 * </pre>
 * <b>
 * Nota bene, DockerOvs will check whether or not docker-compose command requires "sudo"
 * to run. However, if it does require sudo, it must be configured to not prompt for a
 * password ("NOPASSWD: ALL" is the sudoers file).
 * </b>
 * DockerOvs loads its docker-compose yaml files from inside the ovsdb-it-utils bundle
 * at the path META-INF/docker-compose-files/. Currently, a single yaml file is used,
 * "docker-ovs-2.5.1.yml." DockerOvs does support docker-compose files that
 * launch more than one docker image, more on this later. DockerOvs will wait for OVS
 * to accept OVSDB connections.
 * Any docker-compose file must have a port mapping.
 *
 * The following explains how system properties are used to configure DockerOvs
 * <pre>
 *  private static String ENV_USAGE =
 *  "-Ddocker.run - explicitly configure whether or not DockerOvs should run docker-compose\n" +
 *  "-Dovsdbserver.ipaddress - specify IP address of ovsdb server - implies -Ddocker.run=false\n" +
 *  "-Dovsdbserver.port - specify the port of the ovsdb server - required with -Dovsdbserver.ipaddress\n" +
 *  "-Ddocker.compose.file - docker compose file in META-INF/docker-compose-files/. If not specified, default file is used\n" +
 *  "-Dovsdb.userspace.enabled - true when Ovs is running in user space (usually the case with docker)\n" +
 *  "-Dovsdb.controller.address - IP address of the controller (usually the docker0 interface with docker)\n" +
 *  "To auto-run Ovs and connect actively:\n" +
 *  " -Dovsdb.controller.address=x.x.x.x -Dovsdb.userspace.enabled=yes [-Ddocker.compose.file=ffff]\n" +
 *  "To auto-run Ovs and connect passively:\n" +
 *  " -Dovsdbserver.connection=passive -Dovsdb.controller.address=x.x.x.x -Dovsdb.userspace.enabled=yes [-Ddocker.compose.file=ffff]\n" +
 *  "To actively connect to a running Ovs:\n" +
 *  " -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=6641 -Dovsdb.controller.address=y.y.y.y\n" +
 *  "To passively connect to a running Ovs:\n" +
 *  " -Dovsdbserver.connection=passive -Ddocker.run=false\n";
 * </pre>
 * When DockerOvs does not run docker-compose getOvsdbAddress and getOvsdbPort return the address and port specified in
 * the system properties.
 */
public class DockerOvs implements AutoCloseable {
    private static String ENV_USAGE = "Usage:\n" +
            "-Ddocker.run - explicitly configure whether or not DockerOvs should run docker-compose\n" +
                    "-Dovsdbserver.ipaddress - specify IP address of ovsdb server - implies -Ddocker.run=false\n" +
                    "-Dovsdbserver.port - specify the port of the ovsdb server - required with -Dovsdbserver.ipaddress\n" +
                    "-Ddocker.compose.file - docker compose file in META-INF/docker-compose-files/. If not specified, default file is used\n" +
                    "-Dovsdb.userspace.enabled - true when Ovs is running in user space (usually the case with docker)\n" +
                    "-Dovsdb.controller.address - IP address of the controller (usually the docker0 interface with docker)\n" +
                    "To auto-run Ovs and connect actively:\n" +
                    " -Dovsdb.controller.address=x.x.x.x -Dovsdb.userspace.enabled=yes <-Ddocker.compose.file=ffff>\n" +
                    "To auto-run Ovs and connect passively:\n" +
                    " -Dovsdbserver.connection=passive -Dovsdb.controller.address=x.x.x.x -Dovsdb.userspace.enabled=yes <-Ddocker.compose.file=ffff>\n" +
                    "To actively connect to a running Ovs:\n" +
                    " -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=6641 -Dovsdb.controller.address=y.y.y.y\n" +
                    "To passively connect to a running Ovs:\n" +
                    " -Dovsdbserver.connection=passive -Ddocker.run=false\n";

    private static final Logger LOG = LoggerFactory.getLogger(DockerOvs.class);
    private static final String DEFAULT_DOCKER_FILE = "ovs-2.5.0-hwvtep.yml";
    private static final String DOCKER_FILE_PATH = "META-INF/docker-compose-files/";
    private static final int COMPOSE_FILE_IDX = 3;
    private static final int COMPOSE_FILE_IDX_NO_SUDO = 2;
    private static final String DEFAULT_OVSDB_HOST = "127.0.0.1";

    private String[] psCmd = {"sudo", "docker-compose", "-f", null, "ps"};
    private String[] psCmdNoSudo = {"docker-compose", "-f", null, "ps"};
    private String[] upCmd = {"sudo", "docker-compose", "-f", null, "up", "-d", "--force-recreate"};
    private String[] downCmd = {"sudo", "docker-compose", "-f", null, "stop"};
    private String[] execCmd = {"sudo", "docker-compose", "-f", null, "exec", null};

    private File tmpDockerComposeFile;
    boolean isRunning;
    private String envServerAddress;
    private String envServerPort;
    private String envDockerComposeFile;
    private boolean runDocker;

    class DockerComposeServiceInfo {
        public String name;
        public String port;
    }
    private List<DockerComposeServiceInfo> dockerComposeServices = new ArrayList<DockerComposeServiceInfo>();

    /**
     * Get the array of system properties as pax exam Option objects for use in pax exam
     * unit tests with Configuration annotation.
     * @return List of Option objects
     */
    public static Option[] getSysPropOptions() {
        return new Option[] {
                propagateSystemProperties(ItConstants.SERVER_IPADDRESS,
                                            ItConstants.SERVER_PORT,
                                            ItConstants.CONNECTION_TYPE,
                                            ItConstants.CONTROLLER_IPADDRESS,
                                            ItConstants.USERSPACE_ENABLED,
                                            ItConstants.DOCKER_COMPOSE_FILE_NAME,
                                            ItConstants.DOCKER_RUN)
        };
    }

    /**
     * Bring up all docker images in the default docker-compose file.
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    public DockerOvs() throws IOException, InterruptedException {
        this(DEFAULT_DOCKER_FILE);
    }

    /**
     * Bring up all docker images in the provided docker-compose file under "META-INF/docker-compose-files/".
     * @param yamlFileName Just the file name
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    public DockerOvs(String yamlFileName) throws IOException, InterruptedException {
        configureFromEnv();

        if (!runDocker) {
            LOG.info("DockerOvs.DockerOvs: Not running docker, -D{} specified", ItConstants.SERVER_IPADDRESS);
            return;
        }

        tmpDockerComposeFile = createTempDockerComposeFile(yamlFileName);
        buildDockerComposeCommands();
        parseDockerComposeYaml();

        isRunning = false;
        //We run this for A LONG TIME since on the first run docker must download the
        //image from docker hub. In experience it takes significantly less than this
        //even when downloading the image. Once the image is downloaded this command
        //runs like that <snaps fingers>
        ProcUtils.runProcess(60000, upCmd);
        isRunning = true;
        waitForOvsdbServers(10 * 1000);
    }

    /**
     * Pull required configuration from System.getProperties() and validate we have what we need.
     * Note: Note that there is some minor complexity in how this class is configured using System
     * properties. This stems from the fact that we want to preserve the meaning of these properties
     * prior to the introduction of this class. See the ENV_USAGE variable for details.
     */
    private void configureFromEnv() {
        Properties env = System.getProperties();
        envServerAddress = env.getProperty(ItConstants.SERVER_IPADDRESS);
        envServerPort = env.getProperty(ItConstants.SERVER_PORT);
        String envRunDocker = env.getProperty(ItConstants.DOCKER_RUN);
        String connType = env.getProperty(ItConstants.CONNECTION_TYPE, ItConstants.CONNECTION_TYPE_ACTIVE);
        String dockerFile = env.getProperty(ItConstants.DOCKER_COMPOSE_FILE_NAME);
        envDockerComposeFile = DOCKER_FILE_PATH + (null == dockerFile ? DEFAULT_DOCKER_FILE : dockerFile);

        //Are we running docker? If we specified docker.run, that's the answer. Otherwise, if there is a server
        //address we assume docker is already running
        runDocker = (envRunDocker != null) ? Boolean.parseBoolean(envRunDocker) : envServerAddress == null;

        if(runDocker) {
            return;
        }

        if (connType.equals(ItConstants.CONNECTION_TYPE_PASSIVE)) {
            return;
        }

        //At this point we know we're not running docker and the conn type is active - make sure we have what we need
        //If we have a server address than we require a port too as those
        //are returned in getOvsdbPort() and getOvsdbAddress()
        Assert.assertNotNull("Attempt to connect to previous running ovs but missing -Dovsdbserver.ipaddress\n"
                                                                                    + ENV_USAGE, envServerAddress);
        Assert.assertNotNull("Attempt to connect to previous running ovs but missing -Dovsdbserver.port\n"
                + ENV_USAGE, envServerPort);
    }

    /**
     * Verify and build the docker-compose commands we will be running. This function adds the docker-compose file
     * to the command lines and also checks (and adjusts the command line) as to whether sudo is required. This is
     * done by attempting to run "docker-compose ps" without and then with sudo
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    private void buildDockerComposeCommands() throws IOException, InterruptedException {
        psCmd[COMPOSE_FILE_IDX] = tmpDockerComposeFile.toString();
        psCmdNoSudo[COMPOSE_FILE_IDX_NO_SUDO] = tmpDockerComposeFile.toString();
        upCmd[COMPOSE_FILE_IDX] = tmpDockerComposeFile.toString();
        downCmd[COMPOSE_FILE_IDX] = tmpDockerComposeFile.toString();
        execCmd[COMPOSE_FILE_IDX] = tmpDockerComposeFile.toString();

        if (0 == ProcUtils.tryProcess(null, 5000, psCmdNoSudo)) {
            LOG.info("DockerOvs.buildDockerComposeCommands docker-compose does not require sudo");
            String[] tmp;
            tmp = Arrays.copyOfRange(upCmd, 1, upCmd.length);
            upCmd = tmp;
            tmp = Arrays.copyOfRange(downCmd, 1, downCmd.length);
            downCmd = tmp;
            tmp = Arrays.copyOfRange(execCmd, 1, execCmd.length);
            execCmd = tmp;
        } else if (0 == ProcUtils.tryProcess(null, 5000, psCmd)) {
            LOG.info("DockerOvs.buildDockerComposeCommands docker-compose requires sudo");
        } else {
            Assert.fail("docker-compose does not seem to work with or without sudo");
        }
    }

    /**
     * Are we using some other OVS, not a docker we spin up?
     * @return true if we are *not* running a docker image to test against
     */
    public boolean usingExternalDocker() {
        return !runDocker;
    }

    /**
     * Get the IP address of the n'th OVS.
     * @param ovsNumber which OVS?
     * @return IP string
     */
    public String getOvsdbAddress(int ovsNumber) {
        if (!runDocker) {
            return envServerAddress;
        }
        return DEFAULT_OVSDB_HOST;
    }

    /**
     * Get the port of the n'th OVS.
     * @param ovsNumber which OVS?
     * @return Port as a string
     */
    public String getOvsdbPort(int ovsNumber) {
        if (!runDocker) {
            return envServerPort;
        }
        return dockerComposeServices.get(ovsNumber).port;
    }

    /**
     * How many OVS nodes are there.
     * @return number of running OVS nodes
     */
    public int getNumOvsNodes() {
        return dockerComposeServices.size();
    }

    public String[] getExecCmdPrefix(int numOvs) {
        String[] res = new String[execCmd.length];
        System.arraycopy(execCmd, 0, res, 0, execCmd.length);
        res[res.length - 1] = dockerComposeServices.get(numOvs).name;
        return res;
    }

    public void runInContainer(int waitFor, int numOvs, String ... cmdWords) throws IOException, InterruptedException {
        String[] pfx = getExecCmdPrefix(numOvs);
        String[] cmd = new String[pfx.length + cmdWords.length];
        System.arraycopy(pfx, 0, cmd, 0, pfx.length);
        System.arraycopy(cmdWords, 0, cmd, pfx.length, cmdWords.length);
        ProcUtils.runProcess(waitFor, cmd);
    }

    public void tryInContainer(String logText, int waitFor, int numOvs, String ... cmdWords) throws IOException, InterruptedException {
        String[] pfx = getExecCmdPrefix(numOvs);
        String[] cmd = new String[pfx.length + cmdWords.length];
        System.arraycopy(pfx, 0, cmd, 0, pfx.length);
        System.arraycopy(cmdWords, 0, cmd, pfx.length, cmdWords.length);
        ProcUtils.tryProcess(logText, waitFor, cmd);
    }

    /**
     * Parse the docker-compose yaml file to extract the port mappings.
     * @return a list of the external ports
     */
    private List<String> parseDockerComposeYaml() {
        List<String> ports = new ArrayList<String>();

        YamlReader yamlReader = null;
        Map root = null;
        try {
            yamlReader = new YamlReader(new FileReader(tmpDockerComposeFile));
            root = (Map) yamlReader.read();
        } catch (FileNotFoundException e) {
            LOG.warn("DockerOvs.parseDockerComposeYaml error reading yaml file", e);
            return ports;
        } catch (YamlException e) {
            LOG.warn("DockerOvs.parseDockerComposeYaml error parsing yaml file", e);
            return ports;
        }

        if (null == root) {
            return ports;
        }
        for (Object entry : root.entrySet()) {
            String key = ((Map.Entry<String,Map>)entry).getKey();
            Map map = ((Map.Entry<String,Map>)entry).getValue();

            DockerComposeServiceInfo svc = new DockerComposeServiceInfo();
            svc.name = key;

            List portMappings = (List) map.get("ports");
            if (null == portMappings) {
                continue;
            }
            for (Object portMapping : portMappings) {
                String portMappingStr = (String) portMapping;
                int delim = portMappingStr.indexOf(":");
                if (delim == -1) {
                    continue;
                }
                String port = portMappingStr.substring(0, delim);
                ports.add(port);
                svc.port = port;
            }
            //TODO: think this through. What if there is no port?
            dockerComposeServices.add(svc);
        }

        return ports;
    }

    /**
     * Shut everything down.
     * @throws Exception but not really
     */
    @Override
    public void close() throws Exception {
        if (isRunning) {
            ProcUtils.runProcess(10000, downCmd);
            isRunning = false;
        }

        try {
            tmpDockerComposeFile.delete();
        } catch (Exception ignored) {
            //No reason to fail the test, we're just being polite here.
        }
    }

    /**
     * A thread that waits until it can "ping" a running OVS -  tests basic reachability
     * and readiness. The "ping" here is actually a list_dbs method and the response is
     * checked to make sure the Open_Vswitch DB is present. Note that this thread will
     * run until it succeeds unless its interrupt() method is called.
     */
    class OvsdbPing extends Thread {

        private final String host;
        private final int port;
        private final AtomicInteger result;
        public CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        ByteBuffer listDbsRequest;

        /**
         * Construct a new OvsdbPing object.
         * @param ovsNumber which OVS is this?
         * @param result an AtomicInteger that is incremented upon a successful "ping"
         */
        public OvsdbPing(int ovsNumber, AtomicInteger result) {
            this.host = getOvsdbAddress(ovsNumber);
            this.port = Integer.parseInt(getOvsdbPort(ovsNumber));
            this.result = result;
            listDbsRequest = ByteBuffer.wrap(
                    ("{\"method\": \"list_dbs\", \"params\": [], \"id\": " + port + "}").getBytes());
            listDbsRequest.mark();
        }

        @Override
        public void run() {
            while (!doPing()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("OvsdbPing interrupted", e);
                    return;
                }
            }
        }

        /**
         * Attempt a "ping" of the OVSDB connection.
         * @return true if the ping was successful OR IF THIS THREAD WAS INTERRUPTED
         */
        private boolean doPing() {
            try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(host, port))) {
                socketChannel.write(listDbsRequest);
                listDbsRequest.reset();

                ByteBuffer buf = ByteBuffer.allocateDirect(512);
                socketChannel.read(buf);
                buf.flip();
                String response = decoder.decode(buf).toString();

                if (response.contains("Open_vSwitch")) {
                    LOG.info("OvsdbPing connection validated");
                    result.incrementAndGet();
                    return true;
                }
            } catch (ClosedByInterruptException e) {
                LOG.warn("OvsdbPing interrupted", e);
                //return true here because we're done, ne'er to return again.
                return true;
            } catch (Exception e) {
                LOG.info("OvsdbPing exception while attempting connect {}", e.toString());
            }
            return false;
        }
    }

    /**
     * Wait for all Ovs's to accept and respond to OVSDB requests.
     * @param waitFor How long to wait
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    private void waitForOvsdbServers(long waitFor) throws IOException, InterruptedException {
        AtomicInteger numRunningOvs = new AtomicInteger(0);

        int numOvs = dockerComposeServices.size();
        if (0 == numOvs) {
            return;
        }

        OvsdbPing[] pingers = new OvsdbPing[numOvs];
        for (int i = 0; i < numOvs; i++) {
            pingers[i] = new OvsdbPing(i, numRunningOvs);
            pingers[i].start();
        }

        long startTime = System.currentTimeMillis();
        while ( (System.currentTimeMillis() - startTime) < waitFor) {
            if (numRunningOvs.get() >= numOvs) {
                LOG.info("DockerOvs.waitForOvsdbServers all OVS instances running");
                break;
            }
            Thread.sleep(1000);
        }
        LOG.info("DockerOvs.waitForOvsdbServers - finished waiting in {}", System.currentTimeMillis() - startTime);

        for (OvsdbPing pinger : pingers) {
            pinger.interrupt();
        }
    }

    /**
     * Since the docker-compose file is a resource in the bundle and docker-compose needs it.
     * in the file system, we copy it over - ugly but necessary.
     * @param yamlFileName File name
     * @return A File object for the newly created temporary yaml file.
     */
    private File createTempDockerComposeFile(String yamlFileName) {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        Assert.assertNotNull("DockerOvs: bundle is null", bundle);
        URL url = bundle.getResource(envDockerComposeFile);
        Assert.assertNotNull("DockerOvs: URL is null", url);

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("ovsdb-it-tmp-", null);

            try (Reader in = new InputStreamReader(url.openStream());
                                FileWriter out = new FileWriter(tmpFile)) {
                char[] buf = new char[1024];
                int read;
                while (-1 != (read = in.read(buf))) {
                    out.write(buf, 0, read);
                }
            }

        } catch (IOException e) {
            Assert.fail(e.toString());
        }

        return tmpFile;
    }

    /**
     * Useful for debugging. Dump some interesting config
     * @throws IOException If something goes wrong with reading the process output
     * @throws InterruptedException because there's some sleeping in here
     */
    public void logState(int dockerInstance, String logText) throws IOException, InterruptedException {
        tryInContainer(logText, 5000, dockerInstance, "ip", "addr");
        tryInContainer(logText, 5000, dockerInstance, "ovs-vsctl", "show");
        tryInContainer(logText, 5000, dockerInstance, "ovs-ofctl", "-OOpenFlow13", "show", "br-int");
        tryInContainer(logText, 5000, dockerInstance, "ovs-ofctl", "-OOpenFlow13", "dump-flows", "br-int");
        tryInContainer(logText, 5000, dockerInstance, "ip", "netns", "list");
    }

}
