package io.zwt.reservationservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceApplication.class, args);
    }

}

@Component
@RequiredArgsConstructor
@Log4j2
class SampleDataInitializer {

    private final ReservationRepository reservationRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {

        var saved = Flux.just("Josh", "Madhura", "Mark", "Olga", "Spencer", "Ria", "Stéphane", "Violetta")
                .map(name -> new Reservation(null, name))
                .flatMap(this.reservationRepository::save);

        this.reservationRepository.deleteAll()
                .thenMany(saved)
                .thenMany(this.reservationRepository.findAll())
                .subscribe(log::info);
    }

}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, String> {

}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
class Reservation {

    @Id
    private String id;
    private String name;
}
