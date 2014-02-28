package org.opendaylight.ovsdb.lib.message.operations;

import org.opendaylight.ovsdb.lib.notation.Condition;

/**
 * Represents an Operation type that accepts acondition, for e.g Update, Select etc
 * @author araveendrann
 */
public interface ConditionalOperation {

    public void addCondition(Condition condition);
}
