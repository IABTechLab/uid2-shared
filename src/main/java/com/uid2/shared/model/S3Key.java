package com.uid2.shared.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S3Key {
    private int id;
    private int siteId;
    private long activated;
    private long created;
    private String secret;
}
