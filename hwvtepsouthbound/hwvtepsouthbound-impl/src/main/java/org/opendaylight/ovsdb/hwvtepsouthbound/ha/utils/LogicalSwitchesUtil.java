package org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils;

import com.google.common.base.Function;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesBuilder;

import java.util.Comparator;

public class LogicalSwitchesUtil {

    public static LogicalSwitchesComparator logicalSwitchesComparator = new LogicalSwitchesComparator();

    public static Function<LogicalSwitches, LogicalSwitches> logicalSwitchTransformer = new Function<LogicalSwitches, LogicalSwitches>() {
        public LogicalSwitches apply(LogicalSwitches src) {
            LogicalSwitchesBuilder logicalSwitchesBuilder = new LogicalSwitchesBuilder(src);
            return logicalSwitchesBuilder.setLogicalSwitchUuid(HAUtil.getUUid(src.getHwvtepNodeName().getValue())).build();
        }
    };

    public static class LogicalSwitchesComparator implements Comparator<LogicalSwitches> {
        @Override
        public int compare(final LogicalSwitches updated, final LogicalSwitches orig) {
            if (updated.getHwvtepNodeName().getValue().equals(
                    orig.getHwvtepNodeName().getValue())) {
                return 0;
            }
            return 1;
        }
    }

}
