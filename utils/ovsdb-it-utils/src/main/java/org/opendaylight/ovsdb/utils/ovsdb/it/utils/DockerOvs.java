/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.ovsdb.it.utils;

import java.io.BufferedReader;
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
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run OVS(s) using docker-compose for use in integration tests.
 * For example,
 * <pre>
 * try(DockerOvs ovs = new DockerOvs()) {
 *      ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(
 *                               ovs.getOvsdbAddress(0), ovs.getOvsdbPort(0));
 *      ...
        nodeInfo.disconnect();

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
 * to accept OVSDB connections. In order for this to work, the docker-compose file *must*
 * have a port mapping.
 * Currently, DockerOvs does not support docker images with OVS instances that connect actively.
 */
public class DockerOvs implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DockerOvs.class);
    public static final String DOCKER_SUDO = "docker.sudo";
    private static final String DEFAULT_DOCKER_FILE = "docker-ovs-2.5.1.yml";
    private static final String DOCKER_FILE_PATH = "META-INF/docker-compose-files/";
    //private static final String[] HELP_CMD = {"docker-compose", "--help"};
    //private static final String[] EXEC_CMD_PFX = {"sudo", "docker-compose", "-f"};
    private static final int COMPOSE_FILE_IDX = 3;
    private static final String DEFAULT_OVSDB_HOST = "127.0.0.1";
    private static final String[] PS_CMD = {"sudo", "docker-compose", "ps"};
    private static final String[] PS_CMD_NO_SUDO = {"docker-compose", "ps"};

    private String[] upCmd = {"sudo", "docker-compose", "-f", null, "up", "-d"};
    private String[] downCmd = {"sudo", "docker-compose", "-f", null, "stop"};
    private File tmpDockerComposeFile;
    private List<String> ovsdbPorts;
    boolean isRunning;

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
        tmpDockerComposeFile = createTempDockerComposeFile(yamlFileName);
        buildDockerComposeCommands();
        ovsdbPorts = extractPortsFromYaml();

        isRunning = false;
        //We run this for A LONG TIME since on the first run docker must download the
        //image from docker hub. In experience it takes significantly less than this
        //even when downloading the image. Once the image is downloaded this command
        //runs like that <snaps fingers>
        runProcess(60000, upCmd);
        isRunning = true;
        waitForOvsdbServers(10 * 1000);
    }

    /**
     * Verify and build the docker-compose commands we will be running. This function adds the docker-compose file
     * to the command lines and also checks (and adjusts the command line) as to whether sudo is required. This is
     * done by attempting to run "docker-compose ps" without and then with sudo
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    private void buildDockerComposeCommands() throws IOException, InterruptedException {
        upCmd[COMPOSE_FILE_IDX] = tmpDockerComposeFile.toString();
        downCmd[COMPOSE_FILE_IDX] = tmpDockerComposeFile.toString();

        if (0 == tryProcess(5000, PS_CMD_NO_SUDO)) {
            LOG.info("DockerOvs.buildDockerComposeCommands docker-compose does not require sudo");
            String[] tmp;
            tmp = Arrays.copyOfRange(upCmd, 1, upCmd.length);
            upCmd = tmp;
            tmp = Arrays.copyOfRange(downCmd, 1, downCmd.length);
            downCmd = tmp;
        } else if (0 == tryProcess(5000, PS_CMD)) {
            LOG.info("DockerOvs.buildDockerComposeCommands docker-compose requires sudo");
        } else {
            Assert.fail("docker-compose does not seem to work with or without sudo");
        }
    }
    /**
     * Get the IP address of the n'th OVS.
     * @param ovsNumber which OVS?
     * @return IP string
     */
    public String getOvsdbAddress(int ovsNumber) {
        return DEFAULT_OVSDB_HOST;
    }

    /**
     * Get the port of the n'th OVS.
     * @param ovsNumber which OVS?
     * @return Port as a string
     */
    public String getOvsdbPort(int ovsNumber) {
        return ovsdbPorts.get(ovsNumber);
    }

    /**
     * How many OVS nodes are there.
     * @return number of running OVS nodes
     */
    public int getNumOvsNodes() {
        return ovsdbPorts.size();
    }

    /**
     * Parse the docker-compose yaml file to extract the port mappings.
     * @return a list of the external ports
     */
    private List<String> extractPortsFromYaml() {
        List<String> ports = new ArrayList<String>();

        YamlReader yamlReader = null;
        Map root = null;
        try {
            yamlReader = new YamlReader(new FileReader(tmpDockerComposeFile));
            root = (Map) yamlReader.read();
        } catch (FileNotFoundException e) {
            LOG.warn("DockerOvs.extractPortsFromYaml error reading yaml file", e);
            return ports;
        } catch (YamlException e) {
            LOG.warn("DockerOvs.extractPortsFromYaml error parsing yaml file", e);
            return ports;
        }

        if (null == root) {
            return ports;
        }
        for (Object map : root.values()) {
            List portMappings = (List) ((Map)map).get("ports");
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
            }
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
            runProcess(5000, downCmd);
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

        int numOvs = ovsdbPorts.size();
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

    /*
    WIP - todo: need to extract teh service name from the yaml or receive it as a param
    private void validateDockerComposeVersion() throws IOException, InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        runProcess(2000, stringBuilder, HELP_CMD);
        assertTrue("DockerOvs.validateDockerComposeVersion: docker-compose version does not support exec, try updating",
                                                                    stringBuilder.toString().contains(" exec "));
    }

    public String exec(long waitFor, String... execCmdWords) throws IOException, InterruptedException {
        List<String> execCmd = new ArrayList<String>(20);
        execCmd.addAll(Arrays.asList(EXEC_CMD_PFX));
        execCmd.add(tmpDockerComposeFile.toString());
        execCmd.add("exec");
        execCmd.add("ovs");
        execCmd.addAll(Arrays.asList(execCmdWords));

        StringBuilder stringBuilder = new StringBuilder();
        runProcess(waitFor, stringBuilder, execCmd.toArray(new String[0]));
        return stringBuilder.toString();
    }
    */

    /**
     * Run a process and assert the exit code is 0.
     * @param waitFor How long to wait for the command to execute
     * @param words The words of the command to run
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    private void runProcess(long waitFor, String... words) throws IOException, InterruptedException {
        runProcess(waitFor, null, words);
    }

    /**
     * Run a process, collect the stdout, and assert the exit code is 0.
     * @param waitFor How long to wait for the command to execute
     * @param capturedStdout Whatever the process wrote to standard out
     * @param words The words of the command to run
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    private void runProcess(long waitFor,StringBuilder capturedStdout, String... words)
                                                                        throws IOException, InterruptedException {
        int exitValue = tryProcess(waitFor, capturedStdout, words);
        Assert.assertEquals("DockerOvs.runProcess exit code is not 0", 0, exitValue);
    }

    /**
     * Run a process.
     * @param waitFor How long to wait for the command to execute
     * @param words The words of the command to run
     * @return The process's exit code
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    private int tryProcess(long waitFor, String... words) throws IOException, InterruptedException {
        return tryProcess(waitFor, null, words);
    }

    /**
     * Run a process, collect the stdout.
     * @param waitFor How long to wait (milliseconds) for the command to execute
     * @param capturedStdout Whatever the process wrote to standard out
     * @param words The words of the command to run
     * @return The process's exit code or -1 if the the command does not complete within waitFor milliseconds
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    private int tryProcess(long waitFor, StringBuilder capturedStdout, String... words)
                                                                        throws IOException, InterruptedException {

        LOG.info("DockerOvs.runProcess running \"{}\", waitFor {}", words, waitFor);

        Process proc = new ProcessBuilder(words).start();
        int exitValue = -1;

        // Use a try block to guarantee stdout and stderr are closed
        try (BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {

            exitValue = waitForExitValue(waitFor, proc);

            while (stderr.ready()) {
                LOG.warn("DockerOvs.runProcess [stderr]: {}", stderr.readLine());
            }

            StringBuilder stdoutStringBuilder = (capturedStdout != null) ? capturedStdout : new StringBuilder();
            int read;
            char[] buf = new char[1024];
            while (-1 != (read = stdout.read(buf))) {
                stdoutStringBuilder.append(buf, 0, read);
            }

            for (String line : stdoutStringBuilder.toString().split("\\n")) {
                LOG.info("DockerOvs.runProcess [stdout]: {}", line);
            }
        }

        return exitValue;
    }

    /**
     * Wait for a process to end.
     * @param waitFor how long to wait in milliseconds
     * @param proc Process object
     * @return the process's exit value or -1 if the process did not complete within waitFor milliseconds
     * @throws InterruptedException if this thread is interrupted
     */
    private int waitForExitValue(long waitFor, Process proc) throws InterruptedException {
        //Java 7 has no way to check whether a process is still running without blocking
        //until the process exits. What this hack does is checks the exitValue() which
        //throws an IllegalStateException if the process is still running still it does
        //not have a exit value. We catch that exception and implement our own timeout.
        //Once we no longer need to support Java 7, this has more elegant solutions.
        int exitValue = -1;
        long startTime = System.currentTimeMillis();
        while (true) {
            try {
                exitValue = proc.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
                if ((System.currentTimeMillis() - startTime) < waitFor) {
                    Thread.sleep(200);
                } else {
                    LOG.warn("DockerOvs.waitForExitValue: timed out while waiting for command to complete", e);
                    break;
                }
            }
        }
        return exitValue;
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
        URL url = bundle.getResource(DOCKER_FILE_PATH + yamlFileName);
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

}
