/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovs.nx.ofjava;
import org.opendaylight.openflowjava.nx.api.NiciraExtensionCodecRegistrator;
import org.opendaylight.ovs.nx.ofjava.codec.action.NiciraActionCodecs;
import org.opendaylight.ovs.nx.ofjava.codec.action.ResubmitCodec;
<<<<<<< HEAD
=======
import org.opendaylight.ovs.nx.ofjava.codec.action.SetNsiCodec;
import org.opendaylight.ovs.nx.ofjava.codec.action.SetNspCodec;
import org.opendaylight.ovs.nx.ofjava.codec.match.NiciraMatchCodecs;
import org.opendaylight.ovs.nx.ofjava.codec.match.NspCodec;
import org.opendaylight.ovs.nx.ofjava.codec.match.NsiCodec;
>>>>>>> 5e88e21... Added Postman collection to test against the loadbalancer at org.opendaylight.controller.samples.loadbalancer

import com.google.common.base.Preconditions;

public class NiciraExtensionsRegistrator implements AutoCloseable {

    private final NiciraExtensionCodecRegistrator registrator;
    public NiciraExtensionsRegistrator(NiciraExtensionCodecRegistrator registrator) {
        Preconditions.checkNotNull(registrator);
        this.registrator = registrator;
    }

    public void registerNiciraExtensions() {
        registrator.registerActionDeserializer(ResubmitCodec.RESUBMIT_DESERIALIZER_KEY, NiciraActionCodecs.RESUBMIT_CODEC);
        registrator.registerActionDeserializer(ResubmitCodec.RESUBMIT_TABLE_DESERIALIZER_KEY, NiciraActionCodecs.RESUBMIT_CODEC);
        registrator.registerActionSerializer(ResubmitCodec.SERIALIZER_KEY, NiciraActionCodecs.RESUBMIT_CODEC);
<<<<<<< HEAD
=======

        registrator.registerActionDeserializer(SetNspCodec.DESERIALIZER_KEY, NiciraActionCodecs.SET_NSP_CODEC);
        registrator.registerActionSerializer(SetNspCodec.SERIALIZER_KEY, NiciraActionCodecs.SET_NSP_CODEC);

        registrator.registerActionDeserializer(SetNsiCodec.DESERIALIZER_KEY, NiciraActionCodecs.SET_NSI_CODEC);
        registrator.registerActionSerializer(SetNsiCodec.SERIALIZER_KEY, NiciraActionCodecs.SET_NSI_CODEC);

        registrator.registerMatchEntrySerializer(NspCodec.SERIALIZER_KEY, NiciraMatchCodecs.NSP_CODEC);
        registrator.registerMatchEntryDeserializer(NspCodec.DESERIALIZER_KEY, NiciraMatchCodecs.NSP_CODEC);

        registrator.registerMatchEntrySerializer(NsiCodec.SERIALIZER_KEY, NiciraMatchCodecs.NSI_CODEC);
        registrator.registerMatchEntryDeserializer(NsiCodec.DESERIALIZER_KEY, NiciraMatchCodecs.NSI_CODEC);

>>>>>>> 5e88e21... Added Postman collection to test against the loadbalancer at org.opendaylight.controller.samples.loadbalancer
    }

    public void unregisterExtensions() {
        registrator.unregisterActionDeserializer(ResubmitCodec.RESUBMIT_DESERIALIZER_KEY);
        registrator.unregisterActionDeserializer(ResubmitCodec.RESUBMIT_TABLE_DESERIALIZER_KEY);
        registrator.unregisterActionSerializer(ResubmitCodec.SERIALIZER_KEY);
<<<<<<< HEAD
=======
        registrator.unregisterActionDeserializer(SetNsiCodec.DESERIALIZER_KEY);
        registrator.unregisterActionSerializer(SetNsiCodec.SERIALIZER_KEY);
        registrator.unregisterActionDeserializer(SetNspCodec.DESERIALIZER_KEY);
        registrator.unregisterActionSerializer(SetNspCodec.SERIALIZER_KEY);

        registrator.unregisterMatchEntrySerializer(NspCodec.SERIALIZER_KEY);
        registrator.unregisterMatchEntryDeserializer(NspCodec.DESERIALIZER_KEY);
        registrator.unregisterMatchEntrySerializer(NsiCodec.SERIALIZER_KEY);
        registrator.unregisterMatchEntryDeserializer(NsiCodec.DESERIALIZER_KEY);
>>>>>>> 5e88e21... Added Postman collection to test against the loadbalancer at org.opendaylight.controller.samples.loadbalancer
    }

    @Override
    public void close() throws Exception {
        unregisterExtensions();
    }

}
