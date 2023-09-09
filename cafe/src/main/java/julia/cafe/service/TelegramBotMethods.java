package julia.cafe.service;

import julia.cafe.model.MenuCategory;
import julia.cafe.model.Product;
import julia.cafe.model.ProductComparator;
import julia.cafe.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class TelegramBotMethods {

    private ProductComparator productComparator = new ProductComparator();
    // private MenuCategoryComparator menuCategoryComparator = new MenuCategoryComparator();


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
    protected void setCommonKeyBoard(SendMessage sendMessage, String groceryBasket) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); // уменьшенные кнопки
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("☕️" + " Кофе и напитки"); // "☕️" + " Кофе и напитки"

        if(groceryBasket == null){
            firstRow.add("\uD83D\uDED2" + " Корзина"); // "\uD83D\uDED2" + " Моя корзина"
            keyboardRows.add(firstRow);
        } else {
            firstRow.add("✅ \uD83D\uDED2" + " Корзина"); // "\uD83D\uDED2" + " Моя корзина"
            keyboardRows.add(firstRow);
        }

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("⚙️" + " Регистрация и настройки"); // "⚙️" + " Регистрация и настройки"
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
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


    protected SendMessage receiveGroceryBasket(String chatId, String messageText, String[] productData) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();

        for (int i = 0; i < productData.length; i++) {
            String[] orders = productData[i].split("-");
            stringBuilder.append("\n\n✔️  ").append(orders[0]).append("   ").append(orders[3]).append(" ₽   ");
            if (!orders[2].contains("*")) {
                stringBuilder.append(orders[2]).append(" ml");
            }
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


    protected CreateInvoiceLink payOrder(String photoUrl, String providerToken, List<LabeledPrice> labeledPriceList, long chatId, int messageId) {
        String jsonText = "{\"Текст\": \"текст\",\"Число\": 12345}";
        String invoiceText = chatId + "-" + messageId;
        List<Integer> suggestedTipAmounts = new ArrayList<>(); //  - размер чаевых
        suggestedTipAmounts.add(1000);
        suggestedTipAmounts.add(3000);
        suggestedTipAmounts.add(5000);
        return new CreateInvoiceLink("Juli coffee", // @NonNull String title
                "Ваша корзина", // @NonNull String description
                invoiceText, // @NonNull String payload - определенная полезная нагрузка счета-фактуры, 1-128 байт. Это не будет отображаться пользователю, используйте для своих внутренних процессов
                providerToken, // @NonNull String providerToken - Сбер токен
                // "381764678:TEST:62053", // @NonNull String providerToken - Юкасса токен (Юкасса подключает физлиц)
                "RUB", // @NonNull String currency
                labeledPriceList, // @NonNull List<LabeledPrice> prices
                "https://lookaside.fbsbx.com/lookaside/crawler/media/?media_id=101383654886643", // String photoUrl фотография в меню покупки
                null, null, null, //  Integer photoSize, Integer photoWidth, Integer photoHeight

                // Boolean needName, Boolean needPhoneNumber, Boolean needEmail, Boolean needShippingAddress, Boolean isFlexible, Boolean sendPhoneNumberToProvider, Boolean sendEmailToProvider
                true, true, false, true /* needShippingAddress */, false /* isFlexible - цена зависит от доставки */, true/* sendPhoneNumberToProvider */, false,
                jsonText, // String providerData - JSON-сериализованные данные о счете-фактуре, которые будут переданы поставщику платежей. Подробное описание обязательных полей должно быть предоставлено поставщиком платежных услуг
                50000, // Integer maxTipAmount - максимальный размер чаевых
                null); // List<Integer> suggestedTipAmounts) - размер чаевых

    }


    protected EditMessageText setTime(long chatId, int messageId, String messageText, long changeTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        //LocalTime time = LocalTime.now();
        LocalTime time = LocalTime.parse("21:28:00");
        long setTime = 5 * changeTime;
        LocalTime updateTime = time.plusMinutes(setTime);
        String timeString = dateTimeFormatter.format(updateTime);
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();
        if (updateTime.isAfter(LocalTime.parse("21:40:00")) || updateTime.isAfter(LocalTime.parse("00:00:00")) && updateTime.isBefore(LocalTime.parse("08:05:00"))){
            editMessageText.setText("Часы работы Julia coffee с 8:00 до 22:00. К сожалению, данное время недоступно для заказа");
            return editMessageText;
        } else{
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


    protected int createOrderNumber(List<Integer> orderNumbers) {
        int newOrderNumber;
        if (orderNumbers.isEmpty()) {
            newOrderNumber = (int) (Math.random() * 1000) + 1;
        } else {
            newOrderNumber = orderNumbers.get(orderNumbers.size() - 1) + 1;
        }
        return newOrderNumber;
    }


    protected EditMessageMedia receiveProductAssortment(long chatId, int messageId, String chooseMenu, String picturesLink, List<Product> productList) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(picturesLink));
        productList.sort(productComparator);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок

        for (Product product : productList) {
            if (product.getMenuCategory().equalsIgnoreCase(chooseMenu) || chooseMenu.equalsIgnoreCase("весь ассортимент") && !product.getSizeAndPrice().contains("*")){
                List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(product.getProductTitle());
                button.setCallbackData("#product" + product.getProductId());
                rowInlineButton.add(button);
                rowsInline.add(rowInlineButton);
            }
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageMedia;
    }


    protected EditMessageMedia forSaleProduct(long chatId, int messageId, String pictureLinc, String productId, String[] sizeAndPrice) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setMedia(new InputMediaPhoto(pictureLinc));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 0; i < sizeAndPrice.length; i++) {
            String[] splitSizeAndPrice = sizeAndPrice[i].split("-");
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            String buttonText = "Добавить в корзину    " + splitSizeAndPrice[1] + " ₽";
            if (!splitSizeAndPrice[1].equals("-")) {
                buttonText += "    " + splitSizeAndPrice[0] + " ml";
            }
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData("#addproduct" + productId + "-" + sizeAndPrice[i]);
            rowInlineButton.add(button);
            rowsInline.add(rowInlineButton);
        }
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageMedia;
    }


    protected SendPhoto forSaleProduct(long chatId, String pictureLinc, String descriptionText, String productId, String[] sizeAndPrice) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(descriptionText);
        sendPhoto.setPhoto(new InputFile(pictureLinc));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 0; i < sizeAndPrice.length; i++) {
            String[] splitSizeAndPrice = sizeAndPrice[i].split("-");
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            String buttonText = "Добавить в корзину    " + splitSizeAndPrice[1] + " ₽";
            if (!splitSizeAndPrice[1].equals("-")) {
                buttonText += "    " + splitSizeAndPrice[0] + " ml";
            }
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData("#addproduct" + productId + "-" + sizeAndPrice[i]);
            rowInlineButton.add(button);
            rowsInline.add(rowInlineButton);
        }
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
        return sendPhoto;
    }


    protected SendPhoto receiveCategoryMenu(long chatId, String pictureLinc, List<MenuCategory> menuCategories) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(pictureLinc));
        Collections.sort(menuCategories);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        for (int i = 0; i < menuCategories.size(); i++) {
            List<InlineKeyboardButton> inlineButton = new ArrayList<>();
            if (i + 1 < menuCategories.size() && menuCategories.get(i).getCategory().length() < 5 && menuCategories.get(i + 1).getCategory().length() < 5) {
                InlineKeyboardButton buttonOne = new InlineKeyboardButton();
                buttonOne.setText(menuCategories.get(i).getCategory());
                buttonOne.setCallbackData("#assortment" + menuCategories.get(i).getCategory());
                inlineButton.add(buttonOne);
                InlineKeyboardButton buttonTwo = new InlineKeyboardButton();
                buttonTwo.setText(menuCategories.get(i + 1).getCategory());
                buttonTwo.setCallbackData("#assortment" + menuCategories.get(i + 1).getCategory());
                inlineButton.add(buttonTwo);
                i++;
            } else {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(menuCategories.get(i).getCategory());
                button.setCallbackData("#assortment" + menuCategories.get(i).getCategory());
                inlineButton.add(button);
            }
            rowsInline.add(inlineButton);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
        return sendPhoto;
    }


    protected EditMessageMedia receiveSupplementForProduct(long chatId, int messageId, String pictureLinc, List<Product> products) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(pictureLinc));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок

        for (Product product : products) {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            String buttonText;
            String callbackData;
            if (product.getSizeAndPrice().equals("*-*")) {
                buttonText = "сироп " + product.getProductTitle();
                callbackData = "#supplement" + product.getProductId() + "-*-" + "0";
            } else {
                buttonText = "добавка к кофе " + product.getProductTitle() + " " + product.getSizeAndPrice().replace("*-", "") + " ₽";
                callbackData = "#supplement" + product.getProductId() + "-*-" + product.getSizeAndPrice().replace("*-", "");
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData(callbackData);

            rowInlineButton.add(button);
            rowsInline.add(rowInlineButton);
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Без добавок");
        button.setCallbackData("#nosupplement");

        rowInlineButton.add(button);
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageMedia;
    }


    public SendDocument sendDocument(long chatId, String directoryPath) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setCaption("Ваш кассовый чек");
        sendDocument.setDocument(new InputFile(new File(directoryPath))); //"C:\\Users\\admit\\Desktop\\2041024245_29.08.2023_18.50.09.pdf"
        return sendDocument;
    }


    public SendMessage receiveRegisterAndSettingsMenu(String chatId, String text, boolean isAgree) {
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
        //List<InlineKeyboardButton> thirdRowInlineButton = new ArrayList<>();

        InlineKeyboardButton productButton = new InlineKeyboardButton();
        productButton.setText("Удалить продукт");
        productButton.setCallbackData("#delproduct");

        InlineKeyboardButton categoryButton = new InlineKeyboardButton();
        categoryButton.setText("Удалить категорию меню");
        categoryButton.setCallbackData("#delcategory");

        //InlineKeyboardButton blockUserButton = new InlineKeyboardButton();
        //blockUserButton.setText("Заблокировать пользователя");
        //blockUserButton.setCallbackData("#blockuser");

        firstRowInlineButton.add(productButton);
        rowsInline.add(firstRowInlineButton);
        secondRowInlineButton.add(categoryButton);
        rowsInline.add(secondRowInlineButton);
        //thirdRowInlineButton.add(blockUserButton);
        //rowsInline.add(thirdRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        return sendMessage;
    }


    // Метод возвращает true, если при добавлении продукта введённые данные валидны
    public boolean isProductStringValid(String[] productData){
        if(productData.length == 6 && (productData[4].contains("http") ^ productData[4].contains("*"))){
            try{
                String stringNumber = productData[5].replaceAll(" ", "").replaceAll("\\*", "").replaceAll("-", "");
                Long parseNumber = Long.parseLong(stringNumber);System.out.println("test 4");
            } catch (NumberFormatException e){System.out.println("test 6");
                return false;
            }
            return true;
        } else return false;
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

    // Метод создаёт меню с добавками для кофе
    protected EditMessageMedia receiveSupplementMenu(long chatId, int messageId, String pictureLinc, List<Product> supplements) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(pictureLinc));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        supplements.forEach(sup -> {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            String syrupFirCoffee = " " + sup.getSizeAndPrice().replace("*-", "") + " ₽";
            productButton.setText(sup.getProductTitle() + syrupFirCoffee);
            productButton.setCallbackData("#addsupplement" + sup.getProductId() + "-*-" + sup.getSizeAndPrice().replace("*-", ""));
            rowInlineButton.add(productButton);
            rowsInline.add(rowInlineButton);
        });

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Без добавок");
        button.setCallbackData("#nosupplement");

        rowInlineButton.add(button);
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageMedia;
    }

    // Метод создаёт меню с сиропами для кофе
    protected EditMessageMedia receiveSyrupMenu(long chatId, int messageId, String pictureLinc,  String product, List<Product> syrups) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(pictureLinc));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        syrups.forEach(sup -> {
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            if(product.equalsIgnoreCase("кофе")){
                String syrupForCoffee = " " + sup.getSizeAndPrice().replace("*-", "") + " ₽";
                productButton.setText(sup.getProductTitle() + syrupForCoffee);
                productButton.setCallbackData("#addsyrup" + sup.getProductId() + "-*-" + sup.getSizeAndPrice().replace("*-", ""));
            } else {
                String syrupForRaf = " " + " 0 ₽";
                productButton.setText(sup.getProductTitle() + syrupForRaf);
                productButton.setCallbackData("#addsyrup" + sup.getProductId() + "-*-" + "0");
            }
            rowInlineButton.add(productButton);
            rowsInline.add(rowInlineButton);
        });

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Без сиропа");
        button.setCallbackData("#nosyrup");

        rowInlineButton.add(button);
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageMedia;
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
    public EditMessageText receiveStatistic(long chatId, int messageId, ArrayList<User> users) {
        Collections.sort(users);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy");
        int currentYear = Integer.parseInt(dateTimeFormatter.format(LocalDate.now()));

        Timestamp lastUserRegisteredDate = users.get(users.size() - 1).getRegisteredDate();
        int usersAge = 0;
        int countBirthYear = 0;
        int cashReceiptCount = 0;
        long amountPurchase = 0;
        long maxPurchase = 0;
        char pointer = 10033;

        for(int i = 0; i < users.size(); i++){
            if(users.get(i).getBirthday() != null) {
                String[] birth = users.get(i).getBirthday().split("-");
                int age = currentYear - Integer.parseInt(birth[0]);
                usersAge += age; System.out.println("<<" + age);
                countBirthYear++;
            }
            if(users.get(i).getPurchase() != null){
                String[] data = users.get(i).getPurchase().split("purchase");
                cashReceiptCount += data.length;
                for(int y = 0; y < data.length; y++){
                    String[] purchase = data[y].split("-");
                    long purchasePrice = Long.parseLong(purchase[purchase.length - 1]);
                    amountPurchase += purchasePrice;
                    if(maxPurchase < purchasePrice){
                        maxPurchase = purchasePrice;
                    }
                }
            }
        }

        String stringBuffer = "Общая статистика:" +
                "\n\n" + pointer + " Всего пользователей:  " +
                users.size() +
                "\n\n" + pointer + " Средний возраст зарегистрированных пользователей, лет:  " +
                usersAge / countBirthYear +
                "\n\n" + pointer + " Последний на текущий момент пользователь зарегистрирован, дата:  " +
                lastUserRegisteredDate +
                "\n\n" + pointer + " Количество покупок за всё время =  " +
                cashReceiptCount +
                "\n\n" + pointer + " Всего совершено покупок на сумму р. =  " +
                amountPurchase +
                "\n\n" + pointer + " Средняя цена чека р. =  " +
                amountPurchase / cashReceiptCount +
                "\n\n" + pointer + " Максимальная сумма чека р. =  " +
                maxPurchase;

        return  receiveEditMessageText(chatId, messageId, stringBuffer);
    }










////////////////////////////////////////////////// TEST ///////////////////////////////////////////////////////////////

    public EditMessageText receiveStacMenu(long chatId, int messageId, String messageText) {
        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> thirdRowInlineButton = new ArrayList<>();

        InlineKeyboardButton productButton = new InlineKeyboardButton();
        productButton.setText("С");
        productButton.setCallbackData("#userlist");

        InlineKeyboardButton categoryButton = new InlineKeyboardButton();
        categoryButton.setText("С");
        categoryButton.setCallbackData("#chooseinfo");

        InlineKeyboardButton blockUserButton = new InlineKeyboardButton();
        blockUserButton.setText("С");
        blockUserButton.setCallbackData("#userstatistic");

        firstRowInlineButton.add(productButton);
        rowsInline.add(firstRowInlineButton);
        secondRowInlineButton.add(categoryButton);
        rowsInline.add(secondRowInlineButton);
        thirdRowInlineButton.add(blockUserButton);
        rowsInline.add(thirdRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageText;
    }








}

// 639002000000000003   1111 1111 1111 1026
