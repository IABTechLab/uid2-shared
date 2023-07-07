package com.uid2.shared.secret;

public interface IKeyGenerator {
    byte[] generateRandomKey(int keyLen) throws Exception;
    String generateRandomKeyString(int keyLen) throws Exception;
    String generateFormattedKeyString(int keyLen) throws Exception;
}
