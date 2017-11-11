package stinc.male.server.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import stinc.male.server.Server;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestExampleSpringHttpServer_SpringConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public final class TestExampleSpringHttpServer {
  private static final String request(final String url) throws Exception {
    final URLConnection connection = new URL(url).openConnection();
    final String result;
    try (final BufferedReader data = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
      result = data.readLine();
    }
    return result;
  }

  @Inject
  Server server;

  @Inject
  @Value("${prop.port}")
  private int port;

  @Test
  public final void testRoot() throws Exception {
    assertEquals(
        TestExampleSpringHttpServer_RootProcessor.RESPONSE,
        request(String.format("http://localhost:%s/", port)));
  }

  @Test
  public final void testHelloWorld() throws Exception {
    assertEquals(
        TestExampleSpringHttpServer_HelloWorldProcessor.RESPONSE,
        request(String.format("http://localhost:%s/context/path/helloWorld", port)));
  }

  @Test
  public final void testChunkedRequest() throws Exception {
    final HttpURLConnection connection = (HttpURLConnection)new URL(String.format("http://localhost:%s/", port))
        .openConnection();
    connection.setConnectTimeout(3000);
    connection.setReadTimeout(3000);
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setChunkedStreamingMode(1000);
    try {
      try (final OutputStream out = connection.getOutputStream()) {
        out.write(new byte[5000]);
        out.flush();
      }
      try (final BufferedReader data = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        assertEquals(TestExampleSpringHttpServer_RootProcessor.RESPONSE, data.readLine());
      }
    } finally {
      connection.disconnect();
    }
  }
}