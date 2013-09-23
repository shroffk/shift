/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package gov.bnl.shift.api;

import static gov.bnl.shift.api.ShiftFinderClientImpl.CFCBuilder.serviceURL;

public class ShiftFinder {

    public static final String DEFAULT_CLIENT = "composite_client";
    private static volatile ShiftFinderClient client;

    private ShiftFinder() {

    }

    public static void setClient(ShiftFinderClient client) {
        ShiftFinder.client = client;
    }

    /**
     *
     * @return returns the default {@link ShiftFinder}.
     */
    public static ShiftFinderClient getClient() {
        if(client == null){
            ShiftFinder.client = serviceURL().withHTTPAuthentication(false).create();
        }
        return client;
    }

}
