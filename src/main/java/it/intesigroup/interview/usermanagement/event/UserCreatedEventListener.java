package it.intesigroup.interview.usermanagement.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// §2.2.4 – Consumer dell'evento di creazione utente, implementato con il bus interno di Spring.
// Strategia scelta: "evento interno applicativo" (nessuna dipendenza da broker esterni).
@Component
public class UserCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedEventListener.class);

    // @Async: eseguito su thread separato → non blocca la risposta HTTP al client (§2.2.4 asincrono).
    // AFTER_COMMIT: garantisce che l'evento parta solo se il salvataggio su DB è andato a buon fine;
    //               in caso di rollback della transazione, questo metodo non viene mai invocato.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        log.info("User created event received: userId={}, email={}, createdBy={}",
                event.userId(), event.email(), event.createdBy());
    }
}
