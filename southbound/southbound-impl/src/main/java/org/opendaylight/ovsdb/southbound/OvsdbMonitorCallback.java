package org.opendaylight.ovsdb.southbound;

import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbMonitorCallback implements MonitorCallBack {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbMonitorCallback.class);
    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        LOG.debug("result: {} dbSchema: {}",result,dbSchema);

    }

    @Override
    public void exception(Throwable t) {
        LOG.warn("exception {}",t);
    }

}
