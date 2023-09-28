package com.uid2.shared.secure.azurecc;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RuntimeData {
    private String location;
    private String publicKey;
}
