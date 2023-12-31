package julia.cafe.service;

import julia.cafe.model.MenuCategory;
import julia.cafe.model.Product;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import julia.cafe.model.User;

import java.sql.Timestamp;
import java.util.*;

@Component
public class TelegramBotMethods {


    protected void setAdminKeyBoard(SendMessage sendMessage) { // TODO setKeyBoardAdd(SendMessage sendMessage)
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); // уменьшенные кнопки
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("Добавить новый продукт");
        keyboardRows.add(firstRow);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("Добавить новую категорию меню");

        keyboardRows.add(secondRow);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add("Отправить сообщение пользователям");
        keyboardRows.add(thirdRow);

        KeyboardRow fourthRow = new KeyboardRow();
        fourthRow.add("Промо и аналитика"); //
        fourthRow.add("Удалить"); // "\uD83D\uDED2" + " Моя корзина"
        keyboardRows.add(fourthRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }


    // Набор кнопок экранной клавиатуры
    protected void setBaristaKeyBoard(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); // уменьшенные кнопки
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("Заказы");
        keyboardRows.add(firstRow);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("Увеличить время до выдачи заказа клиенту");
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }


    protected void setCommonKeyBoard(SendMessage sendMessage, List<Optional<Product>> test) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); // уменьшенные кнопки
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("☕️" + " Кофе и напитки");

        if (test == null) {
            firstRow.add("\uD83D\uDED2" + " Корзина");
            keyboardRows.add(firstRow);
        } else {
            firstRow.add("✅ \uD83D\uDED2" + " Корзина");
            keyboardRows.add(firstRow);
        }

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("⚙️" + " Регистрация и настройки");
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }


    protected CreateInvoiceLink payOrder(long chatId, int messageId, String providerToken, List<LabeledPrice> labeledPriceList, String photoUrl) {
/*      List<Integer> suggestedTipAmounts = new ArrayList<>(); //  - размер чаевых, опционально
        suggestedTipAmounts.add(1000);
        suggestedTipAmounts.add(3000);
        suggestedTipAmounts.add(5000);         */
        String jsonText = "{\"Текст\": \"Текст\",\"Число\": 12345}"; //TODO {\"Текст\": \"текст\",\"Число\": 12345}
        String invoiceText = chatId + "-" + messageId; // chatId и messageId будут получены в preCheckoutQuery.getInvoicePayload()

        return new CreateInvoiceLink("Juli coffee", // @NonNull String title
                "Ваша корзина", // @NonNull String description
                invoiceText, // @NonNull String payload - определенная полезная нагрузка счета-фактуры, 1-128 байт. Информация не видна пользователю, используйте для своих внутренних процессов
                providerToken, // @NonNull String providerToken - токен банка/провайдера платежей - "381764678:TEST:62053", // @NonNull String providerToken - Юкасса токен (Юкасса подключает физлиц)
                "RUB", // @NonNull String currency (валюта)
                labeledPriceList, // @NonNull List<LabeledPrice> prices
                photoUrl, // String photoUrl - фотография в меню покупки
                null, null, null, //  Integer photoSize, Integer photoWidth, Integer photoHeight

                // Boolean needName, Boolean needPhoneNumber, Boolean needEmail, Boolean needShippingAddress, Boolean isFlexible, Boolean sendPhoneNumberToProvider, Boolean sendEmailToProvider
                false, false, false, false /* needShippingAddress */, false /* isFlexible - цена зависит от доставки */, false/* sendPhoneNumberToProvider */, false,
                jsonText, // String providerData - JSON-сериализованные данные о счете-фактуре, которые будут переданы поставщику платежей. Подробное описание обязательных полей должно быть предоставлено поставщиком платежных услуг //TODO
                0, // Integer maxTipAmount - максимальный размер чаевых
                null); // List<Integer> suggestedTipAmounts) - размер чаевых
    }


    protected EditMessageText setTime(long chatId, int messageId, String messageText, int changeTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        //LocalTime time = LocalTime.now();
        LocalTime time = LocalTime.parse("21:28:00");
        int setTime = 5 * changeTime;
        LocalTime updateTime = time.plusMinutes(setTime);
        String timeString = dateTimeFormatter.format(updateTime);
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();
        if (updateTime.isAfter(LocalTime.parse("21:40:00")) || updateTime.isAfter(LocalTime.parse("00:00:00")) && updateTime.isBefore(LocalTime.parse("08:05:00"))) {
            editMessageText.setText("Часы работы Julia coffee с 8:00 до 22:00. К сожалению, данное время недоступно для заказа");
            return editMessageText;
        } else {
            InlineKeyboardButton setTimeButton = new InlineKeyboardButton();
            setTimeButton.setText("Забрать заказ в  \uD83D\uDD59 " + timeString);
            setTimeButton.setCallbackData("#makeorder" + timeString);

            InlineKeyboardButton minusTimeButton = new InlineKeyboardButton();
            minusTimeButton.setText("⏪  －５ минут");
            minusTimeButton.setCallbackData("#settime" + (changeTime - 1));

            InlineKeyboardButton plusTimeButton = new InlineKeyboardButton();
            plusTimeButton.setText("＋５ минут  ⏩"); // Оформить заказ
            plusTimeButton.setCallbackData("#settime" + (changeTime + 1));

            firstRowInlineButton.add(setTimeButton);
            if (changeTime > 1) {
                secondRowInlineButton.add(minusTimeButton);
            }
            secondRowInlineButton.add(plusTimeButton);
            rowsInline.add(firstRowInlineButton);
            rowsInline.add(secondRowInlineButton);

            inlineKeyboardMarkup.setKeyboard(rowsInline);
            editMessageText.setReplyMarkup(inlineKeyboardMarkup);
            return editMessageText;
        }
    }


    protected EditMessageText approveOrder(long chatId, int messageId, String messageText, String payLinc) {

        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();


        InlineKeyboardButton minusTimeButton = new InlineKeyboardButton(); // TODO
        minusTimeButton.setText("✅ Оплатить заказ");
        minusTimeButton.setUrl(payLinc);
        minusTimeButton.setCallbackData("#payorder");

        InlineKeyboardButton plusTimeButton = new InlineKeyboardButton();
        plusTimeButton.setText("Очистить корзину  \uD83D\uDDD1");
        plusTimeButton.setCallbackData("#delorder");

        secondRowInlineButton.add(minusTimeButton);

        secondRowInlineButton.add(plusTimeButton);
        rowsInline.add(secondRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }


    protected SendPhoto receiveCategoryMenu(long chatId, String pictureLinc, List<MenuCategory> menuCategories) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(pictureLinc));
        Collections.sort(menuCategories);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (int i = 0; i < menuCategories.size(); i++) {
            List<InlineKeyboardButton> inlineButton = new ArrayList<>();
            // если наименование категории < 5 символов, в горизонтальный ряд экранной клавиатуры помещается по 2 кнопки
            if (i + 1 < menuCategories.size() && menuCategories.get(i).getCategory().length() < 5 && menuCategories.get(i + 1).getCategory().length() < 5) {
                InlineKeyboardButton buttonOne = new InlineKeyboardButton();
                buttonOne.setText(menuCategories.get(i).getCategory());
                buttonOne.setCallbackData("#menucategory" + menuCategories.get(i).getCategoryId());
                inlineButton.add(buttonOne);
                InlineKeyboardButton buttonTwo = new InlineKeyboardButton();
                buttonTwo.setText(menuCategories.get(i + 1).getCategory());
                buttonTwo.setCallbackData("#menucategory" + menuCategories.get(i + 1).getCategoryId());
                inlineButton.add(buttonTwo);
                i++;
            } else {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(menuCategories.get(i).getCategory());
                button.setCallbackData("#menucategory" + menuCategories.get(i).getCategoryId());
                inlineButton.add(button);
            }
            rowsInline.add(inlineButton);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
        return sendPhoto;
    }


    public SendMessage receiveSettingsMenu(String chatId, boolean isAgree, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();

        InlineKeyboardButton registerButton = new InlineKeyboardButton();
        registerButton.setText("Регистрация");
        registerButton.setCallbackData("#register");

        InlineKeyboardButton getMessageButton = new InlineKeyboardButton();
        if (!isAgree) {
            getMessageButton.setText("\uD83D\uDFE2 Включить сообщения");
            getMessageButton.setCallbackData("#agree");
        } else {
            getMessageButton.setText("\uD83D\uDD34 Отключить сообщения");
            getMessageButton.setCallbackData("#notagree");
        }

        firstRowInlineButton.add(getMessageButton);
        rowsInline.add(firstRowInlineButton);
        secondRowInlineButton.add(registerButton);
        rowsInline.add(secondRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        return sendMessage;
    }


    public SendMessage receiveMenuForDelete(String chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();

        InlineKeyboardButton productButton = new InlineKeyboardButton();
        productButton.setText("Удалить продукт");
        productButton.setCallbackData("#delproduct");

        InlineKeyboardButton categoryButton = new InlineKeyboardButton();
        categoryButton.setText("Удалить категорию меню");
        categoryButton.setCallbackData("#delcategory");

        firstRowInlineButton.add(productButton);
        rowsInline.add(firstRowInlineButton);
        secondRowInlineButton.add(categoryButton);
        rowsInline.add(secondRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        return sendMessage;
    }


    // Метод создаёт меню с продуктами для удаления из бд
    public EditMessageText receiveProductForDelete(long chatId, int messageId, String messageText, List<Product> productList) {
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        productList.forEach(product -> {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            productButton.setText(product.getProductTitle());
            productButton.setCallbackData("#removeproduct" + product.getProductId());
            rowInlineButton.add(productButton);
            rowsInline.add(rowInlineButton);
        });

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageText;
    }

    // Метод создаёт меню категорий для удаления их из бд
    public EditMessageText receiveCategoryForDelete(long chatId, int messageId, String messageText, List<MenuCategory> productList) {
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        productList.forEach(category -> {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            productButton.setText(category.getCategory());
            productButton.setCallbackData("#removecategory" + category.getCategory());
            rowInlineButton.add(productButton);
            rowsInline.add(rowInlineButton);
        });

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageText;
    }

    // Метод создаёт меню с для раздела "Промо и аналитика"
    public SendMessage receivePromoAnalyticMenu(String chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> thirdRowInlineButton = new ArrayList<>();

        InlineKeyboardButton productButton = new InlineKeyboardButton();
        productButton.setText("Изменить изображение главного меню");
        productButton.setCallbackData("#changepic");

        InlineKeyboardButton categoryButton = new InlineKeyboardButton();
        categoryButton.setText("Акции и скидки");
        categoryButton.setCallbackData("#setdiscont");

        InlineKeyboardButton blockUserButton = new InlineKeyboardButton();
        blockUserButton.setText("Статистика");
        blockUserButton.setCallbackData("#userstatistic");

        firstRowInlineButton.add(productButton);
        rowsInline.add(firstRowInlineButton);
        secondRowInlineButton.add(categoryButton);
        rowsInline.add(secondRowInlineButton);
        thirdRowInlineButton.add(blockUserButton);
        rowsInline.add(thirdRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        return sendMessage;
    }

    // Метод создаёт редактированное сообщение с текстом, содержащем статистическую информацию
    public EditMessageText receiveStatistic(long chatId, int messageId, List<User> users) {
        Collections.sort(users);
        Timestamp lastUserRegisteredDate = users.get(users.size() - 1).getRegisteredDate();
        char pointer = 10033;
        int cashReceiptCount = 0;
        long amountPurchase = 0;
        long maxPurchase = 0;

        for (User user : users) {
            if (!user.getPurchase().equals("no purchase")) {
                String[] data = user.getPurchase().split("purchase");
                cashReceiptCount += data.length;
                for (String datum : data) {
                    String[] purchase = datum.split("-");
                    long purchasePrice = Long.parseLong(purchase[purchase.length - 1]);
                    amountPurchase += purchasePrice;
                    if (maxPurchase < purchasePrice) {
                        maxPurchase = purchasePrice;
                    }
                }
            }
        }

        long midCashReceipt = cashReceiptCount == 0 ? 0 : amountPurchase / cashReceiptCount;

        String text = "Общая статистика:" +
                "\n\n" + pointer + " Всего пользователей:  " +
                users.size() +
                "\n\n" + pointer + " Последний на текущий момент пользователь зарегистрирован, дата:  " +
                lastUserRegisteredDate +
                "\n\n" + pointer + " Количество покупок за всё время =  " +
                cashReceiptCount +
                "\n\n" + pointer + " Всего совершено покупок на сумму р. =  " +
                amountPurchase +
                "\n\n" + pointer + " Средняя цена чека р. =  " +
                midCashReceipt +
                "\n\n" + pointer + " Максимальная сумма чека р. =  " +
                maxPurchase;

        return receiveEditMessageText(chatId, messageId, text);
    }


    public EditMessageText receiveDiscountMenu(long chatId, int messageId, String messageText) {
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();

        InlineKeyboardButton productButton = new InlineKeyboardButton();
        productButton.setText("Установить/отменить скидку на продукт");
        productButton.setCallbackData("#allproducts");

        InlineKeyboardButton observeDiscountButton = new InlineKeyboardButton();
        observeDiscountButton.setText("Посмотреть текущие скидки/акции");
        observeDiscountButton.setCallbackData("#observediscount");

        firstRowInlineButton.add(productButton);
        rowsInline.add(firstRowInlineButton);
        secondRowInlineButton.add(observeDiscountButton);
        rowsInline.add(secondRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageText;
    }


    protected EditMessageMedia forSaleProduct(long chatId, int messageId, Optional<Product> product) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setMedia(new InputMediaPhoto(product.get().getProductPhotoLinc()));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        InlineKeyboardButton cancelProductButton = new InlineKeyboardButton();
        cancelProductButton.setText("❌ Отмена");
        cancelProductButton.setCallbackData("#cancelproduct");
        firstRowInlineButton.add(cancelProductButton);

        InlineKeyboardButton addProductButton = new InlineKeyboardButton();
        addProductButton.setText("В корзину ✅");
        addProductButton.setCallbackData("#addproduct" + product.get().getProductId());
        firstRowInlineButton.add(addProductButton);

        rowsInline.add(firstRowInlineButton);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageMedia;
    }


    protected SendMessage receiveGroceryBasket(String chatId, String messageText, List<Optional<Product>> productList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();

        for (Optional<Product> product : productList) {
            String price = product.get().getProductDiscount() == 0 ? product.get().getProductPrice() + " ₽" :
                    receiveFullPrice(Integer.parseInt(product.get().getProductPrice()), product.get().getProductDiscount()) + " ₽ \uD83D\uDD25%";
            String productSize = product.get().getProductSize().equals("*") ? " " : product.get().getProductSize() + " ml  ";
            stringBuilder.append("\n\n✔️  ").append(product.get().getProductTitle()).append("  ").append(productSize).append(price);
        }

        InlineKeyboardButton clearButton = new InlineKeyboardButton();
        clearButton.setText("Очистить корзину  \uD83D\uDDD1");
        clearButton.setCallbackData("#delorder");

        InlineKeyboardButton orderButton = new InlineKeyboardButton();
        orderButton.setText("Забрать заказ в...  \uD83D\uDD59"); // Оформить заказ
        orderButton.setCallbackData("#settime" + 1);

        firstRowInlineButton.add(clearButton);
        secondRowInlineButton.add(orderButton);
        rowsInline.add(firstRowInlineButton);
        rowsInline.add(secondRowInlineButton);

        SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        return sendMessage;
    }


    protected EditMessageMedia receiveProductAssortmentDistinct(long chatId, int messageId, String picturesLink, List<Product> productList) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(picturesLink));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Product product : productList) {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getProductTitle());
            button.setCallbackData("#prodtitle" + product.getProductTitle());
            rowInlineButton.add(button);
            rowsInline.add(rowInlineButton);
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageMedia;
    }


    protected EditMessageMedia receiveProductSameTitle(long chatId, int messageId, String picturesLink, List<Product> productList) { //, Map<Integer, Integer> discount
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(picturesLink));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок

        for (Product product : productList) {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            /*
            String buttonText = discount.get(product.getProductId()) == null ? product.getProductTitle() + "   " + product.getProductSize() + "ml  " + product.getProductPrice() + " ₽" :
                    product.getProductTitle() + "   " + product.getProductSize() + "ml  " + product.getProductPrice() + " ₽" + "  \uD83D\uDD25 -" + discount.get(product.getProductId()) + "%";*/
            String buttonText = product.getProductDiscount() == 0 ? product.getProductTitle() + "   " + product.getProductSize() + "ml  " + product.getProductPrice() + " ₽" :
                    product.getProductTitle() + "   " + product.getProductSize() + "ml  " + product.getProductPrice() + " ₽" + "  \uD83D\uDD25 -" + product.getProductDiscount() + "%";
            button.setText(buttonText);
            button.setCallbackData("#product" + product.getProductId());
            rowInlineButton.add(button);
            rowsInline.add(rowInlineButton);
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageMedia;
    }


    protected SendMessage receiveOrderMessage(long chatId, int orderNumber, String dataText) {
        String messageText;
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (dataText.equals(" # заказ передан покупателю")) {
            messageText = orderNumber + dataText;
            sendMessage.setText(messageText);
        } else {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            messageText = "Заказ " + orderNumber + "\n" + dataText.replaceAll("#", "\n").replaceAll("\\*ml", " ");
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Заказ забран");
            button.setCallbackData("#ordernum" + orderNumber);

            rowInlineButton.add(button);
            rowsInline.add(rowInlineButton);
            inlineKeyboardMarkup.setKeyboard(rowsInline);

            sendMessage.setText(messageText);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        }

        return sendMessage;
    }


    // Метод создаёт меню с ассортиментом сиропов и простым текстовым сообщением
    protected SendMessage receiveSyrupMenu(long chatId, String messageText, List<Product> syrups) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Product syr : syrups) {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            productButton.setText(syr.getProductTitle() + " " + syr.getProductPrice() + " ₽");
            productButton.setCallbackData("#addsyrup" + syr.getProductId());
            rowInlineButton.add(productButton);
            rowsInline.add(rowInlineButton);
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Без сиропа");
        button.setCallbackData("#nosyrup");

        rowInlineButton.add(button);
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        return sendMessage;
    }


    // Метод создаёт меню с добавками для кофе и простым текстовым сообщением
    protected EditMessageText receiveSupplementMenu(long chatId, int messageId, String messageText, List<Product> supplements) {
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Product sup : supplements) {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            productButton.setText(sup.getProductTitle() + " " + sup.getProductPrice() + " ₽");
            productButton.setCallbackData("#addsup" + sup.getProductId());
            rowInlineButton.add(productButton);
            rowsInline.add(rowInlineButton);
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Без добавок");
        button.setCallbackData("#nosup");

        rowInlineButton.add(button);
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }


    // Метод создаёт меню с полным ассортиментом продуктов
    protected EditMessageText receiveAllProductMenu(long chatId, int messageId, String messageText, List<Product> products) {
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Product product : products) {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            String buttonText = product.getProductDiscount() == 0 ? product.getProductTitle() + " " + product.getProductSize() +
                    " ml" : product.getProductTitle() + " " + product.getProductSize() + " ml  \uD83D\uDD25 -" + product.getProductDiscount() + "%";
            productButton.setText(buttonText);
            productButton.setCallbackData("#prodfordiscount" + product.getProductId());
            rowInlineButton.add(productButton);
            rowsInline.add(rowInlineButton);
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌  Отмена");
        cancelButton.setCallbackData("#cancelmessage");

        rowInlineButton.add(cancelButton);
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }


    // Метод меню добавления/удаления скидки на продукт
    public EditMessageText receiveProductForDiscount(long chatId, int messageId, String messageText, int productId) {
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();

        InlineKeyboardButton addDiscountButton = new InlineKeyboardButton();
        addDiscountButton.setText("Установить скидку");
        addDiscountButton.setCallbackData("#setdiscount" + productId);

        InlineKeyboardButton deleteDiscountButton = new InlineKeyboardButton();
        deleteDiscountButton.setText("Убрать скидку");
        deleteDiscountButton.setCallbackData("#deldiscount" + productId);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌  Отмена");
        cancelButton.setCallbackData("#cancelmessage");


        firstRowInlineButton.add(addDiscountButton);
        firstRowInlineButton.add(deleteDiscountButton);
        rowsInline.add(firstRowInlineButton);
        secondRowInlineButton.add(cancelButton);
        rowsInline.add(secondRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageText;
    }


    // Метод возвращает true, если при добавлении продукта введённые данные валидны
    public boolean isProductStringValid(String[] productData) {
        if (productData.length == 7 && (productData[4].contains("http") ^ productData[4].contains("*"))) {
            try {
                String stringNumber = productData[5].replace("*", "") + productData[6];
                Long parseNumber = Long.parseLong(stringNumber);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        } else return false;
    }


    protected int createOrderNumber(LinkedHashMap<Integer, String> orderNumbers) {
        Integer[] newOrderNumber;
        if (orderNumbers.isEmpty()) {
            newOrderNumber = new Integer[1];
            newOrderNumber[0] = (int) (Math.random() * 1000) + 1;
        } else {
            newOrderNumber = new Integer[orderNumbers.size()];
            orderNumbers.keySet().toArray(newOrderNumber);
        }
        return newOrderNumber[newOrderNumber.length - 1] + 1;
    }


    // Отправка сообщения
    protected SendMessage receiveCreatedSendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }


    protected EditMessageText receiveEditMessageText(long chatId, int messageId, String messageText) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(messageText);
        return editMessageText;
    }


    protected DeleteMessage deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setMessageId(messageId);
        deleteMessage.setChatId(chatId);
        return deleteMessage;
    }


    public SendDocument sendDocument(long chatId, String directoryPath) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setCaption("Ваш кассовый чек");
        sendDocument.setDocument(new InputFile(new File(directoryPath))); // "C:\\2041024245_29.08.2023_18.50.09.pdf"
        return sendDocument;
    }


    public int receiveDiscountPrice(Map<Integer, Integer> discount, int productId, int price) {
        int upPrice = price * 100;
        int upDiscount = discount.get(productId) == null ? 0 : discount.get(productId) * price;
        return (upPrice - upDiscount) / 100;
    }

    public int receiveFullPrice(int price, int discount) {
        int upPrice = price * 100;
        int upDiscount =  discount * price;
        return (upPrice - upDiscount) / 100;
    }


////////////////////////////////////////////////// TEST ///////////////////////////////////////////////////////////////



}

// 639002000000000003   1111 1111 1111 1026

