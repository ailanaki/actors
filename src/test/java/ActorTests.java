import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.PatternsCS;

import org.example.actors.MasterActor;
import org.example.model.Request;
import org.example.model.Responce;
import org.example.server.StubServer;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


public class ActorTests {
    private static final Duration defaultTimeout = Duration.ofMillis(500);
    private static final Duration askTimeout = Duration.ofSeconds(20);
    private static final int CLIENT_PORT = 8080;
    private static final int PORT = 8088;
    private static final int TOP = 5;
    private static final String HOST = "http://localhost:";

    private static String getHost(int port, String searcher) {
        return HOST + port + "/" + searcher;
    }

    @Test
    public void oneHostTest() {
        String searcher = "google";
        String host = getHost(PORT, searcher);
        String query = "super query";

        try (StubServer stubServer = new StubServer(PORT, List.of(searcher))) {
            try (StubServer clientServer = new StubServer(CLIENT_PORT)) {
                ActorSystem system = ActorSystem.create("TestSystem");

                ActorRef master = system.actorOf(
                        Props.create(MasterActor.class, List.of(host), defaultTimeout), "master"
                );

                Responce expectedResponse = StubServer.createResponce(searcher, query, TOP);

                Object message = PatternsCS.ask(master, new Request(TOP, query), askTimeout.toMillis())
                        .toCompletableFuture().join();

                assertTrue(message instanceof Map);
                Map<String, Responce> actualResponses = (Map<String, Responce>) message;

                assertEquals(1, actualResponses.size());

                for (Map.Entry<String, Responce> entry : actualResponses.entrySet()) {
                    assertEquals(searcher, entry.getKey());
                    assertEquals(expectedResponse, entry.getValue());
                }
            }
        }
    }


    @Test
    public void severalHostsTest() {
        List<String> searchers = List.of("google", "yandex", "bing");
        List<String> hosts = searchers.stream().map(searcher -> getHost(PORT, searcher)).toList();
        String query = "super query";

        try (StubServer stubServer = new StubServer(PORT, searchers)) {
            try (StubServer clientServer = new StubServer(CLIENT_PORT)) {
                ActorSystem system = ActorSystem.create("TestSystem");

                ActorRef master = system.actorOf(
                        Props.create(MasterActor.class, hosts, defaultTimeout), "master"
                );

                Object message = PatternsCS.ask(master, new Request(TOP, query), askTimeout.toMillis())
                        .toCompletableFuture().join();

                assertTrue(message instanceof Map);
                Map<String, Responce> actualResponses = (Map<String, Responce>) message;

                assertEquals(searchers.size(), actualResponses.size());

                for (Map.Entry<String, Responce> entry : actualResponses.entrySet()) {
                    assertTrue(searchers.contains(entry.getKey()));
                    assertEquals(StubServer.createResponce(entry.getKey(), query, TOP), entry.getValue());
                }
            }
        }
    }

    @Test
    public void slowHostTest() {
        String searcher = "google";
        String host = getHost(PORT, searcher);
        String query = "super query";

        try (StubServer stubServer = new StubServer(PORT, List.of(searcher), defaultTimeout.plusSeconds(1))) {
            try (StubServer clientServer = new StubServer(CLIENT_PORT)) {
                ActorSystem system = ActorSystem.create("TestSystem");

                ActorRef master = system.actorOf(
                        Props.create(MasterActor.class, List.of(host), defaultTimeout), "master"
                );

                Responce expectedResponse = StubServer.createResponce(searcher, query, TOP);

                Object message = PatternsCS.ask(master, new Request(TOP, query), askTimeout.toMillis())
                        .toCompletableFuture().join();

                assertTrue(message instanceof Map);
                Map<String, Responce> actualResponses = (Map<String, Responce>) message;

                assertEquals(0, actualResponses.size());

                for (Map.Entry<String, Responce> entry : actualResponses.entrySet()) {
                    assertEquals(searcher, entry.getKey());
                    assertEquals(expectedResponse, entry.getValue());
                }
            }
        }
    }
}