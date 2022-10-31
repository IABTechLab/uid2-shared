package com.uid2.shared.secure.nitro;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnicodeString;
import com.uid2.shared.secure.BadFormatException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Signature;
import java.security.cert.*;
import java.security.interfaces.ECPublicKey;
import java.util.*;

public class AttestationRequest {
    private AttestationDocument attestationDocument;
    private byte[] attestationDocumentRaw;
    private byte[] protectedHeader;
    private byte[] signature;

    public static AttestationRequest createFrom(byte[] data) throws BadFormatException {
        try {
            AttestationRequest aReq = new AttestationRequest();
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            Array mainObject = (Array) new CborDecoder(stream).decode().get(0);
            List<DataItem> dataItems = mainObject.getDataItems();
            aReq.protectedHeader = ((ByteString) dataItems.get(0)).getBytes();
            aReq.attestationDocumentRaw = ((ByteString) dataItems.get(2)).getBytes();
            aReq.attestationDocument = AttestationDocument.createFrom(aReq.attestationDocumentRaw);
            aReq.signature = ((ByteString) dataItems.get(3)).getBytes();
            return aReq;
        } catch (CborException ce) {
            throw new BadFormatException(ce.getMessage(), ce);
        }
    }

    public static AttestationRequest createFrom(String base64data) throws BadFormatException {
        return createFrom(Base64.getDecoder().decode(base64data));
    }

    private AttestationRequest() {}

    public AttestationDocument getAttestationDocument() {
        return attestationDocument;
    }

    public byte[] getProtectedHeader() {
        return protectedHeader;
    }

    public byte[] getAttestationDocumentRaw() {
        return attestationDocumentRaw;
    }

    public byte[] getSignature() {
        return signature;
    }

    public boolean verifyCertChain(X509Certificate rootCertificate) {
        try {
            CertPath certPath = this.attestationDocument.getCertPath();
            CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
            PKIXParameters pkixParameters = new PKIXParameters(createTrustAnchors(rootCertificate));
            pkixParameters.setRevocationEnabled(false);
            cpv.validate(certPath, pkixParameters);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyData() {
        try {
            X509Certificate cert = (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(this.attestationDocument.getCertificate()));
            ECPublicKey pk = (ECPublicKey) cert.getPublicKey();
            Signature sig = Signature.getInstance("SHA384withECDSA");
            sig.initVerify(pk);
            sig.update(this.toCoseSign1());
            return sig.verify(ecRawSignatureToDer(this.signature));
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            return false;
        }
    }

    private byte[] toCoseSign1() throws CborException {
        Array obj = new Array();
        // -- no tag 18 : obj.setTag(new Tag(18));
        obj.add(new UnicodeString("Signature1"));
        obj.add(new ByteString(getProtectedHeader()));
        obj.add(new ByteString(new byte[0]));
        obj.add(new ByteString(this.attestationDocumentRaw));
        ByteArrayOutputStream coseStream = new ByteArrayOutputStream();
        new CborEncoder(coseStream).encode(obj);
        return coseStream.toByteArray();
    }

    private static byte[] ecRawSignatureToDer(byte[] rawSignature) {
        ByteArrayOutputStream derStream = new ByteArrayOutputStream();
        byte[] x = unsignedBigIntToDerBytes(Arrays.copyOfRange(rawSignature, 0, 48));
        byte[] y = unsignedBigIntToDerBytes(Arrays.copyOfRange(rawSignature, 48, 96));
        int seqSize = x.length + y.length + 4;
        derStream.write(0x30); // sig: sequence
        derStream.write(seqSize);
        derStream.write(0x02); // x: integer
        derStream.write(x.length);
        derStream.write(x, 0, x.length);
        derStream.write(0x02); // y: integer
        derStream.write(y.length);
        derStream.write(y, 0, y.length);
        return derStream.toByteArray();
    }

    private static byte[] unsignedBigIntToDerBytes(byte[] bigInt) {
        int numPadsToRemove = 0, i;
        for (i = 0; i < bigInt.length && bigInt[i] == 0x00; i++, numPadsToRemove++) ;

        if (i == bigInt.length) return new byte[]{0x00};
        if (bigInt[i] < 0) numPadsToRemove--;
        if (numPadsToRemove == -1) {
            // needing a byte of padding zero
            byte[] res = new byte[bigInt.length + 1];
            for (int t = 0; t < bigInt.length; t++)
                res[t + 1] = bigInt[t];
            return res;
        } else {
            return Arrays.copyOfRange(bigInt, numPadsToRemove, bigInt.length);
        }
    }

    private Set<TrustAnchor> createTrustAnchors(X509Certificate ...certs) {
        Set<TrustAnchor> trustAnchors = new HashSet<> ();
        for(X509Certificate cert : certs)
            trustAnchors.add( new TrustAnchor(cert, null) );
        return trustAnchors;
    }
}
