package {{package}}.app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.web.util.HtmlUtils;

import static org.assertj.core.api.Assertions.assertThat;

class HomePageTest {

    @Test
    void rendersHomePageOnServer() throws Exception {
        try (var application = (ServletWebServerApplicationContext) SpringApplication.run(Main.class, "--server.port=0")) {
            int port = application.getWebServer().getPort();
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(HtmlUtils.htmlUnescape(response.body())).contains("<h1>{{name}}</h1>");
        }
    }
}
