/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cd.go.authorization.github.executors;

import cd.go.authorization.github.annotation.MetadataValidator;
import cd.go.authorization.github.annotation.ValidationError;
import cd.go.authorization.github.annotation.ValidationResult;
import cd.go.authorization.github.models.GitHubConfiguration;
import cd.go.authorization.github.providermanager.GitHubProviderManager;
import cd.go.authorization.github.requests.VerifyConnectionRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.HashMap;

import static cd.go.authorization.github.GitHubPlugin.LOG;
import static cd.go.authorization.github.utils.Util.GSON;

public class VerifyConnectionRequestExecutor implements RequestExecutor {
    private final VerifyConnectionRequest request;
    private final GitHubProviderManager providerManager;

    public VerifyConnectionRequestExecutor(VerifyConnectionRequest request) {
        this(request, GitHubProviderManager.getInstance());
    }

    public VerifyConnectionRequestExecutor(VerifyConnectionRequest request, GitHubProviderManager providerManager) {
        this.request = request;
        this.providerManager = providerManager;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        final ValidationResult validationResult = validate();
        if (validationResult.hasErrors()) {
            return validationFailureResponse(validationResult);
        }

        final ValidationResult verifyConnectionResult = verifyConnection(request.githubConfiguration());
        if (verifyConnectionResult.hasErrors()) {
            return verifyConnectionFailureResponse(verifyConnectionResult);
        }

        return successResponse();
    }

    private ValidationResult validate() {
        return new MetadataValidator().validate(request.githubConfiguration());
    }

    private ValidationResult verifyConnection(GitHubConfiguration gitHubConfiguration) {
        final ValidationResult result = new ValidationResult();

        try {
            providerManager.getTemporaryGitHubProvider(gitHubConfiguration).verifyConnection();
        } catch (Exception e) {
            result.addError(new ValidationError("", e.getMessage()));
            LOG.error("[Verify Connection] Verify connection failed with errors.", e);
        }
        return result;
    }

    private GoPluginApiResponse successResponse() {
        return responseWith("success", "Connection ok", null);
    }

    private GoPluginApiResponse verifyConnectionFailureResponse(ValidationResult validationResult) {
        return responseWith("failure", validationResult.errors().get(0).message(), null);
    }

    private GoPluginApiResponse validationFailureResponse(ValidationResult errors) {
        return responseWith("validation-failed", "Validation failed for the given Auth Config", errors);
    }

    private GoPluginApiResponse responseWith(String status, String message, ValidationResult result) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);

        if (result != null && result.hasErrors()) {
            response.put("errors", result.errors());
        }

        return DefaultGoPluginApiResponse.success(GSON.toJson(response));
    }
}
