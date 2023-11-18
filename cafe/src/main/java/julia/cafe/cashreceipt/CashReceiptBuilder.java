package julia.cafe.cashreceipt;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class CashReceiptBuilder {

    private String companyTitle; // "ИП Джулия кофе" - наименование фирмы "ИП Джулия кофе"
    private String companyAttribute; // адрес, по которому зарегистрирована касса/кошелёк
    private String documentTitle; // название документа: "Кассовый чек"
    private String invoiceSort; // признак расчёта "приход"
    private String payment; // вид оплаты - "электронными"
    private String seller; // кассир - фио продавца - "Сбер касса"
    private String fnsSite; //  сайт ФНС - "сайт ФНС www.nalog.ru"
    private String anyText; // любой сопроводительный текст - "Спасибо за покупку!"
    private String ofd; //  оператор фискальных данных - ""
    private String rnkkt; // регистрационный номер ккт - "рн ккт   000"
    private String znkkt; // заводской номер ккт - "зн ккт   000"
    private String sno; // система налогообложения - "усн доход"
    private double nds; // НДС: "НДС 20%    4.00"


    public String readJsonFromDirectory(String jsonDirectory) {
        String jsonString = "null";
        try {
            FileInputStream fileInputStream = new FileInputStream(jsonDirectory);
            jsonString = new String(fileInputStream.readAllBytes());
            fileInputStream.close();
        } catch (IOException e) {
            log.error("CashReceiptBuilder readJson exc: " + e.getMessage());
        }
        return jsonString;
    }


    public void setDataFromJson(String jsonData) {
        String[] splitJsonString = jsonData.replace("{", "").replace("}", "").replaceAll("\"", "").split(",");
        companyTitle = splitJsonString[0].replace("company_title: ", "");
        companyAttribute = splitJsonString[1].replace("company_attribute: ", "");
        documentTitle = splitJsonString[2].replace("document_title: ", "");
        invoiceSort = splitJsonString[3].replace("invoice_sort: ", "");
        payment = splitJsonString[4].replace("seller: ", "");
        seller = splitJsonString[5].replace("payment: ", "");
        fnsSite = splitJsonString[6].replace("fns_site: ", "");
        ofd = splitJsonString[7].replace("ofd: ", "");
        rnkkt = splitJsonString[8].replace("rnkkt: ", "");
        znkkt = splitJsonString[9].replace("znkkt: ", "");
        sno = splitJsonString[10].replace("sno: ", "");
        anyText = splitJsonString[11].replace("any_text: ", "");
        nds = Double.parseDouble(splitJsonString[12].replace("nds: ", ""));
    }


    public CashReceipt buildCashReceipt(String jsonDirectoryPath, String itemInfo, int cashReceiptNumber, String fn, String fd, String fp, String checkLink) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy   HH:mm");
        LocalDateTime localDateTime = LocalDateTime.now();
        String jsonData = readJsonFromDirectory(jsonDirectoryPath);
        setDataFromJson(jsonData);
        CashReceipt cashReceipt = new CashReceipt();

        cashReceipt.setCompanyTitle(companyTitle); // "ИП Джулия кофе" - наименование фирмы "ИП Джулия кофе"
        cashReceipt.setCompanyAttribute(companyAttribute); // адрес, по которому зарегистрирована касса/кошелёк
        cashReceipt.setDocumentTitle(documentTitle); // название документа: "Кассовый чек"
        cashReceipt.setInvoiceSort(invoiceSort); // признак расчёта "приход"
        cashReceipt.setPayment(payment); // вид оплаты - "электронными"
        cashReceipt.setSeller(seller); // кассир - фио продавца - "Сбер касса"
        cashReceipt.setFnsSite(fnsSite); //  сайт ФНС - "сайт ФНС www.nalog.ru"
        cashReceipt.setOfd(ofd); //  оператор фискальных данных - ""
        cashReceipt.setRnkkt(rnkkt); // регистрационный номер ккт - "рн ккт   000"
        cashReceipt.setZnkkt(znkkt); // заводской номер ккт - "зн ккт   000"
        cashReceipt.setSno(sno); // система налогообложения - "усн доход"
        cashReceipt.setAnyText(anyText); // любой сопроводительный текст - "Спасибо за покупку!"
        cashReceipt.setNds(nds); // НДС: "НДС 20%    4.00"
        cashReceipt.setFn(fn); // заводской номер фискального документа - "фн   000"
        cashReceipt.setFd(fd); //  // порядковый номер фискального документа - "фд   000"
        cashReceipt.setFp(fp); // фискальный признак документа - "фп   000"
        cashReceipt.setItemInfo(itemInfo); // список товаров из корзины
        cashReceipt.setCheckLink(checkLink); // ссылка для проверки чека
        cashReceipt.setDateTime(dateTimeFormatter.format(localDateTime)); // дата и время покупки
        cashReceipt.setCashReceiptNumber(cashReceiptNumber); //  порядковый номер чека за смену - "чек №  000"
        // cashReceipt.setWorkNumber(workNumber); номер смены

        return cashReceipt;
    }


}
