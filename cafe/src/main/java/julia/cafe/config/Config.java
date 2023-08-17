package julia.cafe.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data // аннотация lombok, автоматически создающая конструкторы класса
@PropertySource("application.properties")
public class Config {

    @Value("${bot.name}") // аннотация @Value присваивает полям значение свойств из application.properties
    public String botName;

    @Value("${bot.token}") // аннотация @Value присваивает полям значение свойств из application.properties
    public String botToken;

}