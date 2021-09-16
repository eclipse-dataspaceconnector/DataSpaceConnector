package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.testFixtures.TemporaryKeyLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;




class WriteRequestWriterReaderTest {

    @Test
    void verifyWriteRead() throws Exception {
        var key = TemporaryKeyLoader.loadKeys();
        var privateKey = key.toRSAPrivateKey();
        var publicKey = key.toRSAPublicKey();

        var objectMapper = new ObjectMapper();

        var jwe = new WriteRequestWriter()
                .privateKey(privateKey)
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .commitObject(Map.of("foo", "bar"))
                .kid("kid")
                .sub("sub")
                .context("Foo")
                .type("Bar").buildJwe();

        var commit = new WriteRequestReader().privateKey(privateKey).mapper(objectMapper).verifier((d) -> true).jwe(jwe).readCommit();

        //noinspection unchecked
        Assertions.assertEquals("bar", ((Map<String, String>) commit.getPayload()).get("foo"));
    }



}
