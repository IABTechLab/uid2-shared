// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.secure.gcp;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.util.ArrayMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

public class InstanceDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceDocument.class);

    private String audience;
    private Instant issuedAt;
    private Instant expiredAt;
    private String subject;
    private boolean instanceConfidentiality;
    private Instant instanceCreatedAt;
    private String instanceId;
    private String projectId;
    private String zone;

    public InstanceDocument(GoogleIdToken vmInstanceDocument) {
        GoogleIdToken.Payload payload = vmInstanceDocument.getPayload();
        this.audience = (String)payload.getAudience();
        this.issuedAt = Instant.ofEpochSecond(payload.getIssuedAtTimeSeconds());
        this.expiredAt = Instant.ofEpochSecond(payload.getExpirationTimeSeconds());
        this.subject = payload.getSubject();
        ArrayMap<String, Object> googleMap = (ArrayMap<String, Object>)payload.get("google");
        ArrayMap<String, Object> computeEngineMap = (ArrayMap<String, Object>)googleMap.get("compute_engine");
        if (computeEngineMap.containsKey("instance_confidentiality")) {
            BigDecimal isConfidential = (BigDecimal) computeEngineMap.get("instance_confidentiality");
            this.instanceConfidentiality = isConfidential.equals(BigDecimal.ONE);
        } else {
            this.instanceConfidentiality = false;
        }
        long createdAt = ((BigDecimal)computeEngineMap.get("instance_creation_timestamp")).longValue();
        this.instanceCreatedAt = Instant.ofEpochSecond(createdAt);
        this.instanceId = (String)computeEngineMap.get("instance_id");
        this.projectId = (String)computeEngineMap.get("project_id");
        this.zone = (String)computeEngineMap.get("zone");

        LOGGER.debug("Received instance document { " + projectId + ", " + zone + ", " + instanceId + " }");
    }

    public String getAudience() {
        return audience;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public String getSubject() {
       return subject;
    }

    public boolean getInstanceConfidentiality() {
        return instanceConfidentiality;
    }

    public Instant getInstanceCreatedAt() {
        return instanceCreatedAt;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getZone() {
        return zone;
    }
}
