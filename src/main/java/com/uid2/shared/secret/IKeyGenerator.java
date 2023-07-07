package com.uid2.shared.secret;

public interface IKeyGenerator {
    byte[] generateRandomKey(int keyLen) throws Exception;
    String generateRandomKeyString(int keyLen) throws Exception;

    // To make secrets easier to scan for using a RegEx and reduce the likelihood of keys being leaked, this
    // inserts a . as the 6th character. If the generated string is shorter than 6 characters, no change is made,
    // and this would return the same as generateRandomKey
    String generateFormattedKeyString(int keyLen) throws Exception;
}
