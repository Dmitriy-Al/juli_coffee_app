package julia.cafe.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;


@Data
@EnableScheduling
@Configuration
@PropertySource("application.properties")
public class Config {

    @Value("${bot.name}") // имя бота в BotFather
    public String botName;

    @Value("${bot.token}") // токен бота в BotFather
    public String botToken;

    @Value("${account.admin}") // имя учётной записи администратора
    public String ADMIN = "admin";

    @Value("${account.barista}") // имя учётной записи бариста
    public String BARISTA = "barista";


}