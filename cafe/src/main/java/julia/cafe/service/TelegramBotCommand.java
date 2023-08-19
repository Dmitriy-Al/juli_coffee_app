package julia.cafe.service;

import julia.cafe.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
@Component
public class TelegramBotCommand extends TelegramLongPollingBot {

    private final HashMap<Long, String> TEMP_DATA = new HashMap<>();
    private final List<Integer> ORDERS_NUMBER = new ArrayList<>();
    private final HashMap<Long, String> GROCERY_BASKET = new HashMap<>();
    private final TelegramBotMethods method = new TelegramBotMethods();
    private String pictureLinc = "https://disk.yandex.ru/i/K331xGIlON2LxA";

    @Autowired
    public UserRepository userRepository;
    @Autowired
    public ProductRepository productRepository;
    @Autowired
    public ProductMenuCategoryRepository productMenuCategoryRepository;


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
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Здравствуйте, " + userName + "!\nМы рады предложить вам (текст с дифирамбами)\n" +
                        "Вы можете заказать и оплатить ваш кофе прямо сейчас и забрать ваш готовый заказ без ожидания \uD83D\uDD50");
                method.setKeyBoard(sendMessage);
                executeSendMessage(sendMessage);

            } else if (messageText.equals("☕" + " Кофе и напитки")) { // клавиатура
                List<MenuProductCategory> menuCategories = (List<MenuProductCategory>) productMenuCategoryRepository.findAll();

                executePhotoMessage(method.receiveCategoryMenu(chatId, "https://disk.yandex.ru/i/1QoAtJVbb3U8TA", menuCategories));

            } else if (messageText.equals("⚙" + " Регистрация и настройки")) {


            } else if (messageText.equals("Добавить новый продукт")) {
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "Введите через знак _ категорию меню для продукта, вид продукта, название, описание, ссылку на изображение и объём-цену продукта через пробел (отсутствующее поле: *)"));
                TEMP_DATA.put(chatId, "Добавить новый продукт");

            } else if (messageText.equals("Добавить категорию")) {
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "Введите через знак _ категорию для изображения и ссылку на него"));
                TEMP_DATA.put(chatId, "Добавить категорию");


            } else if (messageText.equals("Удалить продукт из ассортимента")) {


            } else if (messageText.contains("Корзина")) {
                if (GROCERY_BASKET.get(chatId) == null) {
                    SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Ваша корзина пуста");
                    method.setKeyBoard(sendMessage);
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

            } else if (TEMP_DATA.get(chatId) == null) {
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "Такая команда отсутствует"));
            }


            if (TEMP_DATA.get(chatId).equals("Добавить новый продукт")) {
                setProductInDB(messageText);
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "Продукт добавлен в ассортимент"));
                TEMP_DATA.remove(chatId);
            } else if (TEMP_DATA.get(chatId).equals("Добавить категорию")) {
                setPictureInDB(messageText);
                executeSendMessage(method.receiveCreatedSendMessage(chatId, "Категория добавлена"));
                TEMP_DATA.remove(chatId);
            }


            // Если update содержит изменённое сообщение
        } else if (update.hasCallbackQuery()) {
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String stringChatId = String.valueOf(chatId);

            String callbackData = update.getCallbackQuery().getData();

            if (callbackData.contains("#product")) {
                String productId = callbackData.replace("#product", "");
                Optional<Product> product = productRepository.findById(Integer.parseInt(productId));
                String[] sizeAndPrice = product.get().getSizeAndPrice().split(" ");
                //executeDeleteMessage(method.deleteMessage(chatId, messageId));
                executeEditMessageMedia(method.forSaleProduct(chatId, messageId, product.get().getProductPhotoLinc(), product.get().getProductInfo(), productId, sizeAndPrice));
                //executePhotoMessage(sendPhoto);

            } else if (callbackData.contains("#assortment")) { // "#assortmentraf"" "#assortmentsummer" "#assortmentwinter" "#assortmentall" "#assortmentclassic"
                String chooseMenuCategory = callbackData.replace("#assortment", "");
                Optional<MenuProductCategory> menuCategory = productMenuCategoryRepository.findByCategoryLikeIgnoreCase(chooseMenuCategory);
                List<Product> productList = (ArrayList<Product>) productRepository.findAll();
                executeEditMessageMedia(method.receiveProductMenu(chatId, messageId, chooseMenuCategory, menuCategory.get().getPictureLinc(), productList));

            } else if (callbackData.contains("#allassortment")) {


            }

            else if (callbackData.contains("@addproduct")) {
                String productIdAndSizeAndPrice = callbackData.replace("@addproduct", ""); // productIdAndSizeAndPrice - строка с данными: 103-300-150  productId-объём-цена
                if (GROCERY_BASKET.get(chatId) == null) {
                    GROCERY_BASKET.put(chatId, productIdAndSizeAndPrice + "#");
                } else {
                    String order = GROCERY_BASKET.get(chatId);
                    GROCERY_BASKET.put(chatId, order + productIdAndSizeAndPrice + "#");
                }

                String[] productData = productIdAndSizeAndPrice.split("-");
                Optional<Product> product = productRepository.findById(Integer.parseInt(productData[0]));

                if (product.get().getProductCategory().equals("кофе")) {
                    Iterable<Product> products = productRepository.findAll();
                    executeEditMessageMedia(method.receiveAddForSaleMenu(chatId, messageId, pictureLinc, products)); // TODO
                } else {
                    executeDeleteMessage(method.deleteMessage(chatId, messageId)); // TODO
                    SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ добавлен в корзину");
                    method.setKeyBoardAdd(sendMessage);
                    executeSendMessage(sendMessage);
                }

            } else if (callbackData.contains("@adds")) {
                String productIdAndPrice = callbackData.replace("@adds", ""); // productIdAndPrice - строка с данными: 103-*-150  productId-объём-цена
                String order = GROCERY_BASKET.get(chatId);
                GROCERY_BASKET.put(chatId, order + productIdAndPrice + "#");
                executeDeleteMessage(method.deleteMessage(chatId, messageId)); // TODO
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ добавлен в корзину");
                method.setKeyBoardAdd(sendMessage);
                executeSendMessage(sendMessage);

            } else if (callbackData.contains("@noadds")) {
                executeDeleteMessage(method.deleteMessage(chatId, messageId)); // TODO
                SendMessage sendMessage = method.receiveCreatedSendMessage(chatId, "Ваш заказ находится в корзине");
                method.setKeyBoardAdd(sendMessage);
                executeSendMessage(sendMessage);

            } else if (callbackData.contains("@settime")) {
                if (GROCERY_BASKET.get(chatId) == null) {
                    executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "Ваша корзина пуста"));
                } else {
                    long changeTime = Long.parseLong(callbackData.replace("@settime", ""));
                    executeEditMessageText(method.setTime(chatId, messageId, "С помощью клавиш ◄◄  и  ►► вы можете выбрать время, к которому ваш заказ будет готов:", changeTime));
                }

            } else if (callbackData.contains("@delorder")) {
                GROCERY_BASKET.remove(chatId);
                SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Заказ был удалены из корзины");
                method.setKeyBoard(sendMessage);
                executeSendMessage(sendMessage);
                executeDeleteMessage(method.deleteMessage(chatId, messageId));
                executeEditMessageText(method.receiveEditMessageText(chatId, messageId, ""));

            } else if (callbackData.contains("@makeorder")) {
                String time = callbackData.replace("@makeorder", "");

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
                String payLinc = receiveExecutedInvoiceLinc(method.payOrder("https://disk.yandex.ru/i/K331xGIlON2LxA", "401643678:TEST:95e3e338-2311-4825-bd6f-0de31a0b5ce8", labeledPriceList, chatId, messageId));

                executeEditMessageText(method.approveOrder(chatId, messageId, "После оплаты, ваш заказ будет готов к " + time, payLinc));

            } else if (callbackData.contains("@payorder")) {
                System.out.println("TEST @payorder");

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


            int orderNumber = method.createOrderNumber(ORDERS_NUMBER);
            System.out.println(orderNumber);
            ORDERS_NUMBER.add(orderNumber);

            GROCERY_BASKET.remove(chatId);
            executeEditMessageText(method.receiveEditMessageText(chatId, messageId, "del"));
            executeDeleteMessage(method.deleteMessage(chatId, messageId));
            SendMessage sendMessage = new SendMessage(chatIdAndMessageId[0], "Номер вашего заказа " + orderNumber + "\nСпасибо за заказ!");
            method.setKeyBoard(sendMessage);
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
            System.out.println(e.getMessage());
        }
    }


    public void executeSendMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("SendMessage execute error: " + e.getMessage());
        }
    }


    private void executePhotoMessage(SendPhoto sendPhoto) {
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            //log.error("SendMessage execute error: " + e.getMessage());
        }
    }

    private void executeEditMessageText(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            //log.error("SendMessage execute error: " + e.getMessage());
        }
    }


    private void executeDeleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {

        }
    }


    private void executeAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
            //log.error("SendMessage execute error: " + e.getMessage());
        }
    }


    // String productTitle, String productDescription, String productPhotoLinc, long price
    protected void setProductInDB(String productData) {
        String[] splitData = productData.split("_");
        Product product = new Product();
        product.setMenuCategory(splitData[0].toLowerCase());
        product.setProductCategory(splitData[1].toLowerCase());
        product.setProductTitle(splitData[2]);
        product.setProductDescription(splitData[3]);
        product.setProductPhotoLinc(splitData[4]);
        product.setSizeAndPrice(splitData[5]);
        productRepository.save(product);
    }


    protected void setPictureInDB(String pictureData) {
        String[] splitData = pictureData.split("_");
        MenuProductCategory menuProductCategory = new MenuProductCategory();
        System.out.println(splitData[0].toLowerCase() + " >>>> " + splitData[1]);
        menuProductCategory.setCategory(splitData[0]);
        menuProductCategory.setPictureLinc(splitData[1]);
        productMenuCategoryRepository.save(menuProductCategory);
    }


    private String receiveExecutedInvoiceLinc(CreateInvoiceLink createInvoiceLink) {
        String invoiceLincUrl = "null";
        try {
            invoiceLincUrl = execute(createInvoiceLink);
        } catch (TelegramApiException e) {
            log.error("SendMessage execute error: " + e.getMessage());
            System.out.println("Err: " + e.getMessage());
        }
        return invoiceLincUrl;
    }



}

