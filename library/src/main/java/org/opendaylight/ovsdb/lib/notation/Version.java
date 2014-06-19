/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.lib.notation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {

    int major;
    int minor;
    int patch;

    private static final String FORMAT = "(\\d+)\\.(\\d+)\\.(\\d+)";
    private static final Pattern PATTERN = Pattern.compile(Version.FORMAT);

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static Version fromString(String version){
        final Matcher matcher = Version.PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("<"+version+"> does not match format "+Version.FORMAT);
        }
        int major = Integer.valueOf(matcher.group(1));
        int minor = Integer.valueOf(matcher.group(2));
        int patch = Integer.valueOf(matcher.group(3));
        return new Version(major, minor, patch);
    }

    public String toString(){
        return "" + major + "." + minor + "." + patch;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getPatch() {
        return patch;
    }

    public void setPatch(int patch) {
        this.patch = patch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (major != version.major) return false;
        if (minor != version.minor) return false;
        if (patch != version.patch) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }

    @Override
    public int compareTo(Version o) {
        if (this.equals(o)) return 0;
        if (this.major > o.major) return 1;
        if (this.major < o.major) return -1;
        // major is equal
        if (this.minor > o.minor) return 1;
        if (this.minor < o.minor) return -1;
        // minor is equal
        if (this.patch > o.patch) return 1;
        // must be less than
        return -1;
    }
}
