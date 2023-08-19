package julia.cafe.service;

import julia.cafe.model.MenuProductCategory;
import julia.cafe.model.Product;
import julia.cafe.model.ProductComparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink;
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class TelegramBotMethods {

    private ProductComparator productComparator = new ProductComparator();
   // private MenuCategoryComparator menuCategoryComparator = new MenuCategoryComparator();

    // Набор кнопок экранной клавиатуры
    protected void setKeyBoard(SendMessage sendMessage) { // TODO setKeyBoardAdd(SendMessage sendMessage)
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); // уменьшенные кнопки
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("☕" + " Кофе и напитки"); // "☕" + " Кофе и напитки"
        firstRow.add("\uD83D\uDED2" + " Корзина"); // "\uD83D\uDED2" + " Моя корзина"
        keyboardRows.add(firstRow);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("⚙" + " Регистрация и настройки"); // "⚙" + " Регистрация и настройки"
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }


    // Набор кнопок экранной клавиатуры
    protected void setKeyBoardAdd(SendMessage sendMessage) { // TODO setKeyBoardAdd(SendMessage sendMessage)
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); // уменьшенные кнопки
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("☕" + " Кофе и напитки"); // "☕" + " Кофе и напитки"
        firstRow.add("✅ \uD83D\uDED2" + " Корзина"); // "\uD83D\uDED2" + " Моя корзина"
        keyboardRows.add(firstRow);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("⚙" + " Регистрация и настройки"); // "⚙" + " Регистрация и настройки"
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


    protected EditMessageMedia receiveAddForSaleMenu(long chatId, int messageId, String pictureLinc, Iterable<Product> products) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(pictureLinc));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок

        for(Product product : products){
            if(product.getProductCategory().equals("добавка")) {
                List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
                String price = product.getSizeAndPrice().replace("*-", "");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("добавка к кофе " + product.getProductTitle() + " " + price + " ₽");
                button.setCallbackData("@adds" + product.getProductId() + "-*-" + price);

                rowInlineButton.add(button);
                rowsInline.add(rowInlineButton);
            }
        }

        List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Без добавок");
        button.setCallbackData("@noadds");

        rowInlineButton.add(button);
        rowsInline.add(rowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageMedia;
    }

    //



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

        for(int i = 0; i < productData.length; i++){
            String[] orders = productData[i].split("-");
            stringBuilder.append("\n\n✔  ").append(orders[0]).append("   ").append(orders[3]).append(" ₽   ");
            if(!orders[2].contains("*")){
                stringBuilder.append(orders[2]).append(" ml");
            }
        }

        InlineKeyboardButton clearButton = new InlineKeyboardButton();
        clearButton.setText("Очистить корзину  \uD83D\uDDD1");
        clearButton.setCallbackData("@delorder");

        InlineKeyboardButton orderButton = new InlineKeyboardButton();
        orderButton.setText("Забрать заказ в...  \uD83D\uDD59"); // Оформить заказ
        orderButton.setCallbackData("@settime" + 1);

        firstRowInlineButton.add(clearButton);
        secondRowInlineButton.add(orderButton);
        rowsInline.add(firstRowInlineButton);
        rowsInline.add(secondRowInlineButton);

        SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        return sendMessage;
    }


    protected CreateInvoiceLink payOrder(String photoUrl, String providerToken, List<LabeledPrice> labeledPriceList,  long chatId, int messageId) {
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
        LocalTime time = LocalTime.now();
        long setTime = 5 * changeTime;
        LocalTime updateTime = time.plusMinutes(setTime);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeString = dateTimeFormatter.format(updateTime);

        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> firstRowInlineButton = new ArrayList<>();
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();

        InlineKeyboardButton setTimeButton = new InlineKeyboardButton();
        setTimeButton.setText("Забрать заказ в  \uD83D\uDD59 " + timeString);
        setTimeButton.setCallbackData("@makeorder" + timeString);

        InlineKeyboardButton minusTimeButton = new InlineKeyboardButton();
        minusTimeButton.setText("⏪  －５ минут");
        minusTimeButton.setCallbackData("@settime" + (changeTime - 1));

        InlineKeyboardButton plusTimeButton = new InlineKeyboardButton();
        plusTimeButton.setText("＋５ минут  ⏩"); // Оформить заказ
        plusTimeButton.setCallbackData("@settime" + (changeTime + 1));

        firstRowInlineButton.add(setTimeButton);
        if(changeTime > 1){
            secondRowInlineButton.add(minusTimeButton);
        }
        secondRowInlineButton.add(plusTimeButton);
        rowsInline.add(firstRowInlineButton);
        rowsInline.add(secondRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }



    protected EditMessageText approveOrder(long chatId, int messageId, String messageText, String payLinc) {

        EditMessageText editMessageText = receiveEditMessageText(chatId, messageId, messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок
        List<InlineKeyboardButton> secondRowInlineButton = new ArrayList<>();


        InlineKeyboardButton minusTimeButton = new InlineKeyboardButton(); // TODO
        minusTimeButton.setText("✅ Оплатить заказ");
        minusTimeButton.setUrl(payLinc);
        minusTimeButton.setCallbackData("@payorder");

        InlineKeyboardButton plusTimeButton = new InlineKeyboardButton();
        plusTimeButton.setText("Очистить корзину  \uD83D\uDDD1");
        plusTimeButton.setCallbackData("@delorder");

        secondRowInlineButton.add(minusTimeButton);

        secondRowInlineButton.add(plusTimeButton);
        rowsInline.add(secondRowInlineButton);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }


    protected int createOrderNumber(List<Integer> orderNumbers) {
        int newOrderNumber;
        if(orderNumbers.isEmpty()){
            newOrderNumber = (int) (Math.random() * 1000) + 1;
        } else {
            newOrderNumber = orderNumbers.get(orderNumbers.size() - 1) + 1;
        }
        return newOrderNumber;
    }


    protected EditMessageMedia receiveProductMenu(long chatId, int messageId, String chooseMenu, String picturesLink, List<Product> productList) {
        System.out.println("Test>" + chooseMenu +  "<>" + picturesLink + "<>" + productList.size() + "<>");
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(picturesLink));
        productList.sort(productComparator);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок

        for(Product product : productList){
            if(product.getMenuCategory().equalsIgnoreCase(chooseMenu) || chooseMenu.equalsIgnoreCase("весь ассортимент") && !product.getSizeAndPrice().contains("*")) {
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


    protected EditMessageMedia forSaleProduct(long chatId,  int messageId, String pictureLinc, String descriptionText, String productId, String[] sizeAndPrice) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setMedia(new InputMediaPhoto(pictureLinc));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок

        for(int i = 0; i < sizeAndPrice.length; i++){
            String[] splitSizeAndPrice = sizeAndPrice[i].split("-");
            List<InlineKeyboardButton> rowInlineButton = new ArrayList<>();
            String buttonText = "Добавить в корзину    " + splitSizeAndPrice[1] + " ₽";
            if(!splitSizeAndPrice[1].equals("-")){
                buttonText += "    " + splitSizeAndPrice[0] + " ml";
            }
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData("@addproduct" + productId + "-" + sizeAndPrice[i]);
            rowInlineButton.add(button);
            rowsInline.add(rowInlineButton);
        }
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        editMessageMedia.setReplyMarkup(inlineKeyboardMarkup);
        return editMessageMedia;
    }


    protected SendPhoto receiveCategoryMenu(long chatId, String pictureLinc, List<MenuProductCategory> menuCategories) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(pictureLinc));
        Collections.sort(menuCategories);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // коллекция коллекций с горизонтальным рядом кнопок, создаёт вертикальный ряд кнопок

        for(int i = 0; i < menuCategories.size(); i++){

            List<InlineKeyboardButton> inlineButton = new ArrayList<>();
            if(i + 1 < menuCategories.size() && menuCategories.get(i).getCategory().length() < 5 && menuCategories.get(i + 1).getCategory().length() < 5 ){
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





////////////////////////////////////////////////// TEST ///////////////////////////////////////////////////////////////





















/*
    // Отправка спойлера изображения с прикреплённым сообщением
    protected SendPhoto receiveCreatedSendPhotoFromNet(long chatId, String pictureLinc, String text) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(text); // прикреплённое сообщения
        sendPhoto.setPhoto(new InputFile(pictureLinc));
        return sendPhoto;
    }

    protected AnswerCallbackQuery receiveCreateAnswerCallbackQuery(String text, boolean showAlert, String callbackQuery){
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackQuery);
        answerCallbackQuery.setText(text); // прикреплённое сообщения
        answerCallbackQuery.setShowAlert(showAlert); // Присутствие/отсутствие всплывающего окна (текст выводится в любом случае)
        return answerCallbackQuery;
    }


    protected EditMessageMedia receiveEditMessageMedia(long chatId, int messageId, String pictureLinc) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setMessageId(messageId);
        editMessageMedia.setChatId(chatId);
        editMessageMedia.setMedia(new InputMediaPhoto(pictureLinc));
        return editMessageMedia;
    }
 */


}
