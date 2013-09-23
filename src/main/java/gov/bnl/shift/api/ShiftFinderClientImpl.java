/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package gov.bnl.shift.api;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * A Client object to query the shiftFinder service for shifts based on
 * shift id.
 *
 * @author eschuhmacher
 *
 */
public class ShiftFinderClientImpl implements ShiftFinderClient {
    private final WebResource service;
    private final ExecutorService executor;

    private static final String resourcShifts = "resources/shifts";
    private static final String resourceProperties = "resources/properties";
    private static final String resourceTags = "resources/tags";

    /**
     * A Builder class to help create the client to the ShiftFinder Service
     *
     * @author shroffk
     *
     */
    public static class CFCBuilder {

        // required
        private URI uri = null;

        // optional
        private boolean withHTTPAuthentication = false;
        private HTTPBasicAuthFilter httpBasicAuthFilter = null;

        private ClientConfig clientConfig = null;
        private TrustManager[] trustManager = new TrustManager[] { new DummyX509TrustManager() };;
        @SuppressWarnings("unused")
        private SSLContext sslContext = null;

        private String protocol = null;
        private String username = null;
        private String password = null;

        private ExecutorService executor = Executors.newSingleThreadExecutor();

        private CFProperties properties = new CFProperties();

        private static final String serviceURL = "http://localhost:8080/ShiftFinder"; //$NON-NLS-1$

        private CFCBuilder() {
            this.uri = URI.create(this.properties.getPreferenceValue(
                    "shiftfinder.serviceURL", serviceURL)); //$NON-NLS-1$
            this.protocol = this.uri.getScheme();
        }

        private CFCBuilder(URI uri) {
            this.uri = uri;
            this.protocol = this.uri.getScheme();
        }

        /**
         * Creates a {@link CFCBuilder} for a CF client to Default URL in the
         * shiftfinder.properties.
         *
         * @return {@link CFCBuilder}
         */
        public static CFCBuilder serviceURL() {
            return new CFCBuilder();
        }

        /**
         * Creates a {@link CFCBuilder} for a CF client to URI <tt>uri</tt>.
         *
         * @param uri
         * @return {@link CFCBuilder}
         */
        public static CFCBuilder serviceURL(String uri) {
            return new CFCBuilder(URI.create(uri));
        }

        /**
         * Creates a {@link CFCBuilder} for a CF client to {@link URI}
         * <tt>uri</tt>.
         *
         * @param uri
         * @return {@link CFCBuilder}
         */
        public static CFCBuilder serviceURL(URI uri) {
            return new CFCBuilder(uri);
        }

        /**
         * Enable of Disable the HTTP authentication on the client connection.
         *
         * @param withHTTPAuthentication
         * @return {@link CFCBuilder}
         */
        public CFCBuilder withHTTPAuthentication(boolean withHTTPAuthentication) {
            this.withHTTPAuthentication = withHTTPAuthentication;
            return this;
        }

        /**
         * Set the username to be used for HTTP Authentication.
         *
         * @param username
         * @return {@link CFCBuilder}
         */
        public CFCBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the password to be used for the HTTP Authentication.
         *
         * @param password
         * @return {@link CFCBuilder}
         */
        public CFCBuilder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * set the {@link ClientConfig} to be used while creating the
         * shitfinder client connection.
         *
         * @param clientConfig
         * @return {@link CFCBuilder}
         */
        public CFCBuilder withClientConfig(ClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        @SuppressWarnings("unused")
        private CFCBuilder withSSLContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * Set the trustManager that should be used for authentication.
         *
         * @param trustManager
         * @return {@link CFCBuilder}
         */
        public CFCBuilder withTrustManager(TrustManager[] trustManager) {
            this.trustManager = trustManager;
            return this;
        }

        /**
         * Provide your own executor on which the queries are to be made. <br>
         * By default a single threaded executor is used.
         *
         * @param executor
         * @return {@link CFCBuilder}
         */
        public CFCBuilder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Will actually create a {@link ShiftFinderClientImpl} object using
         * the configuration informoation in this builder.
         *
         * @return {@link ShiftFinderClientImpl}
         */
        public ShiftFinderClient create() throws ShiftFinderException {
            if (this.protocol.equalsIgnoreCase("http")) { //$NON-NLS-1$
                this.clientConfig = new DefaultClientConfig();
            } else if (this.protocol.equalsIgnoreCase("https")) { //$NON-NLS-1$
                if (this.clientConfig == null) {
                    SSLContext sslContext = null;
                    try {
                        sslContext = SSLContext.getInstance("SSL"); //$NON-NLS-1$
                        sslContext.init(null, this.trustManager, null);
                    } catch (NoSuchAlgorithmException e) {
                        throw new ShiftFinderException(e.getMessage());
                    } catch (KeyManagementException e) {
                        throw new ShiftFinderException(e.getMessage());
                    }
                    this.clientConfig = new DefaultClientConfig();
                    this.clientConfig.getProperties().put(
                            HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                            new HTTPSProperties(new HostnameVerifier() {

                                @Override
                                public boolean verify(String hostname,
                                                      SSLSession session) {
                                    return true;
                                }
                            }, sslContext));
                }
            }
            if (this.withHTTPAuthentication) {
                this.httpBasicAuthFilter = new HTTPBasicAuthFilter(
                        ifNullReturnPreferenceValue(this.username,
                                "shiftfinder.username", "username"),
                        ifNullReturnPreferenceValue(this.password,
                                "shiftfinder.password", "password"));
            }
            return new ShiftFinderClientImpl(this.uri, this.clientConfig,
                    this.httpBasicAuthFilter, this.executor);
        }

        private String ifNullReturnPreferenceValue(String value, String key,
                                                   String Default) {
            if (value == null) {
                return this.properties.getPreferenceValue(key, Default);
            } else {
                return value;
            }
        }
    }

    ShiftFinderClientImpl(URI uri, ClientConfig config,
                          HTTPBasicAuthFilter httpBasicAuthFilter, ExecutorService executor) {
        Client client = Client.create(config);
        if (httpBasicAuthFilter != null) {
            client.addFilter(httpBasicAuthFilter);
        }
        client.addFilter(new RawLoggingFilter(Logger
                .getLogger(RawLoggingFilter.class.getName())));
        client.setFollowRedirects(true);
        service = client.resource(UriBuilder.fromUri(uri).build());
        this.executor = executor;
    }


    /**
     * Returns a shift that exactly matches the shift id
     * <tt>shiftId</tt>.
     *
     * @param shiftId - Id of the required shift.
     * @return {@link Shift} with id <tt>shiftId</tt> or null
     * @throws ShiftFinderException
     */
    public Shift getShift(Long shiftId) throws ShiftFinderException {
        try {
            return wrappedSubmit(new FindByShiftId(shiftId));
        } catch (ShiftFinderException e) {
            if (e.getStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                return null;
            } else {
                throw e;
            }
        }

    }

    private class FindByShiftId implements Callable<Shift> {

        private final Long shiftId;

        FindByShiftId(Long shiftId){
            super();
            this.shiftId = shiftId;
        }

        @Override
        public Shift call() throws UniformInterfaceException {
            return new Shift(service.path(resourcShifts).path(shiftId)
                    .accept( //$NON-NLS-1$
                            MediaType.APPLICATION_XML).get(XMLShift.class));
        }

    }


    /**
     * Destructively set a single shift <tt>shift</tt>, if the shift
     * already exists return an error.
     *
     * @param shift
     *            the shift to be added
     * @throws ShiftFinderException
     */
    //TODO: right now it just remove the old one, fix it to through an exception
    public void set(Shift.Builder shift) throws ShiftFinderException {
        wrappedSubmit(new SetShifts(new XMLShifts(shift.toXml())));
    }


    /**
     * Update existing shift with <tt>shift</tt>.
     *
     * @param shift
     * @throws ShiftFinderException
     */
    public void update(Shift.Builder shift) throws ShiftFinderException {
        wrappedSubmit(new UpdateShift(shift.toXml()));
    }

    private class UpdateShift implements Runnable {
        private final XMLShift shift;

        UpdateShift(XMLShift xmlShift) {
            super();
            this.shift = xmlShift;
        }

        @Override
        public void run() {
            service.path(resourcShifts).path(shift.getId())
                    .type(MediaType.APPLICATION_XML).post(shift);
        }

    }


    /**
     * Search for shifts who's name match the pattern <tt>pattern</tt>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern
     *            - the search pattern for the shifts ids
     * @return A Collection of shits who's id match the pattern
     *         <tt>pattern</tt>
     * @throws ShiftFinderException
     */
    public Collection<Shift> findById(String pattern)
            throws ShiftFinderException {
        Map<String, String> searchMap = new HashMap<String, String>();
        searchMap.put("~id", pattern);
        return wrappedSubmit(new FindByMap(searchMap));
    }

    /**
     * Search for shifts with start date that match the pattern
     * <tt>pattern</tt>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern
     *            - the search pattern for the start dates
     * @return A Collection of shifts which contain start date who's name match the
     *         pattern <tt>pattern</tt>
     * @throws ShiftFinderException
     */
    public Collection<Shift> findByStartDate(String pattern)
            throws ShiftFinderException {
        Map<String, String> searchMap = new HashMap<String, String>();
        searchMap.put("~startDate", pattern);
        return wrappedSubmit(new FindByMap(searchMap));
    }

    /**
     * Query for shifts based on the Query string <tt>query</tt>
     *
     * IMP: each criteria is logically AND'ed while multiple values for
     * Properties are OR'ed.<br>
     *
     * @param query
     * @return Collection of shifts which satisfy the search criteria.
     * @throws ShiftFinderException
     */
    public Collection<Shift> find(String query) throws ShiftFinderException {
        return wrappedSubmit(new FindByMap(buildSearchMap(query)));
    }

    /**
     * Query for shifts based on the multiple criteria specified in the map.

     *
     * this will return all shifts with name=any name AND tag=tag1 AND
     * property Cell = 1 OR 2 OR 3.
     *
     * @param map
     * @return Collection of shifts which satisfy the search map.
     * @throws ShiftFinderException
     */
    public Collection<Shift> find(Map<String, String> map)
            throws ShiftFinderException {
        return wrappedSubmit(new FindByMap(map));
    }

    @Override
    public Collection<Shift> find(MultivaluedMap<String, String> map) throws ShiftFinderException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private class FindByMap implements Callable<Collection<Shift>> {

        private MultivaluedMapImpl map;

        FindByMap(Map<String, String> map) {
            MultivaluedMapImpl mMap = new MultivaluedMapImpl();
            for (Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                for (String value : Arrays.asList(entry.getValue().split(","))) {
                    mMap.add(key, value.trim());
                }
            }
            this.map = mMap;
        }

        FindByMap(MultivaluedMap<String, String> map) {
            this.map = new MultivaluedMapImpl();
            this.map.putAll(map);
        }

        @Override
        public Collection<Shift> call() throws Exception {
            Collection<Shift> shifts = new HashSet<Shift>();
            XMLShifts xmlShifts = service.path(resourcShifts)
                    //$NON-NLS-1$
                    .queryParams(this.map).accept(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON).get(XMLShifts.class);
            for (XMLShift xmlshift : xmlShifts.getShifts()) {
                shifts.add(new Shift(xmlshift));
            }
            return Collections.unmodifiableCollection(shifts);
        }

    }

    static MultivaluedMap<String, String> buildSearchMap(String searchPattern) {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        searchPattern = searchPattern.replaceAll(", ", ",");
        String[] words = searchPattern.split("\\s");
        if (words.length <= 0) {
            throw new IllegalArgumentException();
        } else {
            for (int index = 0; index < words.length; index++) {
                if (!words[index].contains("=")) {
                    // this is a name value
                    if (words[index] != null)
                        map.add("~id", words[index]);
                } else {
                    // this is a property or tag
                    String[] keyValue = words[index].split("=");
                    String key = null;
                    String valuePattern;
                    try {
                        key = keyValue[0];
                        valuePattern = keyValue[1];
                        if (key.equalsIgnoreCase("startDate")) {
                            key = "~startDate";
                        }
                        for (String value : valuePattern.replace("||", ",")
                                .split(",")) {
                            map.add(key, value);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        if (e.getMessage().equals(String.valueOf(0))) {
                            throw new IllegalArgumentException(
                                    "= must be preceeded by a propertyName or keyword Tags.");
                        } else if (e.getMessage().equals(String.valueOf(1)))
                            throw new IllegalArgumentException("key: '" + key
                                    + "' is specified with no pattern.");
                    }

                }
            }
        }
        return map;
    }


    /**
     * close
     */
    public void close() {
        this.executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!this.executor.awaitTermination(60, TimeUnit.SECONDS)) {
                this.executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!this.executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate"); //$NON-NLS-1$
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            this.executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private <T> T wrappedSubmit(Callable<T> callable) {
        try {
            return this.executor.submit(callable).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() != null
                    && e.getCause() instanceof UniformInterfaceException) {
                throw new ShiftFinderException(
                        (UniformInterfaceException) e.getCause());
            }
            throw new RuntimeException(e);
        }
    }

    private void wrappedSubmit(Runnable runnable) {
        try {
            this.executor.submit(runnable).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() != null
                    && e.getCause() instanceof UniformInterfaceException) {
                throw new ShiftFinderException(
                        (UniformInterfaceException) e.getCause());
            }
            throw new RuntimeException(e);
        }
    }

    Collection<Shift> getAllShifts() {
        try {
            XMLShifts shifts = service.path(resourcShifts) //$NON-NLS-1$
                    .accept(MediaType.APPLICATION_XML).get(XMLShifts.class);
            Collection<Shift> set = new HashSet<Shift>();
            for (XMLShift shift : shifts.getShifts()) {
                set.add(new Shift(shift));
            }
            return set;
        } catch (UniformInterfaceException e) {
            throw new ShiftFinderException(e);
        }
    }

}
