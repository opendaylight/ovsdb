/*
 * Copyright (c) 2013, 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;
/**
 * @author Arun Yerra
 */

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.INetvirtSfcOF13Provider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import java.util.Collection;

/**
 * AbstractChangeListner implemented basic {@link AsyncDataChangeEvent} processing for
 * netvirt-sfc data change listener.
 *
 */
public abstract class AbstractDataTreeListener <T extends DataObject> implements INetvirtSfcDataProcessor<T>{
	private static final Logger LOG = LoggerFactory.getLogger(AbstractDataTreeListener.class);
	protected INetvirtSfcOF13Provider provider;
	protected final Class<T> clazz;

	public AbstractDataTreeListener (INetvirtSfcOF13Provider provider, Class<T> clazz) {
		this.provider = Preconditions.checkNotNull(provider, "provider can not be null!");
		this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
	}

	@Override
	public void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
		Preconditions.checkNotNull(changes, "Changes may not be null!");

		LOG.info("Received Data Tree Changed ...", changes);
		for (DataTreeModification<T> change : changes) {
			final InstanceIdentifier<T> key = change.getRootPath().getRootIdentifier();
			final DataObjectModification<T> mod = change.getRootNode();
			LOG.info("Received Data Tree Changed Update of Type={} for Key={}", mod.getModificationType(), key);
			switch (mod.getModificationType()) {
			case DELETE:
				remove(key, mod.getDataBefore());
				break;
			case SUBTREE_MODIFIED:
				update(key, mod.getDataBefore(), mod.getDataAfter());
				break;
			case WRITE:
				if (mod.getDataBefore() == null) {
					add(key, mod.getDataAfter());
				} else {
					update(key, mod.getDataBefore(), mod.getDataAfter());
				}
				break;
			default:
				throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
			}
		}
	}
}
