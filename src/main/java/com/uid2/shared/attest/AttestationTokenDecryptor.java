package com.uid2.shared.attest;

import com.uid2.shared.Const;

import javax.crypto.Cipher;
import java.security.PrivateKey;

public class AttestationTokenDecryptor {
    public byte[] decrypt(byte[] payload, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(Const.Name.AsymetricEncryptionCipherClass);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(payload);
    }
}
