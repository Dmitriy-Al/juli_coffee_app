package julia.cafe.service;
// 639002000000000003   1111 1111 1111 1026

import julia.cafe.cashreceipt.CashReceipt;
import julia.cafe.cashreceipt.CashReceiptOutput;
import julia.cafe.config.Config;
import julia.cafe.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
@Component
public class TelegramBotCommand extends TelegramLongPollingBot {

    private final LinkedHashMap<Integer, String> ORDER_NUMBER = new LinkedHashMap<>();
    private final Map<Long, List<Optional<Product>>> GROCERY_BASKET = new HashMap<>();
    private final Map<Integer, Integer> DISCOUNT = new HashMap<>();
    private final Map<Long, String> TEMP_DATA = new HashMap<>();

    private final TelegramBotMethods method = new TelegramBotMethods();
    private final Config config;

    private final static String DEFAULT_MAIN_PICTURE_LINK = "https://disk.yandex.ru/i/1QoAtJVbb3U8TA";
    private final static String UNREGISTERED_USER = "Unregistered";
    private final static String DEFAULT_NAME = "незарегистрированный пользователь";
    private final static String NO_PURCHASE = "no purchase";

    private String mainPictureLink = null;
    private LocalTime tempTime = null;


    @Autowired
    public UserRepository userRepository;
    @Autowired
    public ProductRepository productRepository;
    @Autowired
    public MenuCategoryRepository menuCategoryRepository;


    public TelegramBotCommand(Config config) {
        super(config.botToken);
        this.config = config;
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
                SendMessage sendMessage = new SendMessage();

                if (user.isEmpty() || user.get().getUserName().equals(UNREGISTERED_USER)) {
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(userName + ApplicationStrings.GREETiNG_FOR_USER_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                } else if (user.get().getFirstname().equals(config.ADMIN)) {
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Здравствуйте, " + userName + "!");
                    method.setAdminKeyBoard(sendMessage);
                } else if (user.get().getFirstname().equals(config.BARISTA)) {
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Здравствуйте, " + user.get().getFirstname() + "!");
                    method.setBaristaKeyBoard(sendMessage);
                } else {
                    String firstName = user.get().getFirstname().equals(DEFAULT_NAME) ? ", " + user.get().getUserName() :
                            ", " + user.get().getFirstname();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Здравствуйте" + firstName + "!");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                }
                executeSendMessage(sendMessage);

                if (user.isEmpty() || user.get().getUserName().equals(UNREGISTERED_USER)) {
                    setUserInDB(new User(chatId, userName, true, new Timestamp(System.currentTimeMillis()), DEFAULT_NAME, NO_PURCHASE));
                }

            } else if (messageText.equals("/mydata")) {
                Optional<User> user = userRepository.findById(chatId);
                String userData = user.isPresent() ? user.get().getUserData() : "Пользователь не зарегистрирован";
                executeSendMessage(new SendMessage(stringChatId, userData));

            } else if (messageText.equals("/deletedata")) {
                userRepository.deleteById(chatId);
                executeSendMessage(new SendMessage(stringChatId, "Пользовательская информация была удалена"));

            } else if (messageText.equals("☕️" + " Кофе и напитки")) { // клавиатура
                List<MenuCategory> menuCategories = (List<MenuCategory>) menuCategoryRepository.findAll();
                String pictureLink = mainPictureLink == null ? DEFAULT_MAIN_PICTURE_LINK : mainPictureLink; // Изображение в главном меню м.б. установленное/дефолтное
                executePhotoMessage(method.receiveCategoryMenu(chatId, pictureLink, menuCategories));

            } else if (messageText.equals("⚙️" + " Регистрация и настройки")) {
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getUserName().equals(UNREGISTERED_USER)) {
                    executeSendMessage(new SendMessage(stringChatId, ApplicationStrings.START_LINK_TEXT));
                } else {
                    executeSendMessage(method.receiveSettingsMenu(stringChatId, user.get().isAgreeGetMessage(), ApplicationStrings.SETTING_TEXT));
                }

            } else if (messageText.contains("Корзина")) {
                if (GROCERY_BASKET.get(chatId) == null) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Ваша корзина пуста");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    Optional<User> user = userRepository.findById(chatId);
                    String firstName = user.map(user2 -> user2.getFirstname().equals(DEFAULT_NAME) ? "" : user2.getFirstname() + ", ").orElse("");
                    executeSendMessage(method.receiveGroceryBasket(String.valueOf(chatId), "\uD83D\uDED2 " + firstName +
                            "ваша корзина:", GROCERY_BASKET.get(chatId), DISCOUNT));
                }

            } else if (messageText.equals("Добавить новый продукт")) {
                Optional<User> user = userRepository.findById(chatId);
                SendMessage sendMessage;
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else if (user.get().getFirstname().equals(config.ADMIN)) {
                    TEMP_DATA.put(chatId, "#ADD_NEW_PRODUCT");
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    executeSendMessage(sendMessage);
                }

            } else if (messageText.equals("Добавить новую категорию меню")) {
                Optional<User> user = userRepository.findById(chatId);
                SendMessage sendMessage;
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, "Такая команда отсутствует");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else if (user.get().getFirstname().equals(config.ADMIN)) {
                    TEMP_DATA.put(chatId, "#ADD_NEW_MENU_CATEGORY");
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    executeSendMessage(sendMessage);
                }

            } else if (messageText.equals("Отправить сообщение пользователям")) {
                Optional<User> user = userRepository.findById(chatId);
                SendMessage sendMessage;
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else if (user.get().getFirstname().equals(config.ADMIN)) {
                    TEMP_DATA.put(chatId, "#WRITE_MESSAGE_FOR_USERS");
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    executeSendMessage(sendMessage);
                }

            } else if (messageText.equals("Промо и аналитика")) {
                Optional<User> user = userRepository.findById(chatId);
                SendMessage sendMessage;
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else if (user.get().getFirstname().equals(config.ADMIN)) {
                    executeSendMessage(method.receivePromoAnalyticMenu(stringChatId, "Промо и аналитика"));
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    executeSendMessage(sendMessage);
                }

            } else if (messageText.equals("Удалить")) {
                Optional<User> user = userRepository.findById(chatId);
                SendMessage sendMessage;
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else if (user.get().getFirstname().equals(config.ADMIN)) {
                    executeSendMessage(method.receiveMenuForDelete(stringChatId, "В этом разделе вы можете:"));
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    executeSendMessage(sendMessage);
                }

            } else if (messageText.equals("Заказы")) {
                Optional<User> user = userRepository.findById(chatId);
                SendMessage sendMessage;
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else if (user.get().getFirstname().equals(config.BARISTA)) {
                    if (ORDER_NUMBER.isEmpty()) {
                        sendMessage = new SendMessage(stringChatId, "Заказов пока нет");
                        executeSendMessage(sendMessage);
                    } else {
                        ORDER_NUMBER.forEach((key, value) -> executeSendMessage(method.receiveOrderMessage(chatId, key, value)));
                    }
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    executeSendMessage(sendMessage);
                }

            } else if (messageText.equals("Увеличить время до выдачи заказа клиенту")) {
                Optional<User> user = userRepository.findById(chatId);
                SendMessage sendMessage;
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else if (user.get().getFirstname().equals(config.BARISTA)) {
                    tempTime = LocalTime.now();
                    executeSendMessage(method.receiveCreatedSendMessage(chatId, ApplicationStrings.ORDER_TIME_TEXT));
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    executeSendMessage(sendMessage);
                }

            } else if (messageText.equals("m")) {
                SendMessage sendMessage = new SendMessage(stringChatId, "Меню пользователя");
                method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                executeSendMessage(sendMessage);

            } else if (messageText.equals("b")) {
                SendMessage sendMessage = new SendMessage(stringChatId, "Меню бариста");
                method.setBaristaKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (TEMP_DATA.get(chatId) == null) {
                SendMessage sendMessage;
                Optional<User> user = userRepository.findById(chatId);
                if (user.isEmpty() || user.get().getFirstname().equals(DEFAULT_NAME)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                } else if (user.get().getFirstname().equals(config.ADMIN)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setAdminKeyBoard(sendMessage);
                } else if (user.get().getFirstname().equals(config.BARISTA)) {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setBaristaKeyBoard(sendMessage);
                } else {
                    sendMessage = new SendMessage(stringChatId, ApplicationStrings.COMMAND_NOT_EXISTS_TEXT);
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                }
                executeSendMessage(sendMessage);
            }


            // Блок работает, если в HashMap TEMP_DATA были добавлены какие-то инструкции
            if (TEMP_DATA.get(chatId).equals("#ADD_NEW_PRODUCT")) {
                executeSendMessage(method.receiveCreatedSendMessage(chatId, ApplicationStrings.NEW_PRODUCT_TEXT));
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
                executeSendMessage(method.receiveCreatedSendMessage(chatId, ApplicationStrings.NEW_CATEGORY_TEXT));
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
                SendMessage sendMessage = new SendMessage(stringChatId, ApplicationStrings.MESSAGE_TEXT);
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
                TEMP_DATA.remove(chatId);
                Optional<User> admin = Optional.ofNullable(userRepository.findByFirstname(config.ADMIN));
                Optional<User> barista = Optional.ofNullable(userRepository.findByFirstname(config.BARISTA));
                if ((messageText.length() < 2 || messageText.length() > 15) || (messageText.equals(config.ADMIN) &&
                        admin.isPresent()) || (messageText.equals(config.BARISTA) && barista.isPresent())) {
                    SendMessage sendMessage = new SendMessage(stringChatId, "Имя не может содержать меньше 2 и больше 15 символов, или заданную последовательность символов");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    Optional<User> user = userRepository.findById(chatId);
                    User newUser = (User) user.get().clone();
                    newUser.setFirstname(messageText);
                    userRepository.save(newUser);

                    if (messageText.equals(config.ADMIN)) {
                        SendMessage sendMessage = new SendMessage(stringChatId, "Ваша учётная запись зарегистрирована в качестве учётной записи администратора");
                        method.setAdminKeyBoard(sendMessage);
                        executeSendMessage(sendMessage);
                    } else if (messageText.equals(config.BARISTA)) {
                        SendMessage sendMessage = new SendMessage(stringChatId, "Ваша учётная запись зарегистрирована в качестве учётной записи бариста");
                        method.setBaristaKeyBoard(sendMessage);
                        executeSendMessage(sendMessage);
                    } else {
                        SendMessage sendMessage = new SendMessage(stringChatId, messageText + ", спасибо за регистрацию!");
                        method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                        executeSendMessage(sendMessage);
                    }
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

            } else if (TEMP_DATA.get(chatId).contains("#PRODUCT_FOR_DISCOUNT")) {
                int productId = Integer.parseInt(TEMP_DATA.get(chatId).replace("#PRODUCT_FOR_DISCOUNT", ""));
                TEMP_DATA.remove(chatId);
                SendMessage sendMessage;
                int discount;
                try {
                    discount = Integer.parseInt(messageText);
                    DISCOUNT.put(productId, discount);
                    sendMessage = new SendMessage(stringChatId, "Добавлена скидка " + discount + "%");
                } catch (NumberFormatException e) {
                    sendMessage = new SendMessage(stringChatId, "Формат ввода может быть только цифрами, скидка не добавлена");
                }
                method.setAdminKeyBoard(sendMessage);
                executeSendMessage(sendMessage);
                System.out.println("DISCOUNT.get " + DISCOUNT.get(productId));
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
                executeEditMessageMedia(method.forSaleProduct(chatId, messageId, product));

            } else if (callbackData.contains("#menucategory")) {  // выбранная категория меню
                int chooseMenuCategory = Integer.parseInt(callbackData.replace("#menucategory", ""));
                Optional<MenuCategory> menuCategory = menuCategoryRepository.findById(chooseMenuCategory); // категории меню
                List<Product> productList = (List<Product>) productRepository.findAll(); // список продуктов

                productList = productList.stream().filter(product -> menuCategory.get().getCategory().equalsIgnoreCase("весь ассортимент") ?
                        !(product.getProductCategory().equalsIgnoreCase("добавка") || product.getProductCategory().equalsIgnoreCase("сироп")) :
                        product.getMenuCategory().equalsIgnoreCase(menuCategory.get().getCategory())).sorted().distinct().toList();
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
                executeEditMessageMedia(method.receiveProductSameTitle(chatId, messageId, productList.get(0).getProductPhotoLinc(), productList, DISCOUNT));

            } else if (callbackData.contains("#userstatistic")) {
                List<User> users = (ArrayList<User>) userRepository.findAll();
                executeEditMessageText(method.receiveStatistic(chatId, messageId, users));

            } else if (callbackData.contains("#addproduct")) {
                Optional<Product> product = productRepository.findById(Integer.parseInt(callbackData.replace("#addproduct", "")));
                GROCERY_BASKET.computeIfAbsent(chatId, k -> new ArrayList<>());
                List<Optional<Product>> productStream = GROCERY_BASKET.get(chatId).stream().filter(prd -> !prd.get().getProductCategory().equalsIgnoreCase("добавка")).
                        filter(prd -> !prd.get().getProductCategory().equalsIgnoreCase("сироп")).toList();

                // Ограничение позиций в заказе = 3 кофе  TODO сделать не хардкодом
                if (productStream.size() >= 3 && !product.get().getProductCategory().equalsIgnoreCase("добавка") &&
                        !product.get().getProductCategory().equalsIgnoreCase("сироп")) {
                    executeDeleteMessage(method.deleteMessage(chatId, messageId));
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Корзина заполнена максимальным количеством позиций");
                    method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
                    executeSendMessage(sendMessage);
                } else {
                    GROCERY_BASKET.get(chatId).add(product);
                    // цена сиропа для категории напитков "кофе" == цене прайса
                    if (product.get().getProductCategory().equalsIgnoreCase("кофе")) {
                        ArrayList<Product> syrups = (ArrayList<Product>) productRepository.findByProductCategory("сироп");
                        executeDeleteMessage(method.deleteMessage(chatId, messageId));
                        executeSendMessage(method.receiveSyrupMenu(chatId, "Меню с сиропами", syrups)); // меню с сиропами
                        // цена сиропа для категории напитков "раф" == 0
                    } else if (product.get().getProductCategory().equalsIgnoreCase("раф")) {
                        ArrayList<Product> syrups = (ArrayList<Product>) productRepository.findByProductCategory("сироп");
                        syrups.forEach(s -> s.setProductPrice("0"));
                        executeDeleteMessage(method.deleteMessage(chatId, messageId));
                        executeSendMessage(method.receiveSyrupMenu(chatId, "Меню с сиропами", syrups)); // меню с сиропами
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
                String syrupPrice = GROCERY_BASKET.get(chatId).get(GROCERY_BASKET.get(chatId).size() - 1).get().getProductCategory().equals("раф") ? "0" :
                        syrup.get().getProductPrice();
                syrup.get().setProductPrice(syrupPrice);
                GROCERY_BASKET.get(chatId).add(syrup);
                ArrayList<Product> supplements = (ArrayList<Product>) productRepository.findByProductCategory("добавка");
                executeEditMessageText(method.receiveSupplementMenu(chatId, messageId, "Меню с добавками", supplements)); // меню с добавками

            } else if (callbackData.contains("#nosyrup")) {
                ArrayList<Product> supplements = (ArrayList<Product>) productRepository.findByProductCategory("добавка");
                executeEditMessageText(method.receiveSupplementMenu(chatId, messageId, "Меню с добавками", supplements)); // меню с добавками

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
                    int time = Integer.parseInt(callbackData.replace("#settime", ""));
                    int changeTime = tempTime != null && tempTime.plus(5, MINUTES).isAfter(LocalTime.now()) ? time + 1 : time; // TODO сделать не хардкодом
                    executeEditMessageText(method.setTime(chatId, messageId, ApplicationStrings.CHANGE_TIME_TEXT, changeTime));
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
                GROCERY_BASKET.get(chatId).forEach(product -> labeledPriceList.add(new LabeledPrice(product.get().getProductTitle(),
                        method.receiveDiscountPrice(DISCOUNT, product.get().getProductId(), Integer.parseInt(product.get().getProductPrice())) * 100)));
                String payLinc = receiveExecutedInvoiceLinc(method.payOrder(chatId, messageId, "https://disk.yandex.ru/i/K331xGIlON2LxA", "381764678:TEST:62053", labeledPriceList));
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
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Теперь вы не будете получать сообщения бота"));

            } else if (callbackData.contains("#register")) {
                Optional<User> user = userRepository.findById(chatId);
                String firstName = user.get().getFirstname();
                if (firstName.equals(DEFAULT_NAME)) {
                    TEMP_DATA.put(chatId, "#REGISTER");
                    executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Введите, пожалуйста, ваше имя"));
                } else {
                    executeEditMessageText(method.receiveEditMessageText(chatId, messageId, firstName + ", вы являетесь зарегистрированным пользователем"));
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

            } else if (callbackData.contains("#allproducts")) {
                List<Product> productList = (List<Product>) productRepository.findAll(); // список продуктов
                executeEditMessageText(method.receiveAllProductMenu(chatId, messageId, "Выберите продукт для добавления или удаления скидки", productList, DISCOUNT));

            } else if (callbackData.contains("#observediscount")) {
                StringBuilder messageText = new StringBuilder();
                messageText.append("Список продуктов со скидкой:");
                for (Map.Entry<Integer, Integer> discountMap : DISCOUNT.entrySet()) {
                    Optional<Product> product = productRepository.findById(discountMap.getKey());
                    messageText.append("\n").append(product.get().getProductTitle()).append(" ").append(product.get()
                            .getProductSize()).append(" ml  -  скидка ").append(discountMap.getValue()).append("%");
                }
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, messageText.toString()));

            } else if (callbackData.contains("#prodfordiscount")) {
                int productId = Integer.parseInt(callbackData.replace("#prodfordiscount", ""));
                Optional<Product> product = productRepository.findById(productId);
                int price = Integer.parseInt(product.get().getProductPrice());
                int discountPrice = method.receiveDiscountPrice(DISCOUNT, productId, price);
                String text = product.get().getProductTitle() + "\nОбъем " + product.get().getProductSize() + "\nЦена без скидки " +
                        price + " ₽" + "\nЦена со скидкой " + discountPrice + " ₽" + "\nТекущая скидка " + DISCOUNT.get(productId) + "%";
                executeEditMessageText(method.receiveProductForDiscount(chatId, messageId, text, productId));

            } else if (callbackData.contains("#setdiscount")) {
                int productId = Integer.parseInt(callbackData.replace("#setdiscount", ""));
                TEMP_DATA.put(chatId, "#PRODUCT_FOR_DISCOUNT" + productId);
                Optional<Product> product = productRepository.findById(productId);
                String text = "В поле ввода введите размер скидки для " + product.get().getProductTitle() +
                        ", затем отправьте сообщение и скидка для продукта будет добавлена. Формат ввода - только цифры";
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, text));

            } else if (callbackData.contains("#deldiscount")) { // Удаление последнего сообщения чата
                int productId = Integer.parseInt(callbackData.replace("#deldiscount", ""));
                DISCOUNT.remove(productId);
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Скидка отменена"));

            } else if (callbackData.contains("#cancelmessage")) { // Удаление последнего сообщения чата
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
            }


            //  Pre Checkout Query отвечает за обработку и утверждение платежа перед тем, как пользователь его совершит
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
                int discountPrice = method.receiveDiscountPrice(DISCOUNT, product.get().getProductId(), Integer.parseInt(product.get().getProductPrice()));
                purchaseBuilder.append(product.get().getProductTitle()).append("-").append(product.get().getProductSize()).append("ml-").append(discountPrice).append("-"); // для бд, формат: продукт-450ml-300
                orderBuilder.append(product.get().getProductTitle()).append("  ").append(product.get().getProductSize()).append("ml").append("#");
                cashReceiptBuilder.append(product.get().getProductTitle()).append("-").append(discountPrice).append("#"); // строка для кассового чека в формате: продукт-450-300#
                fullPrice += discountPrice;
            }

            // Создание кассового чека
            CashReceipt cashReceipt = new CashReceipt(cashReceiptBuilder.toString(), "00123", 6, 3, "приход", "www.pnh.ru", "Спасибо за покупку!");
            CashReceiptOutput cashReceiptOutput = new CashReceiptOutput();
            cashReceiptOutput.createApachePDF(chatId, "C:\\", cashReceipt.getCashReceiptText());
            String cashReceiptDirectory = cashReceiptOutput.getDirectoryPath();
            executeSendDocument(method.sendDocument(chatId, cashReceiptDirectory));

            // Перезапись истории покупок пользователя в б.д.
            Optional<User> user = userRepository.findById(chatId);
            User newUser = user.isPresent() ? (User) user.get().clone() : new User(chatId, UNREGISTERED_USER, true,
                    new Timestamp(System.currentTimeMillis()), DEFAULT_NAME, NO_PURCHASE);

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy  HH:mm");
            String purchase = dateTimeFormatter.format(LocalDateTime.now()) + purchaseBuilder + fullPrice + "purchase";
            String purchaseForSave = newUser.getPurchase().equals(NO_PURCHASE) ? purchase : newUser.getPurchase() + purchase;
            newUser.setPurchase(purchaseForSave);
            setUserInDB(newUser);
            GROCERY_BASKET.remove(chatId);

            String firstName = user.map(user2 -> user2.getFirstname().equals(DEFAULT_NAME) ? "" : user2.getFirstname() + ", ").orElse("");
            int orderNumber = method.createOrderNumber(ORDER_NUMBER);
            ORDER_NUMBER.put(orderNumber, orderBuilder.toString());
            executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "del"));
            executeDeleteMessage(method.deleteMessage(chatId, messageId));
            SendMessage sendMessage = new SendMessage(chatIdAndMessageId[0], firstName + "номер вашего заказа " + orderNumber + "\nСпасибо за заказ!");
            method.setCommonKeyBoard(sendMessage, GROCERY_BASKET.get(chatId));
            executeSendMessage(sendMessage);

            Optional<User> barista = Optional.ofNullable(userRepository.findByFirstname(config.BARISTA));
            barista.ifPresent(user1 -> ORDER_NUMBER.forEach((key, value) -> executeSendMessage(method.receiveOrderMessage(user1.getChatId(), key, value))));
        }
    }


    @Override
    public String getBotUsername() {
        return config.botName;
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


    private void setUserInDB(User user) {
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


    // Удаление данных из Map по расписанию
    @Scheduled(cron = "0 0 0 * * *")
    private void removeDataFromCollections() {
        GROCERY_BASKET.clear();
        ORDER_NUMBER.clear();
        TEMP_DATA.clear();
    }

}

// 1111 1111 1111 1026