package io.zwt.reservationservice;

import io.r2dbc.spi.ConnectionFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.nio.channels.FileLock;

@SpringBootApplication
public class ReservationServiceApplication {

    @Bean
    ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory cf) {
        return new R2dbcTransactionManager(cf);
    }

    @Bean
    TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }


    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceApplication.class, args);
    }

}

@Service
@RequiredArgsConstructor
class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TransactionalOperator transactionalOperator;

    Flux<Reservation> saveAll(String... names) {
        return this.transactionalOperator
                .transactional(
                Flux
        .fromArray(names)
        .map(name -> new Reservation(null, name))
        .flatMap(this.reservationRepository::save)
        .doOnNext(r -> Assert.isTrue(isValid(r), "The name must have a capital first letter!")));
    }

    private boolean isValid(Reservation r) {
        return Character.isUpperCase(r.getName().charAt(0));
    }
}

@Component
@RequiredArgsConstructor
@Log4j2
class SampleDataInitializer {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {

        var saved = this.reservationService.saveAll("Josh", "Madhura", "Mark", "Olga", "Spencer", "Ria", "Stéphane", "Violetta");

        this.reservationRepository
                .deleteAll()
                .thenMany(saved)
                .thenMany(this.reservationRepository.findAll())
                .subscribe(log::info);
    }

}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

    @Id
    private Integer id;
    private String name;
}
