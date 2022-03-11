/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.ovsdb.it.utils;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run subprocesses and log or return their output.
 */
public final class ProcUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ProcUtils.class);

    private ProcUtils() {
        // Hidden on purpose
    }

     /**
     * Run a process and assert the exit code is 0.
     * @param waitFor How long to wait for the command to execute
     * @param words The words of the command to run
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    public static void runProcess(long waitFor, String... words) throws IOException, InterruptedException {
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
    public static void runProcess(long waitFor, StringBuilder capturedStdout, String... words)
            throws IOException, InterruptedException {
        int exitValue = tryProcess(null, waitFor, capturedStdout, words);
        assertEquals("ProcUtils.runProcess exit code is not 0", 0, exitValue);
    }

    public static int runProcess(int reserved, long waitFor, StringBuilder capturedStdout, String... words)
            throws IOException, InterruptedException {
        int exitValue = tryProcess(null, waitFor, capturedStdout, words);
        LOG.info("ProcUtils.runProcess exit code: {}", exitValue);
        return exitValue;
    }

    /**
     * Run a process.
     * @param waitFor How long to wait for the command to execute
     * @param words The words of the command to run
     * @return The process's exit code
     * @throws IOException if something goes wrong on the IO end
     * @throws InterruptedException If this thread is interrupted
     */
    public static int tryProcess(String logText, long waitFor, String... words)
            throws IOException, InterruptedException {
        return tryProcess(logText, waitFor, null, words);
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
    public static int tryProcess(String logText, long waitFor, StringBuilder capturedStdout, String... words)
            throws IOException, InterruptedException {

        final String procName = logText != null ? logText : "";
        LOG.info("ProcUtils.runProcess {} running \"{}\", waitFor {}", procName, words, waitFor);

        Process proc = new ProcessBuilder(words).start();
        int exitValue = -1;

        // Use a try block to guarantee stdout and stderr are closed
        try (BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream(),
                StandardCharsets.UTF_8));
             BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream(),
                StandardCharsets.UTF_8))) {

            exitValue = waitForExitValue(waitFor, proc);

            while (stderr.ready()) {
                LOG.warn("ProcUtils.runProcess {} [stderr]: {}", procName, stderr.readLine());
            }

            StringBuilder stdoutStringBuilder = capturedStdout != null ? capturedStdout : new StringBuilder();
            int read;
            char[] buf = new char[1024];
            while (-1 != (read = stdout.read(buf))) {
                stdoutStringBuilder.append(buf, 0, read);
            }

            LOG.info("ProcUtils.runProcess {} [stdout]:\n{}", procName, stdoutStringBuilder.toString());
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
    private static int waitForExitValue(long waitFor, Process proc) throws InterruptedException {
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
                if (System.currentTimeMillis() - startTime < waitFor) {
                    Thread.sleep(200);
                } else {
                    LOG.warn("ProcUtils.waitForExitValue: timed out while waiting for command to complete", e);
                    break;
                }
            }
        }
        return exitValue;
    }
}
