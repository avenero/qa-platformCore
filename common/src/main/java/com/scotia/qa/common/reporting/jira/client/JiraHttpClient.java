package com.scotia.qa.common.reporting.jira.client;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.JiraConfig;
import com.scotia.qa.common.utils.SSLUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cliente HTTP para interactuar con Jira/Xray REST API.
 *
 * <p>Configurado automáticamente con SSL usando el truststore del framework
 * a través de {@link SSLUtils}.</p>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class JiraHttpClient {

    private final JiraConfig config;
    private final CloseableHttpClient httpClient;
    private final String authHeader;

    public JiraHttpClient(JiraConfig config) {
        this.config = config;

        // Configurar credenciales HTTP Basic
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(config.getUser(), config.getPassword())
        );

        // Crear HttpClient con SSL configurado usando SSLUtils
        this.httpClient = SSLUtils.createSecureHttpClient(credentialsProvider);

        // Preparar Authorization header
        String auth = config.getUser() + ":" + config.getPassword();
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
            auth.getBytes(StandardCharsets.UTF_8)
        );

        TestLogger.logDebug("JIRA_CLIENT", "JiraHttpClient inicializado con SSL del framework", null);
    }

    public String get(String endpoint) throws IOException {
        String url = buildUrl(endpoint);
        TestLogger.logDebug("JIRA_CLIENT", "GET: " + url, null);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", authHeader);
        request.setHeader("Accept", "application/json");

        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            throw new IOException(
                String.format("Jira GET failed: %d - %s", statusCode, responseBody)
            );
        }

        return responseBody;
    }

    public String post(String endpoint, String jsonBody) throws IOException {
        String url = buildUrl(endpoint);
        TestLogger.logDebug("JIRA_CLIENT", "POST: " + url, null);

        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", authHeader);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");

        if (jsonBody != null) {
            request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }

        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            throw new IOException(
                String.format("Jira POST failed: %d - %s", statusCode, responseBody)
            );
        }

        return responseBody;
    }

    public String put(String endpoint, String jsonBody) throws IOException {
        String url = buildUrl(endpoint);
        TestLogger.logDebug("JIRA_CLIENT", "PUT: " + url, null);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", authHeader);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");

        if (jsonBody != null) {
            request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }

        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            throw new IOException(
                String.format("Jira PUT failed: %d - %s", statusCode, responseBody)
            );
        }

        return responseBody;
    }

    public String postAttachment(String issueKey, File file) throws IOException {
        String url = buildUrl("/rest/api/2/issue/" + issueKey + "/attachments");
        TestLogger.logDebug("JIRA_CLIENT",
            String.format("POST attachment: %s to %s", file.getName(), issueKey), null);

        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", authHeader);
        request.setHeader("X-Atlassian-Token", "no-check");

        HttpEntity multipart = MultipartEntityBuilder.create()
            .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
            .build();

        request.setEntity(multipart);

        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            throw new IOException(
                String.format("Jira attachment upload failed: %d - %s", statusCode, responseBody)
            );
        }

        TestLogger.logDebug("JIRA_CLIENT",
            String.format("✓ Attachment uploaded: %s", file.getName()), null);

        return responseBody;
    }

    private String buildUrl(String endpoint) {
        String base = config.getUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return base + endpoint;
    }

    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
