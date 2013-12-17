package org.opendaylight.ovsdb.neutron;

import junit.framework.TestCase;

public class BaseHandlerTest extends TestCase {

    BaseHandler test = new BaseHandler();

    String nullString = null;
    String uuid = "ce044452-f22e-4ea4-a3ec-d1cde80cf996";
    String tenantId = "8d62bfa112fb4247aa20edc74235c1ce";
    String neutronId = "6b8fd2";

    public void testIsValidNeutronID() throws Exception {
        assertFalse(test.isValidNeutronID(nullString));
        assertTrue(test.isValidNeutronID(uuid));
        assertTrue(test.isValidNeutronID(tenantId));
        assertTrue(test.isValidNeutronID(neutronId));

    }

    public void testConvertNeutronIDToKey() throws Exception {

        String uuidResult = test.convertNeutronIDToKey(uuid);
        String uuidExpected = "ce044452f22eea4a3ecd1cde80cf996";

        String tenantResult = test.convertNeutronIDToKey(tenantId);
        String tenantExpected = "8d62bfa112fb247aa20edc74235c1ce";

        String neutronResult = test.convertNeutronIDToKey(neutronId);

        assertEquals(uuidExpected, uuidResult);
        assertEquals(tenantExpected, tenantResult);
        assertEquals(neutronId, neutronResult);

    }
}
