package com.uid2.shared.attest;

import com.uid2.shared.cloud.ICloudStorage;

public interface IUidCoreClient {
    ICloudStorage getContentStorage();
}
