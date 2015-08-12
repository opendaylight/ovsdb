/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.Packet;
import org.opendaylight.controller.liblldp.PacketException;

/**
 * Represents ARP packet. Contains methods ({@link #setSHAFieldCoordinate(Pair)}
 * {@link #setSPAFieldCoordinate(Pair)} {@link #setTHAFieldCoordinate(Pair)}
 * {@link #setTPAFieldCoordinate(Pair)}) for customization of ARP.
 * Arp by default contain fields for IPv4 as protocol address and MAC as hardware address.
 */
public class Arp extends Packet {

    private static final String HTYPE = "htype";
    private static final String PTYPE = "ptype";
    private static final String HLEN = "hlen";
    private static final String PLEN = "plen";
    private static final String OPERATION = "operation";
    private static final String SHA = "sha";
    private static final String SPA = "spa";
    private static final String THA = "tha";
    private static final String TPA = "tpa";

    private static final int ARP_FIELDS_COUNT = 9;
    private static final int ETHERNET_HW_TYPE = 1;
    private final Map<String, Pair<Integer, Integer>> ARP_FIELD_COORDINATES = new LinkedHashMap<String, Pair<Integer, Integer>>() {

        private static final long serialVersionUID = 1L;

        {
            put(HTYPE, ImmutablePair.of(0, 16));
            put(PTYPE, ImmutablePair.of(16, 16));
            put(HLEN, ImmutablePair.of(32, 8));
            put(PLEN, ImmutablePair.of(40, 8));
            put(OPERATION, ImmutablePair.of(48, 16));
            put(SHA, ImmutablePair.of(64, 48));
            put(SPA, ImmutablePair.of(112, 32));
            put(THA, ImmutablePair.of(144, 48));
            put(TPA, ImmutablePair.of(192, 32));
        }
    };

    public Arp() {
        payload = null;
        hdrFieldsMap = new HashMap<String, byte[]>(ARP_FIELDS_COUNT);
        setHardwareLength((short) 6); // MAC address length
        setProtocolLength((short) 4); // IPv4 address length
        setHardwareType(ETHERNET_HW_TYPE);
        setProtocolType(EtherTypes.IPv4.intValue());
        hdrFieldCoordMap = ARP_FIELD_COORDINATES;
    }

    public Pair<Integer, Integer> setSHAFieldCoordinate(Pair<Integer, Integer> bitOffsetAndBitLength) {
        checkNotNullPair(bitOffsetAndBitLength);
        return ARP_FIELD_COORDINATES.put(SHA, bitOffsetAndBitLength);
    }

    public Pair<Integer, Integer> setSPAFieldCoordinate(Pair<Integer, Integer> bitOffsetAndBitLength) {
        checkNotNullPair(bitOffsetAndBitLength);
        return ARP_FIELD_COORDINATES.put(SPA, bitOffsetAndBitLength);
    }

    public Pair<Integer, Integer> setTHAFieldCoordinate(Pair<Integer, Integer> bitOffsetAndBitLength) {
        checkNotNullPair(bitOffsetAndBitLength);
        return ARP_FIELD_COORDINATES.put(THA, bitOffsetAndBitLength);
    }

    public Pair<Integer, Integer> setTPAFieldCoordinate(Pair<Integer, Integer> bitOffsetAndBitLength) {
        checkNotNullPair(bitOffsetAndBitLength);
        return ARP_FIELD_COORDINATES.put(TPA, bitOffsetAndBitLength);
    }

    private void checkNotNullPair(Pair<Integer, Integer> pair) {
        checkNotNull(pair);
        checkNotNull(pair.getLeft());
        checkNotNull(pair.getRight());
    }

    @Override
    public Packet deserialize(byte[] data, int bitOffset, int size) throws PacketException {
        return super.deserialize(data, bitOffset, size);
    }

    @Override
    public byte[] serialize() throws PacketException {
        return super.serialize();
    }

    @Override
    public int getfieldnumBits(String fieldName) {
        if (fieldName.equals(SHA) || fieldName.equals(THA)) {
            return getHardwareLength() * NetUtils.NumBitsInAByte;
        } else if (fieldName.equals(SPA) || fieldName.equals(TPA)) {
            return getProtocolLength() * NetUtils.NumBitsInAByte;
        }
        return hdrFieldCoordMap.get(fieldName).getRight();
    }

    public Arp setHardwareType(int value) {
        hdrFieldsMap.put(HTYPE, Unpooled.copyShort(value).array());
        return this;
    }

    public Arp setProtocolType(int value) {
        hdrFieldsMap.put(PTYPE, Unpooled.copyShort(value).array());
        return this;
    }

    /**
     * @param value hardware length in Bytes
     */
    public Arp setHardwareLength(short value) {
        hdrFieldsMap.put(HLEN, Unpooled.buffer(1).writeByte(value).array());
        return this;
    }

    /**
     * @param value protocol length in Bytes
     */
    public Arp setProtocolLength(short value) {
        hdrFieldsMap.put(PLEN, Unpooled.buffer(1).writeByte(value).array());
        return this;
    }

    public Arp setOperation(int value) {
        hdrFieldsMap.put(OPERATION, Unpooled.copyShort(value).array());
        return this;
    }

    public Arp setSenderHardwareAddress(byte[] value) {
        hdrFieldsMap.put(SHA, value);
        return this;
    }

    public Arp setSenderProtocolAddress(byte[] value) {
        hdrFieldsMap.put(SPA, value);
        return this;
    }

    public Arp setTargetHardwareAddress(byte[] value) {
        hdrFieldsMap.put(THA, value);
        return this;
    }

    public Arp setTargetProtocolAddress(byte[] value) {
        hdrFieldsMap.put(TPA, value);
        return this;
    }

    public int getHardwareType() {
        byte[] htype = hdrFieldsMap.get(HTYPE);
        return Unpooled.wrappedBuffer(htype).readUnsignedShort();
    }

    public int getProtocolType() {
        byte[] ptype = hdrFieldsMap.get(PTYPE);
        return Unpooled.wrappedBuffer(ptype).readUnsignedShort();
    }

    public short getHardwareLength() {
        byte[] hlen = hdrFieldsMap.get(HLEN);
        return Unpooled.wrappedBuffer(hlen).readUnsignedByte();
    }

    public short getProtocolLength() {
        byte[] plen = hdrFieldsMap.get(PLEN);
        return Unpooled.wrappedBuffer(plen).readUnsignedByte();
    }

    public int getOperation() {
        byte[] operation = hdrFieldsMap.get(OPERATION);
        return Unpooled.wrappedBuffer(operation).readUnsignedShort();
    }

    public byte[] getSenderHardwareAddress() {
        return hdrFieldsMap.get(SHA);
    }

    public byte[] getSenderProtocolAddress() {
        return hdrFieldsMap.get(SPA);
    }

    public byte[] getTargetHardwareAddress() {
        return hdrFieldsMap.get(THA);
    }

    public byte[] getTargetProtocolAddress() {
        return hdrFieldsMap.get(TPA);
    }

}
