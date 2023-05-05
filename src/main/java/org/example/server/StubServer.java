package org.example.server;

import com.google.gson.Gson;
import org.example.model.Responce;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

public class StubServer implements AutoCloseable {


    private final ClientAndServer stubServer;

    public StubServer(int port) {
        stubServer = ClientAndServer.startClientAndServer(port);
    }

    public StubServer(int port, List<String> hosts) {
        this(port, hosts, Duration.ZERO);
    }

    public StubServer(int port, List<String> hosts, Duration duration) {
        stubServer = ClientAndServer.startClientAndServer(port);
        for (String host : hosts) {
            stubServer.when(
                    HttpRequest.request().withMethod("GET").withPath("/" + host)
            ).respond(httpRequest -> {
                Thread.sleep(duration.toMillis());
                int top = Integer.parseInt(httpRequest.getFirstQueryStringParameter("top"));
                String query = httpRequest.getFirstQueryStringParameter("q");
                Gson g = new Gson();
                String responseBody = g.toJson(createResponce(host, query, top));
                return new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(responseBody);
            });
        }
    }

    public static Responce createResponce(String host, String query, int top) {
        return new Responce(host, IntStream.range(0, top).mapToObj(i -> host + "/query result: " + query + " top-" + i).toList());
    }

    public void close() {
        stubServer.close();
    }
}