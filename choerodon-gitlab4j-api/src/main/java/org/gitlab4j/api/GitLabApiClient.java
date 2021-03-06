package org.gitlab4j.api;

/*
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2017 Greg Messner <greg@messners.com>
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy of
 *   this software and associated documentation files (the "Software"), to deal in
 *   the Software without restriction, including without limitation the rights to
 *   use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *   the Software, and to permit persons to whom the Software is furnished to do so,
 *   subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *   FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.Constants.TokenType;
import org.gitlab4j.api.GitLabApi.ApiVersion;
import org.gitlab4j.api.utils.JacksonJson;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

/**
 * This class utilizes the Jersey client package to communicate with a GitLab API endpoint.
 */
public class GitLabApiClient {

    protected static final String PRIVATE_TOKEN_HEADER = "PRIVATE-TOKEN";
    protected static final String SUDO_HEADER = "Sudo";
    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String X_GITLAB_TOKEN_HEADER = "X-Gitlab-Token";

    private ClientConfig clientConfig;
    private Client apiClient;
    private String hostUrl;
    private TokenType tokenType = TokenType.PRIVATE;
    private String authToken;
    private String secretToken;
    private boolean ignoreCertificateErrors;
    private SSLContext openSslContext;
    private HostnameVerifier openHostnameVerifier;
    private Integer sudoAsId;

    /**
     * Construct an instance to communicate with a GitLab API server using the specified GitLab API version,
     * server URL, private token, and secret token.
     *
     * @param apiVersion   the ApiVersion specifying which version of the API to use
     * @param hostUrl      the URL to the GitLab API server
     * @param privateToken the private token to authenticate with
     */
    public GitLabApiClient(ApiVersion apiVersion, String hostUrl, String privateToken) {
        this(apiVersion, hostUrl, TokenType.PRIVATE, privateToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using the specified GitLab API version,
     * server URL, auth token type, private or access token, and secret token.
     *
     * @param apiVersion the ApiVersion specifying which version of the API to use
     * @param hostUrl    the URL to the GitLab API server
     * @param tokenType  the type of auth the token is for, PRIVATE or ACCESS
     * @param authToken  the token to authenticate with
     */
    public GitLabApiClient(ApiVersion apiVersion, String hostUrl, TokenType tokenType, String authToken) {
        this(apiVersion, hostUrl, tokenType, authToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using GitLab API version 4, and the specified
     * server URL, private token, and secret token.
     *
     * @param hostUrl      the URL to the GitLab API server
     * @param privateToken the private token to authenticate with
     */
    public GitLabApiClient(String hostUrl, String privateToken) {
        this(ApiVersion.V4, hostUrl, TokenType.PRIVATE, privateToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using GitLab API version 4, and the specified
     * server URL, private token, and secret token.
     *
     * @param hostUrl   the URL to the GitLab API server
     * @param tokenType the type of auth the token is for, PRIVATE or ACCESS
     * @param authToken the token to authenticate with
     */
    public GitLabApiClient(String hostUrl, TokenType tokenType, String authToken) {
        this(ApiVersion.V4, hostUrl, tokenType, authToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using the specified GitLab API version,
     * server URL, private token, and secret token.
     *
     * @param apiVersion   the ApiVersion specifying which version of the API to use
     * @param hostUrl      the URL to the GitLab API server
     * @param privateToken the private token to authenticate with
     * @param secretToken  use this token to validate received payloads
     */
    public GitLabApiClient(ApiVersion apiVersion, String hostUrl, String privateToken, String secretToken) {
        this(apiVersion, hostUrl, TokenType.PRIVATE, privateToken, secretToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using the specified GitLab API version,
     * server URL, private token, and secret token.
     *
     * @param apiVersion  the ApiVersion specifying which version of the API to use
     * @param hostUrl     the URL to the GitLab API server
     * @param tokenType   the type of auth the token is for, PRIVATE or ACCESS
     * @param authToken   the token to authenticate with
     * @param secretToken use this token to validate received payloads
     */
    public GitLabApiClient(ApiVersion apiVersion, String hostUrl, TokenType tokenType, String authToken, String secretToken) {
        this(apiVersion, hostUrl, tokenType, authToken, secretToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using GitLab API version 4, and the specified
     * server URL, private token, and secret token.
     *
     * @param hostUrl      the URL to the GitLab API server
     * @param privateToken the private token to authenticate with
     * @param secretToken  use this token to validate received payloads
     */
    public GitLabApiClient(String hostUrl, String privateToken, String secretToken) {
        this(ApiVersion.V4, hostUrl, TokenType.PRIVATE, privateToken, secretToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using GitLab API version 4, and the specified
     * server URL, private token, and secret token.
     *
     * @param hostUrl     the URL to the GitLab API server
     * @param tokenType   the type of auth the token is for, PRIVATE or ACCESS
     * @param authToken   the token to authenticate with
     * @param secretToken use this token to validate received payloads
     */
    public GitLabApiClient(String hostUrl, TokenType tokenType, String authToken, String secretToken) {
        this(ApiVersion.V4, hostUrl, tokenType, authToken, secretToken, null);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using GitLab API version 4, and the specified
     * server URL and private token.
     *
     * @param hostUrl                the URL to the GitLab API server
     * @param privateToken           the private token to authenticate with
     * @param secretToken            use this token to validate received payloads
     * @param clientConfigProperties the properties given to Jersey's clientconfig
     */
    public GitLabApiClient(String hostUrl, String privateToken, String secretToken, Map<String, Object> clientConfigProperties) {
        this(ApiVersion.V4, hostUrl, TokenType.PRIVATE, privateToken, secretToken, clientConfigProperties);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using the specified GitLab API version,
     * server URL and private token.
     *
     * @param apiVersion             the ApiVersion specifying which version of the API to use
     * @param hostUrl                the URL to the GitLab API server
     * @param privateToken           the private token to authenticate with
     * @param secretToken            use this token to validate received payloads
     * @param clientConfigProperties the properties given to Jersey's clientconfig
     */
    public GitLabApiClient(ApiVersion apiVersion, String hostUrl, String privateToken, String secretToken, Map<String, Object> clientConfigProperties) {
        this(apiVersion, hostUrl, TokenType.PRIVATE, privateToken, secretToken, clientConfigProperties);
    }

    /**
     * Construct an instance to communicate with a GitLab API server using the specified GitLab API version,
     * server URL and private token.
     *
     * @param apiVersion             the ApiVersion specifying which version of the API to use
     * @param hostUrl                the URL to the GitLab API server
     * @param tokenType              the type of auth the token is for, PRIVATE or ACCESS
     * @param authToken              the private token to authenticate with
     * @param secretToken            use this token to validate received payloads
     * @param clientConfigProperties the properties given to Jersey's clientconfig
     */
    public GitLabApiClient(ApiVersion apiVersion, String hostUrl, TokenType tokenType, String authToken, String secretToken, Map<String, Object> clientConfigProperties) {

        // Remove the trailing "/" from the hostUrl if present
        this.hostUrl = (hostUrl.endsWith("/") ? hostUrl.replaceAll("/$", "") : hostUrl) + apiVersion.getApiNamespace();
        this.tokenType = tokenType;
        this.authToken = authToken;

        if (secretToken != null) {
            secretToken = secretToken.trim();
            secretToken = (secretToken.length() > 0 ? secretToken : null);
        }

        this.secretToken = secretToken;

        clientConfig = new ClientConfig();
        if (clientConfigProperties != null) {
            for (Map.Entry<String, Object> propertyEntry : clientConfigProperties.entrySet()) {
                clientConfig.property(propertyEntry.getKey(), propertyEntry.getValue());
            }
        }

        clientConfig.register(JacksonJson.class);
    }

    /**
     * Set the ID of the user to sudo as.
     *
     * @param sudoAsId the ID of the user to sudo as
     */
    Integer getSudoAsId() {
        return (sudoAsId);
    }

    /**
     * Set the ID of the user to sudo as.
     *
     * @param sudoAsId the ID of the user to sudo as
     */
    void setSudoAsId(Integer sudoAsId) {
        this.sudoAsId = sudoAsId;
    }

    /**
     * Construct a REST URL with the specified path arguments.
     *
     * @param pathArgs variable list of arguments used to build the URI
     * @return a REST URL with the specified path arguments
     * @throws IOException if an error occurs while constructing the URL
     */
    protected URL getApiUrl(Object... pathArgs) throws IOException {

        StringBuilder url = new StringBuilder();
        url.append(hostUrl);
        for (Object pathArg : pathArgs) {
            if (pathArg != null) {
                url.append("/");
                url.append(pathArg.toString());
            }
        }

        return (new URL(url.toString()));
    }

    /**
     * Validates the secret token (X-GitLab-Token) header against the expected secret token, returns true if valid,
     * otherwise returns false.
     *
     * @param response the Response instance sent from the GitLab server
     * @return true if the response's secret token is valid, otherwise returns false
     */
    protected boolean validateSecretToken(Response response) {

        if (this.secretToken == null)
            return (true);

        String secretToken = response.getHeaderString(X_GITLAB_TOKEN_HEADER);
        if (secretToken == null)
            return (false);

        return (this.secretToken.equals(secretToken));
    }

    /**
     * Perform an HTTP GET call with the specified query parameters and path objects, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param pathArgs    variable list of arguments used to build the URI
     * @return a ClientResponse instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response get(MultivaluedMap<String, String> queryParams, Object... pathArgs) throws IOException {
        URL url = getApiUrl(pathArgs);
        return (get(queryParams, url));
    }

    /**
     * Perform an HTTP GET call with the specified query parameters and URL, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param url         the fully formed path to the GitLab API endpoint
     * @return a ClientResponse instance with the data returned from the endpoint
     */
    protected Response get(MultivaluedMap<String, String> queryParams, URL url) {
        return (invocation(url, queryParams).get());
    }

    /**
     * Perform an HTTP GET call with the specified query parameters and path objects, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param accepts     if non-empty will set the Accepts header to this value
     * @param pathArgs    variable list of arguments used to build the URI
     * @return a ClientResponse instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response getWithAccepts(MultivaluedMap<String, String> queryParams, String accepts, Object... pathArgs) throws IOException {
        URL url = getApiUrl(pathArgs);
        return (getWithAccepts(queryParams, url, accepts));
    }

    /**
     * Perform an HTTP GET call with the specified query parameters and URL, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param url         the fully formed path to the GitLab API endpoint
     * @param accepts     if non-empty will set the Accepts header to this value
     * @return a ClientResponse instance with the data returned from the endpoint
     */
    protected Response getWithAccepts(MultivaluedMap<String, String> queryParams, URL url, String accepts) {
        return (invocation(url, queryParams, accepts).get());
    }

    /**
     * Perform an HTTP POST call with the specified form data and path objects, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param formData the Form containing the name/value pairs
     * @param pathArgs variable list of arguments used to build the URI
     * @return a ClientResponse instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response post(Form formData, Object... pathArgs) throws IOException {
        URL url = getApiUrl(pathArgs);
        return post(formData, url);
    }

    /**
     * Perform an HTTP POST call with the specified payload object and URL, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param payload  the object instance that will be serialized to JSON and used as the POST data
     * @param pathArgs variable list of arguments used to build the URI
     * @return a ClientResponse instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response post(Object payload, Object... pathArgs) throws IOException {
        URL url = getApiUrl(pathArgs);
        Entity<?> entity = Entity.entity(payload, MediaType.APPLICATION_JSON);
        return (invocation(url, null).post(entity));
    }

    /**
     * Perform an HTTP POST call with the specified form data and path objects, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param pathArgs    variable list of arguments used to build the URI
     * @return a Response instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response post(MultivaluedMap<String, String> queryParams, Object... pathArgs) throws IOException {
        URL url = getApiUrl(pathArgs);
        return post(queryParams, url);
    }

    /**
     * Perform an HTTP POST call with the specified form data and URL, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param formData the Form containing the name/value pairs
     * @param url      the fully formed path to the GitLab API endpoint
     * @return a ClientResponse instance with the data returned from the endpoint
     */
    protected Response post(Form formData, URL url) {
        if (formData instanceof GitLabApiForm)
            return (invocation(url, null).post(Entity.entity(formData.asMap(), MediaType.APPLICATION_FORM_URLENCODED_TYPE)));
        else
            return (invocation(url, null).post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE)));
    }

    /**
     * Perform an HTTP POST call with the specified form data and URL, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parametersformData the Form containing the name/value pairs
     * @param url         the fully formed path to the GitLab API endpoint
     * @return a ClientResponse instance with the data returned from the endpoint
     */
    protected Response post(MultivaluedMap<String, String> queryParams, URL url) {
        return (invocation(url, queryParams).post(null));
    }

    /**
     * Perform an HTTP PUT call with the specified form data and path objects, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param pathArgs    variable list of arguments used to build the URI
     * @return a ClientResponse instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response put(MultivaluedMap<String, String> queryParams, Object... pathArgs) throws IOException {
        URL url = getApiUrl(pathArgs);
        return (put(queryParams, url));
    }

    /**
     * Perform an HTTP PUT call with the specified form data and URL, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param url         the fully formed path to the GitLab API endpoint
     * @return a ClientResponse instance with the data returned from the endpoint
     */
    protected Response put(MultivaluedMap<String, String> queryParams, URL url) {
        return (invocation(url, null).put(Entity.entity(queryParams, MediaType.APPLICATION_FORM_URLENCODED_TYPE)));
    }

    /**
     * Perform an HTTP PUT call with the specified form data and path objects, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param formData the Form containing the name/value pairs
     * @param pathArgs variable list of arguments used to build the URI
     * @return a ClientResponse instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response put(Form formData, Object... pathArgs) throws IOException {
        URL url = getApiUrl(pathArgs);
        return put(formData, url);
    }

    /**
     * Perform an HTTP PUT call with the specified form data and URL, returning
     * a ClientResponse instance with the data returned from the endpoint.
     *
     * @param formData the Form containing the name/value pairs
     * @param url      the fully formed path to the GitLab API endpoint
     * @return a ClientResponse instance with the data returned from the endpoint
     */
    protected Response put(Form formData, URL url) {
        if (formData instanceof GitLabApiForm)
            return (invocation(url, null).put(Entity.entity(formData.asMap(), MediaType.APPLICATION_FORM_URLENCODED_TYPE)));
        else
            return (invocation(url, null).put(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE)));
    }

    /**
     * Perform an HTTP DELETE call with the specified form data and path objects, returning
     * a Response instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param pathArgs    variable list of arguments used to build the URI
     * @return a Response instance with the data returned from the endpoint
     * @throws IOException if an error occurs while constructing the URL
     */
    protected Response delete(MultivaluedMap<String, String> queryParams, Object... pathArgs) throws IOException {
        return (delete(queryParams, getApiUrl(pathArgs)));
    }

    /**
     * Perform an HTTP DELETE call with the specified form data and URL, returning
     * a Response instance with the data returned from the endpoint.
     *
     * @param queryParams multivalue map of request parameters
     * @param url         the fully formed path to the GitLab API endpoint
     * @return a Response instance with the data returned from the endpoint
     */
    protected Response delete(MultivaluedMap<String, String> queryParams, URL url) {
        return (invocation(url, queryParams).delete());
    }

    protected Invocation.Builder invocation(URL url, MultivaluedMap<String, String> queryParams) {
        return (invocation(url, queryParams, MediaType.APPLICATION_JSON));
    }

    protected Invocation.Builder invocation(URL url, MultivaluedMap<String, String> queryParams, String accept) {

        if (apiClient == null) {
            if (ignoreCertificateErrors) {
                apiClient = ClientBuilder.newBuilder()
                        .withConfig(clientConfig)
                        .sslContext(openSslContext)
                        .hostnameVerifier(openHostnameVerifier)
                        .build();
            } else {
                apiClient = ClientBuilder.newBuilder().withConfig(clientConfig).build();
            }
        }

        WebTarget target = apiClient.target(url.toExternalForm()).property(ClientProperties.FOLLOW_REDIRECTS, true);
        if (queryParams != null) {
            for (Map.Entry<String, List<String>> param : queryParams.entrySet()) {
                target = target.queryParam(param.getKey(), param.getValue().toArray());
            }
        }

        String authHeader = (tokenType == TokenType.ACCESS ? AUTHORIZATION_HEADER : PRIVATE_TOKEN_HEADER);
        String authValue = (tokenType == TokenType.ACCESS ? "Bearer " + authToken : authToken);
        Invocation.Builder builder = target.request();
        if (accept == null || accept.trim().length() == 0) {
            builder = builder.header(authHeader, authValue);
        } else {
            builder = builder.header(authHeader, authValue).accept(accept);
        }

        // If sudo as ID is set add the Sudo header
        if (sudoAsId != null && sudoAsId.intValue() > 0)
            builder = builder.header(SUDO_HEADER, sudoAsId);

        return (builder);
    }

    /**
     * Returns true if the API is setup to ignore SSL certificate errors, otherwise returns false.
     *
     * @return true if the API is setup to ignore SSL certificate errors, otherwise returns false
     */
    public boolean getIgnoreCertificateErrors() {
        return (ignoreCertificateErrors);
    }

    /**
     * Sets up the Jersey system ignore SSL certificate errors or not.
     *
     * @param ignoreCertificateErrors if true will set up the Jersey system ignore SSL certificate errors
     */
    public void setIgnoreCertificateErrors(boolean ignoreCertificateErrors) {

        if (this.ignoreCertificateErrors == ignoreCertificateErrors) {
            return;
        }

        if (!ignoreCertificateErrors) {

            this.ignoreCertificateErrors = false;
            openSslContext = null;
            openHostnameVerifier = null;
            apiClient = null;

        } else {

            if (setupIgnoreCertificateErrors()) {
                this.ignoreCertificateErrors = true;
                apiClient = null;
            } else {
                this.ignoreCertificateErrors = false;
                apiClient = null;
                throw new RuntimeException("Unable to ignore certificate errors.");
            }
        }
    }

    /**
     * Sets up Jersey client to ignore certificate errors.
     *
     * @return true if successful at setting up to ignore certificate errors, otherwise returns false.
     */
    private boolean setupIgnoreCertificateErrors() {

        // Create a TrustManager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{new X509ExtendedTrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            }
        }};

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            openSslContext = sslContext;
            openHostnameVerifier = hostnameVerifier;
        } catch (GeneralSecurityException ex) {
            openSslContext = null;
            openHostnameVerifier = null;
            return (false);
        }

        return (true);
    }
}
