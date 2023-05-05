package org.example.actors;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import org.example.model.Request;
import org.example.model.Responce;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MasterActor extends UntypedActor {

    private final List<String> hosts;
    private final Map<String, Responce> responses;
    private final Duration timeout;
    private ActorRef parent;

    public MasterActor(List<String> hosts, java.time.Duration timeout) {
        this.hosts = hosts;
        this.timeout = Duration.fromNanos(timeout.toNanos());
        this.responses = new HashMap<>();
    }

    @Override
    public void onReceive(Object message) throws Throwable, Throwable {
        if (message instanceof Request) {
            parent = getSender();
            for (String host : hosts) {
                getContext().actorOf(Props.create(ChildActor.class, host));
            }
            getContext().getChildren().forEach(actorRef -> actorRef.tell(message, getSelf()));
            getContext().setReceiveTimeout(timeout);
        } else if (message instanceof Responce response) {
            responses.put(response.host, response);
            if (responses.size() == hosts.size()) {
                stop();
            }
        } else if (message instanceof ReceiveTimeout) {
            stop();
        }
    }

    private void stop() {
        parent.tell(responses, getSelf());
        getContext().getChildren().forEach(actorRef -> getContext().stop(actorRef));
        getContext().stop(getSelf());
    }
}