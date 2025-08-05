package org.alfresco.repo.jscript;

import org.alfresco.repo.jscript.auth.OAuthService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.mozilla.javascript.NativeObject;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Exposes a root JavaScript object to Alfresco for triggering Hyland process instances.
 * Used in repository-tier scripts, folder rules, or workflows to start remote BPMN processes.
 */
public class HylandProcessScript extends BaseScopableProcessorExtension {

    /**
     * OAuth service used to retrieve and cache bearer tokens for API calls.
     */
    private OAuthService oauthService;

    /**
     * Base URL of the Hyland Automate/Flowable API endpoint.
     * Should end with /process-instances.
     */
    private String apiUrl;

    /**
     * Entry point for triggering a Hyland process from Alfresco JavaScript.
     *
     * @param processKey  (Optional) The BPMN process definition key. If null or empty, uses the default.
     * @param variables   A JavaScript object (converted to a Java Map) of key-value pairs for the process payload.
     * @return true if the process was successfully started; false otherwise.
     */

     // Method to use apiUrl in alfresco-global.properties config
    public boolean startProcess(String processKey, Object variables) throws Exception {

        // Convert the JS object (NativeObject) into a Java Map
        Map<String, Object> vars = new LinkedHashMap<>();
        if (variables instanceof NativeObject) {
            vars.putAll((NativeObject) variables);
        }
        

        return invokeProcess(processKey, vars);

        
    }
    // Method to use apiUrl from the javascript
     public boolean startProcess(String apiUrl, String processKey, Object variables) throws Exception {

        // Convert the JS object (NativeObject) into a Java Map
        Map<String, Object> vars = new LinkedHashMap<>();
        if (variables instanceof NativeObject) {
            vars.putAll((NativeObject) variables);
        }
        setApiUrl(apiUrl); 

        return invokeProcess(processKey, vars);

        
    }

    /**
     * Constructs and sends a POST request to start a process on the Hyland Process API.
     *
     * @param processKey  The process definition key (from Studio/Flowable).
     * @param vars        Process variables as a key-value map.
     * @return true if the HTTP response status code is 2xx; false otherwise.
     * @throws Exception if any network or OAuth error occurs.
     */
    private boolean invokeProcess(String processKey, Map<String, Object> vars) throws Exception {
        // Build JSON payload
        JSONObject root = new JSONObject();
        root.put("processDefinitionKey", processKey);
        root.put("name", "Triggered from ACS");
        root.put("variables", new JSONObject(vars));
        root.put("payloadType", "StartProcessPayload");

        // Prepare and execute the HTTP request
        try (CloseableHttpClient c = HttpClients.createDefault()) {
            HttpPost p = new HttpPost(apiUrl);
            p.addHeader("Authorization", "Bearer " + oauthService.getToken());
            p.addHeader("Content-Type", "application/json");
            p.setEntity(new StringEntity(root.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse r = c.execute(p)) {
                int code = r.getStatusLine().getStatusCode();
                return code >= 200 && code < 300;
            }
        }
    }

    /**
     * Injects the OAuth service used to retrieve access tokens.
     */
    public void setOauthService(OAuthService oauthService) {
        this.oauthService = oauthService;
    }

    /**
     * Injects the full API URL of the Hyland process instance endpoint.
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

}