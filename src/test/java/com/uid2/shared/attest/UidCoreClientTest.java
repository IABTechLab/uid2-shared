package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.cloud.ICloudStorage;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UidCoreClientTest{
    private ICloudStorage contentStorage;
    private Proxy proxy = CloudUtils.defaultProxy;
    private boolean allowContentFromLocalFileSystem = false;
    private AtomicReference<Handler<Integer>> responseWatcher;
    private AttestationTokenRetriever attestationTokenRetriever = mock(AttestationTokenRetriever.class);
    private UidCoreClient uidCoreClient = new UidCoreClient("core_attest_url", "userToken", new ApplicationVersion("appName", "appVersion"), proxy,
            mock(IAttestationProvider.class), true);

    public UidCoreClientTest() throws Exception {
    }

    public String convertInputStreamToString(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    @Test
    public void testDownload() throws IOException, CloudStorageException {
        uidCoreClient.setAttestationTokenRetriever(attestationTokenRetriever);
        URLConnection mockConn = mock(URLConnection.class);
        JsonArray content = new JsonArray();
        content.add("Test1");
        content.add("Test2");
        content.add("Test3");
        when(mockConn.getInputStream()).thenReturn(makeInputStream(content));
        when(attestationTokenRetriever.sendGet("fakeUrl")).thenReturn(mockConn);

        InputStream result = uidCoreClient.download("fakeUrl");
        String expected = "[\"Test1\",\"Test2\",\"Test3\"]\n";

        Assert.assertEquals(expected, convertInputStreamToString(result));
    }

}
