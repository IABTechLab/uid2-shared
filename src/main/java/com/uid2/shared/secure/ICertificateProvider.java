package com.uid2.shared.secure;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface ICertificateProvider {
    X509Certificate getRootCertificate() throws CertificateException;
}
