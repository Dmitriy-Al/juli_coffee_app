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

    @Value("${tme.up}")
    public int tmeUp; // к значению time добавляется tmeUp, т.е. изначальное время до выдачи заказа увеличивается до (+tmeUp * 5) минут

    @Value("${order.limit}")
    public int orderLimit; // ограничение количества товара в корзине

    @Value("${account.admin}")
    public String admin; // имя учётной записи администратора

    @Value("${account.barista}")
    public String barista; // имя учётной записи бариста

    @Value("${bot.register.name}")
    public String botName; // имя бота в BotFather

    @Value("${bot.register.token}")
    public String botToken; // токен бота в BotFather

    @Value("${pay.token}")
    public String payProviderToken; // токен провайдера платежей

    @Value("${font.directory.path}")
    public String fontDirectoryPath; // путь к директории шрифта для pdf файла кассового чека

    @Value("${cash.receipt.directory.path}")
    public String cashReceiptDirectoryPath; // путь к директории сохранения кассового чека


}