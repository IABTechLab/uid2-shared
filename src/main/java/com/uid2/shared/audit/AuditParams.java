package com.uid2.shared.audit;

import java.util.Collections;
import java.util.List;

public record AuditParams(String source, List<String> queryParams, List<String> bodyParams) {
    public AuditParams(String source, List<String> queryParams, List<String> bodyParams) {
        this.queryParams = queryParams != null
                ? Collections.unmodifiableList(queryParams)
                : Collections.emptyList();
        this.bodyParams = bodyParams != null
                ? Collections.unmodifiableList(bodyParams)
                : Collections.emptyList();
        this.source = source;
    }

    public AuditParams(String source) {
        this(source, Collections.emptyList(), Collections.emptyList());
    }
}
