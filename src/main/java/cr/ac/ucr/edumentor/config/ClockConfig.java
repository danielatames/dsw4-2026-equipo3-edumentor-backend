package cr.ac.ucr.edumentor.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Reloj de la aplicación en hora de Costa Rica.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Costa_Rica"));
    }
}
