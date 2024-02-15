package com.uid2.shared.secure.nitro;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.*;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class AttestationDocument {

    public static AttestationDocument createFrom(byte[] data) throws CborException {
        ByteArrayInputStream docStream = new ByteArrayInputStream(data);
        List<DataItem> docDataItems = new CborDecoder(docStream).decode();
        AttestationDocument aDoc = new AttestationDocument();
        Map docMap = (Map) docDataItems.get(0);
        for (DataItem key : docMap.getKeys()) {
            String keyStr = ((UnicodeString) key).getString();
            if (keyStr.equals("module_id")) aDoc.loadModuleId(docMap.get(key));
            else if (keyStr.equals("digest")) aDoc.loadDigest(docMap.get(key));
            else if (keyStr.equals("timestamp")) aDoc.loadTimeStamp(docMap.get(key));
            else if (keyStr.equals("pcrs")) aDoc.loadPcrs(docMap.get(key));
            else if (keyStr.equals("certificate")) aDoc.loadCertificate(docMap.get(key));
            else if (keyStr.equals("cabundle")) aDoc.loadCaBundle(docMap.get(key));
            else if (keyStr.equals("public_key")) aDoc.loadPublicKey(docMap.get(key));
            else if (keyStr.equals("user_data")) aDoc.loadUserData(docMap.get(key));
            else if (keyStr.equals("nonce")) aDoc.loadNonce(docMap.get(key));
        }
        return aDoc;
    }

    private String moduleId;
    private String digest;
    private BigInteger timestamp;
    private HashMap<Integer, byte[]> pcrs;
    private byte[] certificate;
    private List<byte[]> cabundle;
    private byte[] publicKey;
    private byte[] userData;
    private String userDataString;
    private byte[] nonce;

    private AttestationDocument() {}

    private void loadTimeStamp(DataItem d) {
        this.timestamp = ((UnsignedInteger)d).getValue();
    }

    private void loadModuleId(DataItem d) {
        this.moduleId = ((UnicodeString)d).getString();
    }

    private void loadDigest(DataItem d) {
        this.digest = ((UnicodeString)d).getString();
    }

    private void loadPcrs(DataItem d) {
        this.pcrs = new HashMap<>();
        Map pcrMap = (Map) d;
        for (DataItem pcrkey : pcrMap.getKeys()) {
            Integer index = getInt(pcrkey);
            pcrs.put(index, getByteString(pcrMap.get(pcrkey)));
        }
    }

    private void loadCertificate(DataItem d) {
        this.certificate = ((ByteString)d).getBytes();
    }

    private void loadCaBundle(DataItem d) {
        this.cabundle = new ArrayList<>();
        List<DataItem> items = ((Array) d).getDataItems();
        for(DataItem dd : items) {
            this.cabundle.add(((ByteString) dd).getBytes());
        }
    }

    private void loadPublicKey(DataItem d) {
        if(d instanceof SimpleValue) {
            this.publicKey = null;
        } else {
            this.publicKey = ((ByteString) d).getBytes();
        }
    }

    private void loadUserData(DataItem d) {
        if(d instanceof SimpleValue) {
            this.userData = null;
        } else {
            this.userData = ((ByteString) d).getBytes();
            this.userDataString = new String(this.userData, StandardCharsets.UTF_16);
        }
    }

    private void loadNonce(DataItem d) {
        if(d instanceof SimpleValue) {
            this.nonce = null;
        } else {
            this.nonce = ((ByteString) d).getBytes();
        }
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getDigest() {
        return digest;
    }

    public BigInteger getTimestamp() {
        return timestamp;
    }

    public byte[] getPcr(int index) {
        return pcrs.getOrDefault(index, null);
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public CertPath getCertPath() throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        List<Certificate> path = this.cabundle.stream()
                .map(b -> {
                    try {
                        return cf.generateCertificate(new ByteArrayInputStream(b));
                    } catch (CertificateException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());
        Collections.reverse(path);
        return cf.generateCertPath(path);
    }

    public byte[] getUserData() {
        return userData;
    }

    public String getUserDataString() {
        return userDataString;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    private static Integer getInt(DataItem dataItem) {
        if(dataItem instanceof co.nstant.in.cbor.model.Number) {
            return ((co.nstant.in.cbor.model.Number)dataItem).getValue().intValue();
        }
        else if(dataItem instanceof co.nstant.in.cbor.model.NegativeInteger) {
            return ((co.nstant.in.cbor.model.NegativeInteger)dataItem).getValue().intValue();
        }
        else if(dataItem instanceof co.nstant.in.cbor.model.UnsignedInteger) {
            return ((co.nstant.in.cbor.model.UnsignedInteger)dataItem).getValue().intValue();
        }
        return null;
    }

    private static byte[] getByteString(DataItem dataItem) {
        return ((ByteString)dataItem).getBytes();
    }
}
