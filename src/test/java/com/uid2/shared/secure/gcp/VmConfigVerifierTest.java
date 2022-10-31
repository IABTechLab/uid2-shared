package com.uid2.shared.secure.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import com.uid2.shared.cloud.CloudUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class VmConfigVerifierTest {

    private static final String testVmConfig = "    [Unit]\n" +
            "    Description=Start UID 2.0 operator as docker container\n" +
            "\n" +
            "    [Service]\n" +
            "    Environment=\"UID2_ENCLAVE_API_TOKEN=test_value_1\"\n" +
            "    Environment=\"UID2_ENCLAVE_IMAGE_ID=test_value_2\"";

    public static void requireCredential() {
        Assume.assumeTrue(System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null);
        Object defaultCredentials = null;
        try {
            defaultCredentials = GoogleCredentials.getApplicationDefault();
        } catch (Exception ex) {}
        Assume.assumeTrue(defaultCredentials != null);
    }

    @Test
    public void testInstancesAttest() throws Exception {
        VmConfigVerifierTest.requireCredential();

        InstanceDocument id = InstanceDocumentVerifierTest.getTestInstanceDocument();
        VmConfigVerifier vmConfigVerifier = new VmConfigVerifier(GoogleCredentials.getApplicationDefault(), null);
        String vmConfigId = vmConfigVerifier.getVmConfigId(id);
        Assert.assertNotNull(vmConfigId);
    }

    @Test
    public void testNullEnclaveParams() throws Exception {
        VmConfigVerifier vmConfigVerifier = new VmConfigVerifier(null, null);
        Assert.assertEquals("abc", vmConfigVerifier.templatizeVmConfig("abc"));
        Assert.assertEquals("#cloud-init\n", vmConfigVerifier.templatizeVmConfig("#cloud-init\n"));
        Assert.assertEquals(testVmConfig, vmConfigVerifier.templatizeVmConfig(testVmConfig));
    }

    @Test
    public void testEmptyEnclaveParams() throws Exception {
        Set<String> emptySet = new HashSet<>();
        VmConfigVerifier vmConfigVerifier = new VmConfigVerifier(null, emptySet);
        Assert.assertEquals("abc", vmConfigVerifier.templatizeVmConfig("abc"));
        Assert.assertEquals("#cloud-init\n", vmConfigVerifier.templatizeVmConfig("#cloud-init\n"));
        Assert.assertEquals(testVmConfig, vmConfigVerifier.templatizeVmConfig(testVmConfig));
    }

    @Test
    public void testSingleEnclaveParam() throws Exception {
        {
            Set<String> set1 = new HashSet<>();
            set1.add("api_token");
            VmConfigVerifier vmConfigVerifier1 = new VmConfigVerifier(null, set1);
            Assert.assertEquals("abc", vmConfigVerifier1.templatizeVmConfig("abc"));
            Assert.assertEquals("#cloud-init\n", vmConfigVerifier1.templatizeVmConfig("#cloud-init\n"));

            String expectedResult1 = testVmConfig.replace("test_value_1", "dummy");
            Assert.assertEquals(expectedResult1, vmConfigVerifier1.templatizeVmConfig(testVmConfig));
        }

        {
            Set<String> set2 = new HashSet<>();
            set2.add("image_id");
            VmConfigVerifier vmConfigVerifier2 = new VmConfigVerifier(null, set2);
            Assert.assertEquals("abc", vmConfigVerifier2.templatizeVmConfig("abc"));
            Assert.assertEquals("#cloud-init\n", vmConfigVerifier2.templatizeVmConfig("#cloud-init\n"));

            String expectedResult2 = testVmConfig.replace("test_value_2", "dummy");
            Assert.assertEquals(expectedResult2, vmConfigVerifier2.templatizeVmConfig(testVmConfig));
        }
    }

    @Test
    public void testEnclaveParams() throws Exception {
        Set<String> set = new HashSet<>();
        set.add("api_token");
        set.add("image_id");
        VmConfigVerifier vmConfigVerifier = new VmConfigVerifier(null, set);
        Assert.assertEquals("abc", vmConfigVerifier.templatizeVmConfig("abc"));
        Assert.assertEquals("#cloud-init\n", vmConfigVerifier.templatizeVmConfig("#cloud-init\n"));

        String expectedResult = testVmConfig.replace("test_value_1", "dummy")
                .replace("test_value_2", "dummy");
        Assert.assertEquals(expectedResult, vmConfigVerifier.templatizeVmConfig(testVmConfig));

    }
}
