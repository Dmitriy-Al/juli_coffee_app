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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

@Slf4j
@Component
public class TelegramBotCommand extends TelegramLongPollingBot {

    private final List<Integer> ORDER_NUMBER = new ArrayList<>();
    private final HashMap<Long, String> TEMP_DATA = new HashMap<>();
    private final HashMap<Long, String> USER_BIRTHDAY = new HashMap<>();
    private final HashMap<Long, String> GROCERY_BASKET = new HashMap<>(); // <chatId , productId + "-" + productSize + "-" + productPrice + "#">
    private final HashMap<Long, List<Optional<Product>>> GROCERY = new HashMap<>(); // <chatId , productId + "-" + productSize + "-" + productPrice + "#">
    private final HashMap<Long, String> USER_FIRST_NAME = new HashMap<>();
    private final TelegramBotMethods method = new TelegramBotMethods();
    private final String DEFAULT_MAIN_PICTURE_LINK = "https://disk.yandex.ru/i/1QoAtJVbb3U8TA";
    private final String SYRUP_PICTURE_LINK = "https://taketea.ru/wp-content/uploads/4/d/9/4d912867ba0872a09f4da7bb117cd50d.jpeg";
    private final String SUPPLEMENT_MAIN_PICTURE_LINK = "https://midwestcouponclippers.net/wp-content/uploads/2012/09/Baileys-COFFEE-CREAMERS.jpg";
    private String mainPictureLink = null;
    private final String admin = "admin";
    private final String barista = "barista";


    @Autowired
    public UserRepository userRepository;
    @Autowired
    public ProductRepository productRepository;
    @Autowired
    public MenuCategoryRepository menuCategoryRepository;


    public TelegramBotCommand() {
        super("5684975537:AAHNI1ulaYG9U0ifSlOet3r6DClVoPWlgUk");
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

                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(admin)) {
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
                    String[] productData = GROCERY_BASKET.get(chatId).split("#"); // productData[0]: 103-300-150  productId, объём, цена
                    for (int i = 0; i < productData.length; i++) {
                        String[] splitData = productData[i].split("-");
                        int productId = Integer.parseInt(splitData[0]);
                        Optional<Product> product = productRepository.findById(productId);
                        String temp = productData[i];
                        productData[i] = product.get().getProductCategory() + "  " + product.get().getProductTitle() + "-" + temp;
                    }
                    executeSendMessage(method.receiveGroceryBasket(String.valueOf(chatId), "Ваша корзина:", productData));
                }

            } else if (messageText.equals("Добавить новый продукт")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(admin)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    TEMP_DATA.put(chatId, "#ADD_NEW_PRODUCT");
                }

            } else if (messageText.equals("Добавить новую категорию меню")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(admin)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    TEMP_DATA.put(chatId, "#ADD_NEW_MENU_CATEGORY");
                }

            } else if (messageText.equals("Отправить сообщение пользователям")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(admin)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    TEMP_DATA.put(chatId, "#WRITE_MESSAGE_FOR_USERS");
                }

            } else if (messageText.equals("Промо и аналитика")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(admin)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    executeSendMessage(method.receivePromoAnalyticMenu(stringChatId, "Промо и аналитика"));
                }

            } else if (messageText.equals("Удалить")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(admin)) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    executeSendMessage(method.receiveMenuForDelete(stringChatId, "В этом разделе вы можете:"));
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

            } else if (TEMP_DATA.get(chatId) == null) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname() == null || !user.get().getFirstname().equals(admin)) {
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
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "❗Вводите текст без пробелов. Разделяя части текста символом  _  введите категорию меню для продукта_вид продукта_название продукта_описание_ссылку на изображение_объём_цену продукта. Отсутствующее значение обозначьте * (например, объём сиропа *), а затем отправьте сообщение." +
                        "\nОбразец 1): Лето_кофе_Nescafe_очень вкусный кофе_https://disk.yandex.ru/i/ _250_100\nОбразец 2): Классика_чай_Lipton_листовой чай_https://disk.yandex.ru/i/ _350_200\nОбразец 3): *_сироп_Вишневый сироп_самый вкусный сироп_*_*_100\nОбразец 4): *_добавка_Шоколад_шоколадная добавка_*_*_150\n "));
                TEMP_DATA.put(chatId, "#SET_PRODUCT_IN_DB");

            } else if (TEMP_DATA.get(chatId).equals("#SET_PRODUCT_IN_DB")) {
                TEMP_DATA.remove(chatId);
                SendMessage sendMessage;
                String[] productData = messageText.split("_");
                if (method.isProductStringValid(productData)) {
                    setProductInDB(productData);
                    sendMessage = new SendMessage(stringChatId, "Продукт добавлен в ассортимент");
                } else {
                    sendMessage = new SendMessage(stringChatId, "Ошибка ввода данных. Продукт не был добавлен в ассортимент");
                }
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (TEMP_DATA.get(chatId).equals("#ADD_NEW_MENU_CATEGORY")) {
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "❗Ведите разделяя символом  _  категорию меню и ссылку на изображение, а затем отправьте сообщение.\nОбразец 1): Лето_https://disk.yandex.ru/i/\nОбразец 2): Классика_https://disk.yandex.ru/i/"));
                TEMP_DATA.put(chatId, "#SET_MENU_CATEGORY_IN_DB");

            } else if (TEMP_DATA.get(chatId).equals("#SET_MENU_CATEGORY_IN_DB")) {
                TEMP_DATA.remove(chatId);
                SendMessage sendMessage;
                String[] categoryData = messageText.split("_");
                if (categoryData.length == 2 && categoryData[1].contains("http")) {
                    setCategoryInDB(messageText);
                    sendMessage = new SendMessage(stringChatId, "Категория добавлена");
                } else {
                    sendMessage = new SendMessage(stringChatId, "Ошибка ввода данных. Категория не была добавлена в ассортимент");
                }
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (TEMP_DATA.get(chatId).equals("#WRITE_MESSAGE_FOR_USERS")) {
                SendMessage sendMessage = new SendMessage(stringChatId, "❗Внимание! Введённое сообщение будет отправлено всем пользователям! Сообщение длинной менее чем 3 символа не будет доставлено пользователям.\nВведите текст сообщения, а затем отправьте его. Сообщение длинной менее чем 3 символа не будет доставлено пользователям");
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

                    if (USER_FIRST_NAME.get(chatId).equals(admin)) {
                        TEMP_DATA.remove(chatId);
                        SendMessage sendMessage = new SendMessage(stringChatId, "Ваша учётная запись зарегистрирована в качестве учётной записи администратора");
                        method.setAdminKeyBoard(sendMessage);
                        executeSendMessage(sendMessage);

                    } else if (USER_FIRST_NAME.get(chatId).equals("barista")) {

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

            } else if (TEMP_DATA.get(chatId).equals("#")) {

            }


            // Если update содержит изменённое сообщение
        } else if (update.hasCallbackQuery()) {
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String stringChatId = String.valueOf(chatId);
            String callbackData = update.getCallbackQuery().getData();
/*
            if (callbackData.contains("#product")) {
                String productId = callbackData.replace("#product", "");
                Optional<Product> product = productRepository.findById(Integer.parseInt(productId));
                String[] sizeAndPrice = product.get().getSizeAndPrice().split(" ");
                //executeEditMessageMedia(method.forSaleProduct(chatId, messageId, product.get().getProductPhotoLinc(), product.get().getProductInfo(), productId, sizeAndPrice));
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                executePhotoMessage(method.forSaleProduct(chatId, product.get().getProductPhotoLinc(), product.get().getProductInfo(), productId, sizeAndPrice));

 */

            if (callbackData.contains("#product")) {
                String productId = callbackData.replace("#product", "");
                Optional<Product> product = productRepository.findById(Integer.parseInt(productId));
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                //executeEditMessageMedia(method.forSaleProduct(chatId, messageId, product.get().getProductPhotoLinc(), product.get().getProductInfo(), productId, sizeAndPrice));
                executePhotoMessage(method.forSaleProduct(chatId, product));

            } else if (callbackData.contains("#menucategory")) { // "#assortmentraf" "#assortmentsummer" "#assortmentwinter" "#assortmentall" "#assortmentclassic"
                // String chooseMenuCategory = callbackData.replace("#assortment", "");
                //Optional<MenuCategory> menuCategory = menuCategoryRepository.findByCategoryLikeIgnoreCase(chooseMenuCategory);
                int chooseMenuCategory = Integer.parseInt(callbackData.replace("#menucategory", ""));
                Optional<MenuCategory> menuCategory = menuCategoryRepository.findById(chooseMenuCategory); // категории меню
                List<Product> productList = (List<Product>) productRepository.findAll(); // список продуктов
                executeEditMessageMedia(method.receiveProductAssortment(chatId, messageId, menuCategory.get().getCategory(), menuCategory.get().getPictureLinc(), productList));

            } else if (callbackData.contains("#changepic")) {
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Добавьте ссылку на изображение, а затем отправьте сообщение."));
                TEMP_DATA.put(chatId, "#CHANGE_MAIN_PICTURE");

            } else if (callbackData.contains("#setdiscont")) {


            } else if (callbackData.contains("#userstatistic")) {
                ArrayList<User> users = (ArrayList<User>) userRepository.findAll();
                executeEditMessageText(method.receiveStatistic(chatId, messageId, users));

            } else if (callbackData.contains("!")) {

            } else if (callbackData.contains("#addproduct")) {
                Optional<Product> product = productRepository.findById(Integer.parseInt(callbackData.replace("#addproduct", "")));
                GROCERY.computeIfAbsent(chatId, k -> new ArrayList<>());
                List<Optional<Product>> productStream = GROCERY.get(chatId).stream().filter(prd -> !prd.get().getProductCategory().equalsIgnoreCase("добавка")).filter(prd -> !prd.get().getProductCategory().equalsIgnoreCase("сироп")).toList();
                System.out.println(GROCERY.get(chatId).size() + " <> " + productStream.size());

                // Ограничение позиций в заказе = 3 кофе  TODO сделать не хардкодом
                if (productStream.size() >= 3 && !product.get().getProductCategory().equalsIgnoreCase("добавка") && !product.get().getProductCategory().equalsIgnoreCase("сироп")) {
                    executeDeleteMessage(method.deleteMessage(chatId, messageId));
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Корзина заполнена максимальным количеством позиций");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));// TODO сделать GROCERY_BASKE
                    executeSendMessage(sendMessage);
                } else {
                    GROCERY.get(chatId).add(product);
                    if (product.get().getProductCategory().equalsIgnoreCase("кофе")) {
                        ArrayList<Product> syrups = (ArrayList<Product>) productRepository.findByProductCategory("сироп");
                        executeEditMessageMedia(method.receiveSyrupMenu(chatId, messageId, SYRUP_PICTURE_LINK, product.get().getProductCategory(), syrups));
                    } else if (product.get().getProductCategory().equalsIgnoreCase("раф")) {
                        // ArrayList<Product> syrups = (ArrayList<Product>) productRepository.findByProductCategory("сироп").stream().peek(syrup -> syrup.setProductPrice("0"));
                        ArrayList<Product> syrups = (ArrayList<Product>) productRepository.findByProductCategory("сироп");
                        for(Product syr : syrups){
                            syr.setProductPrice("0");
                        }
                        executeEditMessageMedia(method.receiveSyrupMenu(chatId, messageId, SYRUP_PICTURE_LINK, product.get().getProductCategory(), syrups));
                    } else {
                        executeDeleteMessage(method.deleteMessage(chatId, messageId));
                        SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ добавлен в корзину");
                        method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                        executeSendMessage(sendMessage);
                    }
                }


            } else if (callbackData.contains("#cancelproduct")) {
                executeDeleteMessage(method.deleteMessage(chatId, messageId));

            } else if (callbackData.contains("!!!!!!!!!!!!!!!")) {

            /*    if (GROCERY_BASKET.get(chatId) != null && GROCERY_BASKET.get(chatId).length() >= 40) { // Ограничение позиций в заказе (одна позиция ~10 символов) TODO сделать не хардкодом
                    executeDeleteMessage(method.deleteMessage(chatId, messageId));
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Корзина заполнена максимальным количеством позиций");bvjh
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);

                     } else {
                    String productIdAndSizeAndPrice = callbackData.replace("#addproduct", ""); // productIdAndSizeAndPrice - строка с данными: 103-300-150  productId-объём-цена
                    if (GROCERY_BASKET.get(chatId) == null) {
                        GROCERY_BASKET.put(chatId, productIdAndSizeAndPrice + "#");
                    } else {
                        String order = GROCERY_BASKET.get(chatId);
                        GROCERY_BASKET.put(chatId, order + productIdAndSizeAndPrice + "#");
                    }
                    String[] productData = productIdAndSizeAndPrice.split("-"); // id продукта / объём-цена продукта
                    Optional<Product> product = productRepository.findById(Integer.parseInt(productData[0]));

                    if (product.get().getProductCategory().equalsIgnoreCase("кофе") || product.get().getProductCategory().equalsIgnoreCase("раф")) {
                        ArrayList<Product> products = (ArrayList<Product>) productRepository.findAll();
                        List<Product> syrupStream = products.stream().filter(prod -> prod.getProductCategory().equals("сироп")).toList();
                        executeEditMessageMedia(method.receiveSyrupMenu(chatId, messageId, SYRUP_PICTURE_LINK,  product.get().getProductCategory(), syrupStream));
                    } else {
                        executeDeleteMessage(method.deleteMessage(chatId, messageId)); // TODO
                        SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ добавлен в корзину");
                        method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                        executeSendMessage(sendMessage);
                    }
                }
             */
                if (GROCERY.get(chatId).size() > 6) { // Ограничение позиций в заказе  TODO сделать не хардкодом и продумать ограничение
                    executeDeleteMessage(method.deleteMessage(chatId, messageId));
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Корзина заполнена максимальным количеством позиций");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));// TODO сделать GROCERY_BASKE
                    executeSendMessage(sendMessage);
                } else {
                    Optional<Product> product = productRepository.findById(Integer.parseInt(callbackData.replace("#addproduct", "")));
                    GROCERY.get(chatId).add(product);
                    if (product.get().getProductCategory().equalsIgnoreCase("кофе") || product.get().getProductCategory().equalsIgnoreCase("раф")) {
                        ArrayList<Product> products = (ArrayList<Product>) productRepository.findAll();
                        List<Product> syrupStream = products.stream().filter(prod -> prod.getProductCategory().equals("сироп")).toList();
                        executeEditMessageMedia(method.receiveSyrupMenu(chatId, messageId, SYRUP_PICTURE_LINK, product.get().getProductCategory(), syrupStream));

                    } else {
                        executeDeleteMessage(method.deleteMessage(chatId, messageId));
                        SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ добавлен в корзину");
                        method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                        executeSendMessage(sendMessage);
                    }
                }

            } else if (callbackData.contains("#addsyrup")) {
                Optional<Product> syrup = productRepository.findById(Integer.parseInt(callbackData.replace("#addsyrup", "")));
                GROCERY.get(chatId).add(syrup);
                ArrayList<Product> supplements = (ArrayList<Product>) productRepository.findByProductCategory("добавка");
                executeEditMessageMedia(method.receiveSupplementMenu(chatId, messageId, SUPPLEMENT_MAIN_PICTURE_LINK, supplements));

            } else if (callbackData.contains("#nosyrup")) {
                    ArrayList<Product> supplements = (ArrayList<Product>) productRepository.findByProductCategory("добавка");
                    executeEditMessageMedia(method.receiveSupplementMenu(chatId, messageId, SUPPLEMENT_MAIN_PICTURE_LINK, supplements));

            } else if (callbackData.contains("#addsup")) {
                Optional<Product> supplement = productRepository.findById(Integer.parseInt(callbackData.replace("#addsup", "")));
                GROCERY.get(chatId).add(supplement);
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ находится в корзине");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);

            } else if (callbackData.contains("#nosup")) {
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ находится в корзине");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);


          /*   } else if (callbackData.contains("#nosyrup")) {
                ArrayList<Product> products = (ArrayList<Product>) productRepository.findAll();
                List<Product> supplementStream = products.stream().filter(prod -> prod.getProductCategory().equals("добавка")).toList();
                executeEditMessageMedia(method.receiveSupplementMenu(chatId, messageId, SUPPLEMENT_MAIN_PICTURE_LINK, supplementStream));


            } else if (callbackData.contains("#addsupplement")) {
                String productIdAndPrice = callbackData.replace("#addsupplement", ""); // productIdAndPrice - строка с данными: 103-*-150  productId-объём-цена
                String order = GROCERY_BASKET.get(chatId);
                GROCERY_BASKET.put(chatId, order + productIdAndPrice + "#");
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ добавлен в корзину");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);

            } else if (callbackData.contains("#nosupplement")) {
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ находится в корзине");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);*/

            } else if (callbackData.contains("#settime")) {
                if (GROCERY_BASKET.get(chatId) == null) {
                    executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Ваша корзина пуста"));
                } else {
                    long changeTime = Long.parseLong(callbackData.replace("#settime", ""));
                    executeEditMessageText(method.setTime(chatId, messageId, "С помощью клавиш ◄◄ -5  и +5 ►► вы можете выбрать время, к которому ваш заказ будет готов:", changeTime));
                }

            } else if (callbackData.contains("#delorder")) {
                GROCERY_BASKET.remove(chatId);
                SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Заказ был удалены из корзины");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, ""));

            } else if (callbackData.contains("#makeorder")) {
                String time = callbackData.replace("#makeorder", ""); // 00:00

                String[] productData = GROCERY_BASKET.get(chatId).split("#"); // productData[0]: 103-300-150  productId, объём, цена
                List<LabeledPrice> labeledPriceList = new ArrayList<>();
                for (int i = 0; i < productData.length; i++) {
                    String[] splitData = productData[i].split("-");
                    int productId = Integer.parseInt(splitData[0]);
                    int productPrice = Integer.parseInt(splitData[2]);
                    Optional<Product> product = productRepository.findById(productId);
                    String temp = productData[i];
                    productData[i] = product.get().getProductCategory() + "  " + product.get().getProductTitle() + "-" + temp;
                    labeledPriceList.add(new LabeledPrice(product.get().getProductTitle(), productPrice * 100));
                }
                String payLinc = receiveExecutedInvoiceLinc(method.payOrder("https://disk.yandex.ru/i/K331xGIlON2LxA", /*"401643678:TEST:95e3e338-2311-4825-bd6f-0de31a0b5ce8"*/"381764678:TEST:62053", labeledPriceList, chatId, messageId));

                executeEditMessageText(method.approveOrder(chatId, messageId, "После оплаты, ваш заказ будет готов к " + time, payLinc));
            } else if (callbackData.contains("#agree")) {
                Optional<User> us = userRepository.findById(chatId);
                User newUser = new User(chatId, us.get().getUserName(), us.get().isAgreeGetMessage(), us.get().getFirstname(), us.get().getLastname(), us.get().getPatronymic(), us.get().getBirthday(), us.get().getRegisteredDate(), us.get().getPurchase());
                userRepository.deleteById(chatId);
                setUserInDB(chatId, newUser.getUserName(), true, newUser.getFirstname(), newUser.getLastname(), newUser.getPatronymic(), newUser.getBirthday(), newUser.getRegisteredDate(), newUser.getPurchase());
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Теперь вы сможете получать сообщения бота"));

            } else if (callbackData.contains("#notagree")) {
                Optional<User> us = userRepository.findById(chatId);
                User newUser = new User(chatId, us.get().getUserName(), us.get().isAgreeGetMessage(), us.get().getFirstname(), us.get().getLastname(), us.get().getPatronymic(), us.get().getBirthday(), us.get().getRegisteredDate(), us.get().getPurchase());
                userRepository.deleteById(chatId);
                setUserInDB(chatId, newUser.getUserName(), false, newUser.getFirstname(), newUser.getLastname(), newUser.getPatronymic(), newUser.getBirthday(), newUser.getRegisteredDate(), newUser.getPurchase());
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
            String[] groceryBasket = GROCERY_BASKET.get(chatId).split("#");
            StringBuilder cashReceiptBuilder = new StringBuilder();
            StringBuilder purchaseBuilder = new StringBuilder();
            int fullPrice = 0;
            for (String productData : groceryBasket) {
                String[] data = productData.split("-");
                Optional<Product> product = productRepository.findById(Integer.parseInt(data[0]));
                cashReceiptBuilder.append(product.get().getProductTitle()).append("-").append(data[2]).append("#");
                purchaseBuilder.append(product.get().getProductTitle()).append("-").append(data[1]).append("ml-").append(data[2]).append("-");
                fullPrice += Integer.parseInt(data[2]);
            }

            // Создание кассового чека
            CashReceipt cashReceipt = new CashReceipt(cashReceiptBuilder.toString(), "00123", 6, 3, "приход", "www.pnh.ru", "Спасибо за покупку!");
            CashReceiptOutput cashReceiptOutput = new CashReceiptOutput();  // cashReceipt.getCashReceiptText()
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
            ORDER_NUMBER.add(orderNumber);
            executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "del"));
            executeDeleteMessage(method.deleteMessage(chatId, messageId));
            SendMessage sendMessage = new SendMessage(chatIdAndMessageId[0], "Номер вашего заказа " + orderNumber + "\nСпасибо за заказ!");
            method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
            executeSendMessage(sendMessage);
        }

    }


    @Override
    public String getBotUsername() {
        return "TestDemoUnicNameBot";
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