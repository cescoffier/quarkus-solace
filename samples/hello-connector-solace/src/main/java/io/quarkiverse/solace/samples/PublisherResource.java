package io.quarkiverse.solace.samples;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;

@Path("/hello")
public class PublisherResource {

    @Channel("hello")
    MutinyEmitter<Person> foobar;

    @POST
    public Uni<Void> publish(Person person) {
        return foobar.send(person);
    }

}
