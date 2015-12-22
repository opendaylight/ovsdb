/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class containing the common utility functions needed for operating on
 * networking data structures
 */
public class NetUtils {
    protected static final Logger LOG = LoggerFactory.getLogger(NetUtils.class);
    /**
     * Constant holding the number of bits in a byte
     */
    public static final int NumBitsInAByte = 8;
    
    /**
     * Constant holding the number of bytes in MAC Address
     */
    public static final int MACAddrLengthInBytes = 6;

    /**
     * Constant holding the number of words in MAC Address
     */
    public static final int MACAddrLengthInWords = 3;

    /**
     * Constant holding the broadcast MAC address
     */
    private static final byte[] BroadcastMACAddr = {-1, -1, -1, -1, -1, -1};

    /**
     * Converts a 4 bytes array into an integer number
     *
     * @param ba the 4 bytes long byte array
     * @return the integer number
     */
    public static int byteArray4ToInt(byte[] ba) {
        if (ba == null || ba.length != 4) {
            return 0;
        }
        return (0xff & ba[0]) << 24 | (0xff & ba[1]) << 16 | (0xff & ba[2]) << 8 | (0xff & ba[3]);
    }
    
    /**
     * Converts a 6 bytes array into a long number MAC addresses.
     *
     * @param ba The 6 bytes long byte array.
     * @return The long number.
     * Zero is returned if {@code ba} is {@code null} or
     * the length of it is not six.
     */
    public static long byteArray6ToLong(byte[] ba) {
        if (ba == null || ba.length != MACAddrLengthInBytes) {
            return 0L;
        }
        long num = 0L;
        int ival = 0; 
        do {
            num <<= NumBitsInAByte;
            num |= 0xff & ba[ival];
            ival++;
        } while (ival < MACAddrLengthInBytes);
        return num;
    }
    
    /**
     * Converts a long number to a 6 bytes array for MAC addresses.
     *
     * @param addr The long number.
     * @return The byte array.
     */
    public static byte[] longToByteArray6(long addr) {
        byte[] mac = new byte[MACAddrLengthInBytes];
        int ival = MACAddrLengthInBytes - 1;
        do {
            mac[ival] = (byte) addr;
            addr >>>= NumBitsInAByte;
            ival--;
        } while (ival >= 0);
        return mac;
    }

    /**
     * Converts an integer number into a 4 bytes array
     *
     * @param ival the integer number
     * @return the byte array 
     */
    public static byte[] intToByteArray4(int ival) {
        return new byte[]{(byte) ((ival >> 24) & 0xff), (byte) ((ival >> 16) & 0xff), (byte) ((ival >> 8) & 0xff),
            (byte) (ival & 0xff)};
    }

    /**
     * Converts an IP address passed as integer value into the respective
     * InetAddress object
     *
     * @param address the IP address in integer form
     * @return the IP address in InetAddress form
     */
    public static InetAddress getInetAddress(int address) {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByAddress(NetUtils.intToByteArray4(address));
        } catch (UnknownHostException e) {
            LOG.error("", e);
        }
        return ip;
    }
    
    /**
     * Return the InetAddress Network Mask given the length of the prefix bit
     * mask. The prefix bit mask indicates the contiguous leading bits that are
     * NOT masked out. Example: A prefix bit mask length of 8 will give an
     * InetAddress Network Mask of 255.0.0.0
     *
     * @param prefixMaskLength integer representing the length of the prefix network mask
     * @param isV6             boolean representing the IP version of the returned address
     * @return the inet address
     */
    public static InetAddress getInetNetworkMask(int prefixMaskLength, boolean isV6) {
        if (prefixMaskLength < 0 || (!isV6 && prefixMaskLength > 32) || (isV6 && prefixMaskLength > 128)) {
            return null;
        }
        byte[] v4Address = {0, 0, 0, 0};
        byte[] v6Address = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] address = (isV6) ? v6Address : v4Address;
        int numBytes = prefixMaskLength / 8;
        int numBits = prefixMaskLength % 8;
        int ival = 0; 
        for (; ival < numBytes; ival++) {
            address[ival] = (byte) 0xff;
        }
        if (numBits > 0) {
            int rem = 0; 
            for (int jval = 0; jval < numBits; jval++) {
                rem |= 1 << (7 - jval);
            }
            address[ival] = (byte) rem;
        }
    
        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            LOG.error("", e);
        }
        return null;
    }

    /**
     * Returns the prefix size in bits of the specified subnet mask. Example:
     * For the subnet mask ff.ff.ff.e0 it returns 25 while for ff.00.00.00 it
     * returns 8. If the passed subnetMask array is null, 0 is returned.
     *
     * @param subnetMask the subnet mask as byte array
     * @return the prefix length as number of bits
     */
    public static int getSubnetMaskLength(byte[] subnetMask) {
        int maskLength = 0;
        if (subnetMask != null && (subnetMask.length == 4 || subnetMask.length == 16)) {
            int index = 0;
            while (index < subnetMask.length && subnetMask[index] == (byte) 0xFF) {
                maskLength += NetUtils.NumBitsInAByte;
                index++;
            }
            if (index != subnetMask.length) {
                int bits = NetUtils.NumBitsInAByte - 1;
                while (bits >= 0 && (subnetMask[index] & 1 << bits) != 0) {
                    bits--;
                    maskLength++;
                }
            }
        }
        return maskLength;
    }
    
    /**
     * Returns the prefix size in bits of the specified subnet mask. Example:
     * For the subnet mask 255.255.255.128 it returns 25 while for 255.0.0.0 it
     * returns 8. If the passed subnetMask object is null, 0 is returned
     *
     * @param subnetMask the subnet mask as InetAddress
     * @return the prefix length as number of bits
     */
    public static int getSubnetMaskLength(InetAddress subnetMask) {
        return subnetMask == null ? 0 : NetUtils.getSubnetMaskLength(subnetMask.getAddress());
    }
    
    /**
     * Given an IP address and a prefix network mask length, it returns the
     * equivalent subnet prefix IP address Example: for ip = "172.28.30.254" and
     * maskLen = 25 it will return "172.28.30.128"
     *
     * @param ip      the IP address in InetAddress form
     * @param maskLen the length of the prefix network mask
     * @return the subnet prefix IP address in InetAddress form
     */
    public static InetAddress getSubnetPrefix(InetAddress ip, int maskLen) {
        int bytes = maskLen / 8;
        int bits = maskLen % 8;
        byte modifiedByte;
        byte[] sn = ip.getAddress();
        if (bits > 0) {
            modifiedByte = (byte) (sn[bytes] >> (8 - bits));
            sn[bytes] = (byte) (modifiedByte << (8 - bits));
            bytes++;
        }
        for (; bytes < sn.length; bytes++) {
            sn[bytes] = (byte) (0);
        }
        try {
            return InetAddress.getByAddress(sn);
        } catch (UnknownHostException excep) {
            LOG.error("Exception received in getSubnetPrefix", excep.toString());
            return null;
        }
    }
    
    /**
     * Checks if the test address and mask conflicts with the filter address and
     * mask
     *
     * For example:
     * testAddress: 172.28.2.23
     * testMask: 255.255.255.0
     * filterAddress: 172.28.1.10
     * testMask: 255.255.255.0
     * do conflict
     *
     * testAddress: 172.28.2.23
     * testMask: 255.255.255.0
     * filterAddress: 172.28.1.10
     * testMask: 255.255.0.0
     * do not conflict
     *
     * Null parameters are permitted
     *
     * @param testAddress test address
     * @param filterAddress filter address
     * @param testMask test mask
     * @param filterMask filter mask
     * @return true if there is an address confict
     */
    public static boolean inetAddressConflict(InetAddress testAddress, InetAddress filterAddress, InetAddress testMask,
                          InetAddress filterMask) {
        // Sanity check
        if ((testAddress == null) || (filterAddress == null)) {
            return false;
        }
    
        // Presence check
        if (isAny(testAddress) || isAny(filterAddress)) {
            return false;
        }

        int testMaskLen = (testMask == null) ? ((testAddress instanceof Inet4Address) ? 32 : 128) : NetUtils
            .getSubnetMaskLength(testMask);
        int filterMaskLen = (filterMask == null) ? ((testAddress instanceof Inet4Address) ? 32 : 128) : NetUtils
            .getSubnetMaskLength(filterMask);
    
        // Mask length check. Test mask has to be more specific than filter one
        if (testMaskLen < filterMaskLen) {
            return true;
        }
    
        // Subnet Prefix on filter mask length must be the same
        InetAddress prefix1 = getSubnetPrefix(testAddress, filterMaskLen);
        InetAddress prefix2 = getSubnetPrefix(filterAddress, filterMaskLen);
        return (!prefix1.equals(prefix2));
    }

    /**
     * Returns true if the passed MAC address is all zero
     *
     * @param mac the byte array representing the MAC address
     * @return true if all MAC bytes are zero
     */ 
    public static boolean isZeroMAC(byte[] mac) {
        for (short ival = 0; ival < 6; ival++) {
            if (mac[ival] != 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns true if the MAC address is the broadcast MAC address and false
     * otherwise.
     *
     * @param macAddress MAC Address
     * @return true if the MAC address is the broadcast MAC address
     */
    public static boolean isBroadcastMACAddr(byte[] macAddress) {
        if (macAddress.length == MACAddrLengthInBytes) {
            for (int ival = 0; ival < 6; ival++) {
                if (macAddress[ival] != BroadcastMACAddr[ival]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the MAC address is a unicast MAC address and false
     * otherwise.
     *
     * @param macAddress MAC Address
     * @return true if the MAC address is the unicast MAC address
     */
    public static boolean isUnicastMACAddr(byte[] macAddress) {
        if (macAddress.length == MACAddrLengthInBytes) {
            return (macAddress[0] & 1) == 0;
        }
        return false;
    }
    
    /**
     * Returns true if the MAC address is a multicast MAC address and false
     * otherwise. Note that this explicitly returns false for the broadcast MAC
     * address.
     *
     * @param macAddress MAC Address
     * @return true if the MAC address is the multicast MAC address
     */
    public static boolean isMulticastMACAddr(byte[] macAddress) {
        if (macAddress.length == MACAddrLengthInBytes && !isBroadcastMACAddr(macAddress)) {
            return (macAddress[0] & 1) != 0;
        }
        return false;
    }
    
    /**
     * Returns true if the passed InetAddress contains all zero
     *
     * @param ip the IP address to test
     * @return true if the address is all zero
     */
    public static boolean isAny(InetAddress ip) {
        for (byte ival : ip.getAddress()) {
            if (ival != 0) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean fieldsConflict(int field1, int field2) {
        if ((field1 == 0) || (field2 == 0) || (field1 == field2)) {
            return false;
        }
        return true;
    }
    
    public static InetAddress parseInetAddress(String addressString) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(addressString);
        } catch (UnknownHostException excep) {
            LOG.error("", excep);
        }
        return address;
    }
    
    /**
     * Checks if the passed IP v4 address in string form is valid The address
     * may specify a mask at the end as "/MM"
     *
     * @param cidr the v4 address as A.B.C.D/MM
     * @return true if the passed IP v4 address in string form is valid
     */
    public static boolean isIPv4AddressValid(String cidr) {
        if (cidr == null) {
            return false;
        }
    
        String[] values = cidr.split("/");
        Pattern ipv4Pattern = Pattern
            .compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
        Matcher mm = ipv4Pattern.matcher(values[0]);
        if (!mm.matches()) {
            return false;
        }
        if (values.length >= 2) {
            int prefix = Integer.valueOf(values[1]);
            if ((prefix < 0) || (prefix > 32)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the passed IP v6 address in string form is valid The address
     * may specify a mask at the end as "/MMM"
     *
     * @param cidr the v6 address as A::1/MMM
     * @return true if the passed IP v6 address in string form is valid
     */
    public static boolean isIPv6AddressValid(String cidr) {
        if (cidr == null) {
            return false;
        }
    
        String[] values = cidr.split("/");
        try {
            // when given an IP address, InetAddress.getByName validates the ip
            // address
            InetAddress addr = InetAddress.getByName(values[0]);
            if (!(addr instanceof Inet6Address)) {
                return false;
            }
        } catch (UnknownHostException ex) {
            return false;
        }
    
        if (values.length >= 2) {
            int prefix = Integer.valueOf(values[1]);
            if ((prefix < 0) || (prefix > 128)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the passed IP address in string form is a valid v4 or v6
     * address. The address may specify a mask at the end as "/MMM"
     *
     * @param cidr the v4 or v6 address as IP/MMM
     * @return true if the passed IP address in string form is a valid v4 or v6
     */
    public static boolean isIPAddressValid(String cidr) {
        return NetUtils.isIPv4AddressValid(cidr) || NetUtils.isIPv6AddressValid(cidr);
    }

    /*
     * Following utilities are useful when you need to compare or bit shift java
     * primitive type variable which are inherently signed
     */
    
    /**
     * Returns the unsigned value of the passed byte variable
     *
     * @param ival the byte value
     * @return the int variable containing the unsigned byte value
     */
    public static int getUnsignedByte(byte ival) {
        return ival & 0xFF;
    }
    
    /**
     * Return the unsigned value of the passed short variable
     *
     * @param ival the short value
     * @return the int variable containing the unsigned short value
     */
    public static int getUnsignedShort(short ival) {
        return ival & 0xFFFF;
    }
    
    /**
     * Returns the highest v4 or v6 InetAddress
     *
     * @param v6 true for IPv6, false for Ipv4
     * @return The highest IPv4 or IPv6 address
     */
    public static InetAddress gethighestIP(boolean v6) {
        try {
            return (v6) ? InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff") : InetAddress
                .getByName("255.255.255.255");
        } catch (UnknownHostException excep) {
            LOG.error("Exception received in gethighestIP", excep.toString());
            return null;
        }
    }

    /**
     * Returns Broadcast MAC Address
     *
     * @return the byte array containing  broadcast mac address
     */
    public static byte[] getBroadcastMACAddr() {
        return Arrays.copyOf(BroadcastMACAddr, BroadcastMACAddr.length);
    }
}
