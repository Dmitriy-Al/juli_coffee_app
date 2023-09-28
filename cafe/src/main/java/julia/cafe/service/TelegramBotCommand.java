package julia.cafe.service;

import julia.cafe.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
@Component
public class TelegramBotCommand extends TelegramLongPollingBot {

    private final LinkedHashMap<Integer, String> ORDER_NUMBER = new LinkedHashMap<>();
    private final HashMap<Long, String> TEMP_DATA = new HashMap<>();
    private final HashMap<Long, String> USER_BIRTHDAY = new HashMap<>();
    private final HashMap<Long, List<Optional<Product>>> GROCERY_BASKET = new HashMap<>();
    private final HashMap<Long, String> USER_FIRST_NAME = new HashMap<>();
    private final TelegramBotMethods method = new TelegramBotMethods();
    private final String DEFAULT_MAIN_PICTURE_LINK = "";
    private final String SYRUP_PICTURE_LINK = "";
    private final String SUPPLEMENT_MAIN_PICTURE_LINK = "";
    private String mainPictureLink = null;
    private final String ADMIN = "admin";
    private final String BARISTA = "barista";
    private LocalTime tempTime = null;


    @Autowired
    public UserRepository userRepository;
    @Autowired
    public ProductRepository productRepository;
    @Autowired
    public MenuCategoryRepository menuCategoryRepository;


    public TelegramBotCommand() {
        super("");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String stringChatId = String.valueOf(chatId);

            if (messageText.equals("/start")) {
                String userName = update.getMessage().getChat().getFirstName();
                Optional<User> user = userRepository.findById(chatId);

                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Здравствуйте, " + userName + "!\nМы рады предложить вам (текст с дифирамбами)\n" + //TODO stringChatId
                            "Вы можете заказать и оплатить ваш кофе прямо сейчас и забрать ваш готовый заказ без ожидания \uD83D\uDD50");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Здравствуйте, " + userName + "!");
                    method.setAdminKeyBoard(sendMessage);
                    executeSendMessage(sendMessage);
                }
                if (user.isEmpty() || user.get().getUserName().equals("Unregistered")) {
                    setUserInDB(chatId, userName, true, null, null, null, null, new Timestamp(System.currentTimeMillis()), null);
                }

            } else if (messageText.equals("☕️" + " Кофе и напитки")) { // клавиатура
                List<MenuCategory> menuCategories = (List<MenuCategory>) menuCategoryRepository.findAll();
                String pictureLink = mainPictureLink == null ? DEFAULT_MAIN_PICTURE_LINK : mainPictureLink; // Изображение в главном меню м.б. установленное/дефолтное
                executePhotoMessage(method.receiveCategoryMenu(chatId, pictureLink, menuCategories));

            } else if (messageText.equals("⚙️" + " Регистрация и настройки")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getUserName().equals("Unregistered")) {
                    executeSendMessage(new SendMessage(stringChatId, "Пожалуйста, начните ваше знакомство с бот-приложением с команды /start"));
                } else {
                    executeSendMessage(method.receiveRegisterAndSettingsMenu(stringChatId, "Вы можете зарегистрироваться и получать сообщения о скидках и акциях", user.get().isAgreeGetMessage()));
                }

            } else if (messageText.contains("Корзина")) {
                if (GROCERY_BASKET.get(chatId) == null) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Ваша корзина пуста");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    executeSendMessage(method.receiveGroceryBasket(String.valueOf(chatId), "Ваша корзина:", GROCERY_BASKET.get(chatId)));
                }

            } else if (messageText.equals("Добавить новый продукт")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    TEMP_DATA.put(chatId, "#ADD_NEW_PRODUCT");
                }

            } else if (messageText.equals("Добавить новую категорию меню")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    TEMP_DATA.put(chatId, "#ADD_NEW_MENU_CATEGORY");
                }

            } else if (messageText.equals("Отправить сообщение пользователям")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    TEMP_DATA.put(chatId, "#WRITE_MESSAGE_FOR_USERS");
                }

            } else if (messageText.equals("Промо и аналитика")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    executeSendMessage(method.receivePromoAnalyticMenu(stringChatId, "Промо и аналитика"));
                }

            } else if (messageText.equals("Удалить")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    executeSendMessage(method.receiveMenuForDelete(stringChatId, "В этом разделе вы можете:"));
                }

            } else if (messageText.equals("Заказы")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    if(ORDER_NUMBER.isEmpty()){
                        executeSendMessage(method.receiveCreatedSendMessage(chatId, "Заказов пока нет"));
                    } else {
                        ORDER_NUMBER.forEach((key, value) -> executeSendMessage(method.receiveOrderMessage(stringChatId, key, value)));
                    }
                }

            } else if (messageText.equals("Увеличить время до выдачи заказа клиенту")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    tempTime = LocalTime.now();
                    executeSendMessage(method.receiveCreatedSendMessage(chatId, "Время от момента заказа до передачи заказа клиенту теперь составляет 10 минут. \nЧерез 5 минут настройки времени вернутся к установленным по умолчанию"));
                }

            } else if (messageText.equals("m")) {
                String random = String.valueOf(Math.random());
                Optional<User> user = userRepository.findById(chatId);
                User newUser = (User) user.get().clone();
                newUser.setLastname(random);
                userRepository.save(newUser);
                SendMessage sendMessage = new SendMessage(stringChatId, "Меню пользователя " + random);
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);

            } else if (messageText.equals("b")) {
                SendMessage sendMessage = new SendMessage(stringChatId, "Меню бариста");
                method.setBaristaKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (TEMP_DATA.get(chatId) == null) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(ADMIN)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setAdminKeyBoard(sendMessage);
                    executeSendMessage(sendMessage);
                }
            }


            // Блок работает, если в HashMap TEMP_DATA были добавлены какие-то инструкции
            if (TEMP_DATA.get(chatId).equals("#ADD_NEW_PRODUCT")) {
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "❗Вводите текст без пробелов. Разделяя части текста символом  +  введите категорию меню для продукта+вид продукта+название продукта+описание+ссылку на изображение+объём+цену продукта. Отсутствующее значение обозначьте * (например, объём сиропа *), а затем отправьте сообщение." +
                        "\nОбразец 1): Лето+кофе+Nescafe+очень вкусный кофе+https://disk.yandex.ru/i/ +250+100\nОбразец 2): Классика+чай+Lipton+листовой чай+https://disk.yandex.ru/i/ +350+200\nОбразец 3): *+сироп+Вишневый сироп+самый вкусный сироп+*+*+100\nОбразец 4): *+добавка+Шоколад+шоколадная добавка+*+*+150\n "));
                TEMP_DATA.put(chatId, "#SET_PRODUCT_IN_DB");

            } else if (TEMP_DATA.get(chatId).equals("#SET_PRODUCT_IN_DB")) {
                TEMP_DATA.remove(chatId);
                SendMessage sendMessage;
                String[] productData = messageText.split("\\+");
                if (method.isProductStringValid(productData)) {
                    setProductInDB(productData);
                    sendMessage = new SendMessage(stringChatId, "Продукт добавлен в ассортимент");
                } else {
                    sendMessage = new SendMessage(stringChatId, "Ошибка ввода данных. Продукт не был добавлен в ассортимент");
                }
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (TEMP_DATA.get(chatId).equals("#ADD_NEW_MENU_CATEGORY")) {
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "❗Ведите разделяя символом  +  категорию меню и ссылку на изображение, а затем отправьте сообщение.\nОбразец 1): Лето+https://disk.yandex.ru/i/\nОбразец 2): Классика+https://disk.yandex.ru/i/"));
                TEMP_DATA.put(chatId, "#SET_MENU_CATEGORY_IN_DB");

            } else if (TEMP_DATA.get(chatId).equals("#SET_MENU_CATEGORY_IN_DB")) {
                TEMP_DATA.remove(chatId);
                SendMessage sendMessage;
                String[] categoryData = messageText.split("\\+");
                if (categoryData.length == 2 && categoryData[1].contains("http")) {
                    setCategoryInDB(messageText);
                    sendMessage = new SendMessage(stringChatId, "Категория добавлена");
                } else {
                    sendMessage = new SendMessage(stringChatId, "Ошибка ввода данных. Категория не была добавлена в ассортимент");
                }
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (TEMP_DATA.get(chatId).equals("#WRITE_MESSAGE_FOR_USERS")) {
                SendMessage sendMessage = new SendMessage(stringChatId, "❗Внимание! Введённое сообщение будет отправлено всем пользователям! \nВведите текст сообщения, а затем отправьте его. Сообщение длинной менее чем 3 символа не будет доставлено пользователям");
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);
                TEMP_DATA.put(chatId, "#SEND_MESSAGE_TO_USERS");

            } else if (TEMP_DATA.get(chatId).equals("#SEND_MESSAGE_TO_USERS")) {
                TEMP_DATA.remove(chatId);
                SendMessage sendMessage = new SendMessage();
                if (messageText.length() > 3) {
                    Iterable<User> users = userRepository.findAll();
                    for (User user : users) {
                        if (user.isAgreeGetMessage()) {
                            sendMessage.setText(messageText);
                            sendMessage.setChatId(user.getChatId());
                            executeSendMessage(sendMessage);
                        }
                    }
                    sendMessage.setText("Сообщение отправлено всем пользователям");
                } else {
                    sendMessage.setText("Отправка сообщения отменена");
                }
                sendMessage.setChatId(chatId);
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (TEMP_DATA.get(chatId).equals("#REGISTER")) {
                if (messageText.length() < 2 || messageText.length() > 15) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Имя не может содержать меньше 2 и больше 15 символов");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                    TEMP_DATA.remove(chatId);
                } else {
                    USER_FIRST_NAME.put(chatId, messageText);
                    executeSendMessage(method.receiveCreatedSendMessage(chatId, "Введите дату вашего рождения в формате 2001-01-05 (год-месяц-день)"));
                    TEMP_DATA.put(chatId, "#SET_USER_BIRTHDAY");
                }
            } else if (TEMP_DATA.get(chatId).equals("#SET_USER_BIRTHDAY")) {
                LocalDate localDate = null;
                try {
                    localDate = LocalDate.parse(messageText);
                } catch (DateTimeParseException e) {
                    log.error("Ошибка ввода данных в процессе регистрации пользователя (Register process exc): " + e.getMessage());
                }
                if (localDate == null || localDate.isAfter(LocalDate.now())) {
                    TEMP_DATA.remove(chatId);
                    SendMessage sendMessage = new SendMessage(stringChatId, "Неправильный ввод даты");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                    USER_FIRST_NAME.remove(chatId, messageText);
                } else {
                    USER_BIRTHDAY.put(chatId, messageText);
                    Optional<User> user = userRepository.findById(chatId);
                    User newUser = (User) user.get().clone();
                    newUser.setFirstname(USER_FIRST_NAME.get(chatId));
                    newUser.setBirthday(USER_BIRTHDAY.get(chatId));
                    userRepository.save(newUser);

                    if (USER_FIRST_NAME.get(chatId).equals(ADMIN)) {
                        TEMP_DATA.remove(chatId);
                        SendMessage sendMessage = new SendMessage(stringChatId, "Ваша учётная запись зарегистрирована в качестве учётной записи администратора");
                        method.setAdminKeyBoard(sendMessage);
                        executeSendMessage(sendMessage);
                    } else if (USER_FIRST_NAME.get(chatId).equals("barista")) {
                        SendMessage sendMessage = new SendMessage(stringChatId, "Ваша учётная запись зарегистрирована в качестве учётной записи бариста");
                        method.setBaristaKeyBoard(sendMessage);
                        executeSendMessage(sendMessage);
                    } else {
                        SendMessage sendMessage = new SendMessage(stringChatId, "Спасибо за регистрацию!");
                        method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                        executeSendMessage(sendMessage);
                    }
                    USER_FIRST_NAME.remove(chatId, messageText);
                    USER_BIRTHDAY.remove(chatId);
                }

            } else if (TEMP_DATA.get(chatId).equals("#CHANGE_MAIN_PICTURE")) {
                TEMP_DATA.remove(chatId);
                SendMessage sendMessage;
                if (messageText.contains("http")) {
                    mainPictureLink = messageText;
                    sendMessage = new SendMessage(stringChatId, "Новое изображение главного меню установлено");
                } else {
                    sendMessage = new SendMessage(stringChatId, "Ошибка ввода");
                }
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);
            }

            // Если update содержит изменённое сообщение
        } else if (update.hasCallbackQuery()) {
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String stringChatId = String.valueOf(chatId);
            String callbackData = update.getCallbackQuery().getData();

            if (callbackData.contains("#product")) { // выбранный из ассортимента продукт
                String productId = callbackData.replace("#product", "");
                Optional<Product> product = productRepository.findById(Integer.parseInt(productId));
                executeEditMessageMedia(method.forSaleProduct(chatId, messageId, product));  // TODO метод № 1 - без удаления предыдущего сообщения
                // executeDeleteMessage(method.deleteMessage(chatId, messageId)); // TODO  метод № 2 - удаление предыдущего сообщения
                // executePhotoMessage(method.forSaleProduct(chatId, product)); // TODO  метод № 2 - удаление предыдущего сообщения  и отправка нового с описанием товара

            } else if (callbackData.contains("#menucategory")) {  // выбранная категория меню
                int chooseMenuCategory = Integer.parseInt(callbackData.replace("#menucategory", ""));
                Optional<MenuCategory> menuCategory = menuCategoryRepository.findById(chooseMenuCategory); // категории меню
                List<Product> productList = (List<Product>) productRepository.findAll(); // список продуктов // TODO сделать без приведения к List

                // TODO метод № 1 - на экран выводится сразу весь ассортимент товаров всех объёмов, согласно выбранной категории меню
                // productList = productList.stream().filter(product -> menuCategory.get().getCategory().equalsIgnoreCase("весь ассортимент") ? !(product.getProductCategory().equalsIgnoreCase("добавка") || product.getProductCategory().equalsIgnoreCase("сироп")) : product.getMenuCategory().equalsIgnoreCase(menuCategory.get().getCategory())).sorted().toList();
                // executeEditMessageMedia(method.receiveProductAssortment(chatId, messageId, menuCategory.get().getCategory(), productList));

                // TODO  метод № 2 - на экран выводится ассортимент из уникальных товаров одного объёма согласно выбранной котегории меню
                productList = productList.stream().filter(product -> menuCategory.get().getCategory().equalsIgnoreCase("весь ассортимент") ? !(product.getProductCategory().equalsIgnoreCase("добавка") || product.getProductCategory().equalsIgnoreCase("сироп")) : product.getMenuCategory().equalsIgnoreCase(menuCategory.get().getCategory())).sorted().distinct().toList();
                executeEditMessageMedia(method.receiveProductAssortmentDistinct(chatId, messageId, menuCategory.get().getPictureLinc(), productList));

            } else if (callbackData.contains("#changepic")) {
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Добавьте ссылку на изображение, а затем отправьте сообщение."));
                TEMP_DATA.put(chatId, "#CHANGE_MAIN_PICTURE");

            } else if (callbackData.contains("#cancelproduct")) {
                executeDeleteMessage(method.deleteMessage(chatId, messageId));

            } else if (callbackData.contains("#setdiscont")) {
                executeEditMessageText(method.receiveDiscountMenu(chatId, messageId, "В этом разделе меню можно установить скидки и акции"));

            } else if (callbackData.contains("#ordernum")) {
                int orderNumber = Integer.parseInt(callbackData.replace("#ordernum", ""));
                ORDER_NUMBER.put(orderNumber, " # заказ передан покупателю"); // TODO сделать не хардкодом
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Заказ отмечен как выполненный"));

            } else if (callbackData.contains("#prodtitle")) {
                String productTitle = callbackData.replace("#prodtitle", "");
                List<Product> productList = productRepository.findByProductTitleIgnoreCase(productTitle);
                executeEditMessageMedia(method.receiveProductSameTitle(chatId, messageId, productList.get(0).getProductPhotoLinc(), productList));

            } else if (callbackData.contains("#userstatistic")) {
                List<User> users = (ArrayList<User>) userRepository.findAll();
                executeEditMessageText(method.receiveStatistic(chatId, messageId, users));

            } else if (callbackData.contains("#addproduct")) {
                Optional<Product> product = productRepository.findById(Integer.parseInt(callbackData.replace("#addproduct", "")));
                GROCERY_BASKET.computeIfAbsent(chatId, k -> new ArrayList<>());
                List<Optional<Product>> productStream = GROCERY_BASKET.get(chatId).stream().filter(prd -> !prd.get().getProductCategory().equalsIgnoreCase("добавка")).
                        filter(prd -> !prd.get().getProductCategory().equalsIgnoreCase("сироп")).toList();

                // Ограничение позиций в заказе = 3 кофе  TODO сделать не хардкодом
                if (productStream.size() >= 3 && !product.get().getProductCategory().equalsIgnoreCase("добавка") && !product.get().getProductCategory().equalsIgnoreCase("сироп")) {
                    executeDeleteMessage(method.deleteMessage(chatId, messageId));
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Корзина заполнена максимальным количеством позиций");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    GROCERY_BASKET.get(chatId).add(product);
                    // цена сиропа для категории напитков "кофе" == цене прайса
                    if (product.get().getProductCategory().equalsIgnoreCase("кофе")) {
                        ArrayList<Product> syrups = (ArrayList<Product>) productRepository.findByProductCategory("сироп");
                        executeEditMessageMedia(method.receiveSyrupMenu(chatId, messageId, SYRUP_PICTURE_LINK, product.get().getProductCategory(), syrups));
                        // цена сиропа для категории напитков "раф" == 0
                    } else if (product.get().getProductCategory().equalsIgnoreCase("раф")) {
                        ArrayList<Product> syrups = (ArrayList<Product>) productRepository.findByProductCategory("сироп");
                        syrups.forEach(s -> s.setProductPrice("0"));
                        executeEditMessageMedia(method.receiveSyrupMenu(chatId, messageId, SYRUP_PICTURE_LINK, product.get().getProductCategory(), syrups));
                    } else {// остальные напитки будут добавлены в корзину без добавок
                        executeDeleteMessage(method.deleteMessage(chatId, messageId));
                        SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ добавлен в корзину");
                        method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                        executeSendMessage(sendMessage);
                    }
                }

            } else if (callbackData.contains("#addsyrup")) {
                Optional<Product> syrup = productRepository.findById(Integer.parseInt(callbackData.replace("#addsyrup", "")));
                // если последний товар в списке == "раф", syrupPrice == 0 (цена сиропа для категории напитков "раф" == 0)
                String syrupPrice = GROCERY_BASKET.get(chatId).get(GROCERY_BASKET.get(chatId).size() - 1).get().getProductCategory().equals("раф") ? "0" : syrup.get().getProductPrice();
                syrup.get().setProductPrice(syrupPrice);
                GROCERY_BASKET.get(chatId).add(syrup);
                ArrayList<Product> supplements = (ArrayList<Product>) productRepository.findByProductCategory("добавка");
                executeEditMessageMedia(method.receiveSupplementMenu(chatId, messageId, SUPPLEMENT_MAIN_PICTURE_LINK, supplements));

            } else if (callbackData.contains("#nosyrup")) {
                ArrayList<Product> supplements = (ArrayList<Product>) productRepository.findByProductCategory("добавка");
                executeEditMessageMedia(method.receiveSupplementMenu(chatId, messageId, SUPPLEMENT_MAIN_PICTURE_LINK, supplements));

            } else if (callbackData.contains("#addsup")) {
                Optional<Product> supplement = productRepository.findById(Integer.parseInt(callbackData.replace("#addsup", "")));
                GROCERY_BASKET.get(chatId).add(supplement);
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ находится в корзине");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);

            } else if (callbackData.contains("#nosup")) {
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ находится в корзине");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);

            } else if (callbackData.contains("#settime")) {
                if (GROCERY_BASKET.get(chatId) == null) {
                    executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Ваша корзина пуста"));
                } else {
                    System.out.println(tempTime); // TODO
                    int time = Integer.parseInt(callbackData.replace("#settime", ""));
                    int changeTime = tempTime != null && tempTime.plus(5,MINUTES).isAfter(LocalTime.now()) ? time + 1 : time; // TODO сделать не хардкодом
                    executeEditMessageText(method.setTime(chatId, messageId, "С помощью клавиш ◄◄ -5  и +5 ►► вы можете выбрать время, к которому ваш заказ будет готов:", changeTime));
                }

            } else if (callbackData.contains("#delorder")) {
                GROCERY_BASKET.remove(chatId);
                SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Заказ был удалён из корзины");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, ""));

            } else if (callbackData.contains("#makeorder")) {
                String time = callbackData.replace("#makeorder", ""); // выбранное время в формате 00:00 - ч:м
                List<LabeledPrice> labeledPriceList = new ArrayList<>();
                GROCERY_BASKET.get(chatId).stream().forEach(product -> labeledPriceList.add(new LabeledPrice(product.get().getProductTitle(), Integer.parseInt(product.get().getProductPrice()) * 100)));
                String payLinc = receiveExecutedInvoiceLinc(method.payOrder(chatId, messageId, "https://disk.yandex.ru/i/K331xGIlON2LxA", "381764678:TEST:62053", labeledPriceList)); //"401643678:TEST:95e3e338-2311-4825-bd6f-0de31a0b5ce8"
                executeEditMessageText(method.approveOrder(chatId, messageId, "После оплаты, ваш заказ будет готов к " + time, payLinc));

            } else if (callbackData.contains("#payorder")) {
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                // Далее запрос передаётся блоку  else if(update.hasPreCheckoutQuery())

            } else if (callbackData.contains("#agree")) {
                Optional<User> user = userRepository.findById(chatId);
                User newUser = (User) user.get().clone();
                newUser.setAgreeGetMessage(true);
                userRepository.save(newUser);
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Теперь вы сможете получать сообщения бота"));

            } else if (callbackData.contains("#notagree")) {
                Optional<User> user = userRepository.findById(chatId);
                User newUser = (User) user.get().clone();
                newUser.setAgreeGetMessage(false);
                userRepository.save(newUser);
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Теперь вы не сможете получать сообщения бота"));

            } else if (callbackData.contains("#register")) {
                Optional<User> us = userRepository.findById(chatId);
                if (us.get().getFirstname() == null) {
                    TEMP_DATA.put(chatId, "#REGISTER");
                    executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Введите, пожалуйста, ваше имя"));
                } else {
                    executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Вы являетесь зарегистрированным пользователем"));
                }

            } else if (callbackData.contains("#delproduct")) {
                List<Product> productList = (ArrayList<Product>) productRepository.findAll();
                executeEditMessageText(method.receiveProductForDelete(chatId, messageId, "Выберите продукт для удаления", productList));

            } else if (callbackData.contains("#removeproduct")) {
                int productId = Integer.parseInt(callbackData.replace("#removeproduct", ""));
                productRepository.deleteById(productId);
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Продукт удалён из базы"));

            } else if (callbackData.contains("#delcategory")) {
                List<MenuCategory> categoryList = (ArrayList<MenuCategory>) menuCategoryRepository.findAll();
                executeEditMessageText(method.receiveCategoryForDelete(chatId, messageId, "Выберите категорию для удаления", categoryList));

            } else if (callbackData.contains("#removecategory")) {
                String category = callbackData.replace("#removecategory", "");
                menuCategoryRepository.deleteById(menuCategoryRepository.findByCategoryLikeIgnoreCase(category).get().getCategoryId()); // TODO реализовать удаление по-человечески
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Категория удалена"));
            }

            //  Pre Checkout Query отвечает за обработку и утверждение платежа перед тем, как пользователь его совершит. Так можно проверить доступность товара на складе или уточнить стоимость.
        } else if (update.hasPreCheckoutQuery()) {
            PreCheckoutQuery preCheckoutQuery = update.getPreCheckoutQuery();
            String preCheckoutQueryId = preCheckoutQuery.getId();

            AnswerPreCheckoutQuery answerPreCheckoutQuery = new AnswerPreCheckoutQuery();
            answerPreCheckoutQuery.setPreCheckoutQueryId(preCheckoutQueryId);
            answerPreCheckoutQuery.setOk(true);
            answerPreCheckoutQuery.setErrorMessage("Причина отмены");
            try {
                execute(answerPreCheckoutQuery);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            String[] chatIdAndMessageId = preCheckoutQuery.getInvoicePayload().split("-");
            long chatId = Long.parseLong(chatIdAndMessageId[0]);
            int messageId = Integer.parseInt(chatIdAndMessageId[1]);

            // Создаются 2 строки: строка для кассового чека в формате: продукт-450-300#  и  строка покупок в формате: продукт-450ml-300 для записи в б.д.
            StringBuilder cashReceiptBuilder = new StringBuilder();
            StringBuilder purchaseBuilder = new StringBuilder();
            StringBuilder orderBuilder = new StringBuilder();
            int fullPrice = 0;
            for (Optional<Product> product : GROCERY_BASKET.get(chatId)) {
                purchaseBuilder.append(product.get().getProductTitle()).append("-").append(product.get().getProductSize()).append("ml-").append(product.get().getProductPrice()).append("-"); // для бд, формат: продукт-450ml-300
                orderBuilder.append(product.get().getProductTitle()).append("  ").append(product.get().getProductSize()).append("ml").append("#");
                cashReceiptBuilder.append(product.get().getProductTitle()).append("-").append(product.get().getProductPrice()).append("#"); // строка для кассового чека в формате: продукт-450-300#
                fullPrice += Integer.parseInt(product.get().getProductPrice());
            }

            // Создание кассового чека
            CashReceipt cashReceipt = new CashReceipt(cashReceiptBuilder.toString(), "00123", 6, 3, "приход", "www.pnh.ru", "Спасибо за покупку!");
            CashReceiptOutput cashReceiptOutput = new CashReceiptOutput();
            cashReceiptOutput.createApachePDF(chatId, "C:\\", cashReceipt.getCashReceiptText());
            String cashReceiptDirectory = cashReceiptOutput.getDirectoryPath();
            executeSendDocument(method.sendDocument(chatId, cashReceiptDirectory));

            // Перезапись истории покупок пользователя в б.д.
            Optional<User> user = userRepository.findById(chatId);
            User newUser = user.isPresent() ? (User) user.get().clone() : new User(chatId, "Unregistered", true, null, null, null, null, new Timestamp(System.currentTimeMillis()), null);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy  HH:mm");
            String purchase = dateTimeFormatter.format(LocalDateTime.now()) + purchaseBuilder + fullPrice + "purchase";
            String purchaseForSave = newUser.getPurchase() == null ? purchase : newUser.getPurchase() + purchase;
            newUser.setPurchase(purchaseForSave);
            userRepository.save(newUser);
            GROCERY_BASKET.remove(chatId);

            int orderNumber = method.createOrderNumber(ORDER_NUMBER);
            ORDER_NUMBER.put(orderNumber, orderBuilder.toString());
            executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "del"));
            executeDeleteMessage(method.deleteMessage(chatId, messageId));
            SendMessage sendMessage = new SendMessage(chatIdAndMessageId[0], "Номер вашего заказа " + orderNumber + "\nСпасибо за заказ!");
            method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
            executeSendMessage(sendMessage);

            ORDER_NUMBER.forEach((key, value) -> executeSendMessage(method.receiveOrderMessage(String.valueOf(chatId), key, value))); // TODO отправка only for бариста
        }
    }


    @Override
    public String getBotUsername() {
        return "";
    }


    public void executeEditMessageMedia(EditMessageMedia editMessageMedia) {
        try {
            execute(editMessageMedia);
        } catch (TelegramApiException e) {
            log.error("EditMessageMedia execute exc: " + e.getMessage());
        }
    }


    public void executeSendMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("SendMessage execute exc: " + e.getMessage());
        }
    }


    private void executePhotoMessage(SendPhoto sendPhoto) {
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("PhotoMessage execute exc: " + e.getMessage());
        }
    }

    private void executeEditMessageText(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error("EditMessageText execute exc: " + e.getMessage());
        }
    }


    private void executeDeleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("DeleteMessage execute exc: " + e.getMessage());
        }
    }


    private void executeAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            log.error("AnswerCallbackQuery execute exc: " + e.getMessage());
        }
    }


    private void executeSendDocument(SendDocument sendDocument) {
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error("SendDocument execute exc: " + e.getMessage());
        }
    }


    private void setUserInDB(long chatId, String userName, boolean isAgreeGetMessage, String firstname, String lastname, String patronymic, String birthday, Timestamp registeredDate, String purchase) {
        User user = new User();
        user.setChatId(chatId);
        user.setUserName(userName);
        user.setAgreeGetMessage(isAgreeGetMessage);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setPatronymic(patronymic);
        user.setBirthday(birthday);
        user.setRegisteredDate(registeredDate);
        user.setPurchase(purchase);
        userRepository.save(user);
    }


    private void setProductInDB(String[] productData) { // String productTitle, String productDescription, String productPhotoLinc, long price
        Product product = new Product();
        product.setMenuCategory(productData[0].toLowerCase());
        product.setProductCategory(productData[1].toLowerCase());
        product.setProductTitle(productData[2]);
        product.setProductDescription(productData[3]);
        product.setProductPhotoLinc(productData[4]);
        product.setProductSize(productData[5]);
        product.setProductPrice(productData[6]);
        productRepository.save(product);
    }


    private void setCategoryInDB(String pictureData) {
        String[] splitData = pictureData.split("_");
        MenuCategory categoryMenu = new MenuCategory();
        categoryMenu.setCategory(splitData[0]);
        categoryMenu.setPictureLinc(splitData[1]);
        menuCategoryRepository.save(categoryMenu);
    }


    private String receiveExecutedInvoiceLinc(CreateInvoiceLink createInvoiceLink) {
        String invoiceLincUrl = "null";
        try {
            invoiceLincUrl = execute(createInvoiceLink);
        } catch (TelegramApiException e) {
            log.error("InvoiceLinc execute exc: " + e.getMessage());
        }
        return invoiceLincUrl;
    }


}

// 1111 1111 1111 1026