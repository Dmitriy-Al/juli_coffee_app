package julia.cafe.cashreceipt;

@lombok.Setter
@lombok.Getter
public class CashReceipt {

    private String companyTitle; // "ИП Джулия кофе" - наименование фирмы "ИП Джулия кофе"
    private String companyAttribute; // адрес, по которому зарегистрирована касса/кошелёк
    private String documentTitle; // название документа: "Кассовый чек"
    private String invoiceSort; // признак расчёта "приход"
    private double nds; // НДС: "НДС 20%    4.00"
    private String itemInfo; // товары - цены: "х    24"
    private String payment; // вид оплаты - "электронными"
    private String seller; // кассир - фио продавца - "Сбер касса"
    private String fnsSite; //  сайт ФНС - "сайт ФНС www.nalog.ru"
    private String ofd; //  оператор фискальных данных - ""
    private String rnkkt; // регистрационный номер ккт - "рн ккт   000"
    private String znkkt; // заводской номер ккт - "зн ккт   000"
    private String sno; // система налогообложения - "усн доход"
    private String anyText; // любой сопроводительный текст - "Спасибо за покупку!"

    private String fn; // заводской номер фискального документа - "фн   000"
    private String fd; //  // порядковый номер фискального документа - "фд   000"
    private String fp; // фискальный признак документа - "фп   000"
    private String checkLink; // ссылка для проверки чека

    private int workNumber; // номер смены
    private int cashReceiptNumber; //  порядковый номер чека за смену - "чек №  000"
    private String dateTime;//  дата и время - "11.09.2002   14:00"


    public CashReceipt(){}


    public CashReceipt(String itemInfo, String companyTitle, String companyAttribute, String invoiceSort, double nds, String payment, String seller, String fnsSite, String ofd,
                       String rnkkt, String znkkt, String sno, String fn, String fd, String fp, int workNumber, int cashReceiptNumber, String checkLink, String anyText) {
        this.companyTitle = companyTitle;
        this.companyAttribute = companyAttribute;
        this.invoiceSort = invoiceSort;
        this.nds = nds;
        this.itemInfo = itemInfo;
        this.payment = payment;
        this.seller = seller;
        this.fnsSite = fnsSite;
        this.ofd = ofd;
        this.rnkkt = rnkkt;
        this.znkkt = znkkt;
        this.sno = sno;
        this.fn = fn;
        this.fd = fd;
        this.fp = fp;
        this.workNumber = workNumber;
        this.cashReceiptNumber = cashReceiptNumber;
        this.checkLink = checkLink;
        this.anyText = anyText;
    }


    public String getCashReceipt() {
        int summa = 0;
        String[] itemData = itemInfo.split("#");
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : itemData) {
            String[] temp = s.split("-");
            stringBuilder.append("\n").append(temp[0]).append(" ").append("   = ").append(temp[1]);
            summa += Integer.parseInt(temp[1]);
        }return companyTitle +
                "\n" + companyAttribute +
                "\n" + documentTitle +
                "\n" + invoiceSort +
                "\n" + stringBuilder +
                "\n" + "итого   " + summa +
                "\n" + nds + "%  =  " + (summa * (nds * 0.01)) +
                "\n" + payment +
                "\n" + seller +
                "\n" + fnsSite +
                "\n" + ofd +
                "\n" + rnkkt +
                "\n" + znkkt +
                "\n" + sno +
                "\n" + fn +
                "\n" + fd +
                "\n" + fp +
                "\n" + "смена № " + workNumber +
                "\n" + "чек № " + cashReceiptNumber +
                "\n" + dateTime +
                "\n" + checkLink +
                "\n" + anyText;
    }


    public String getCashReceiptText() {
        int summa = 0;
        String[] itemData = itemInfo.split("#");
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : itemData) {
            String[] temp = s.split("-");
            stringBuilder.append("#").append(temp[0]).append(" ").append("   = ").append(temp[1]);
            summa += Integer.parseInt(temp[1]);
        }
        return companyTitle +
                "#" + companyAttribute +
                "#" + documentTitle +
                "#" + invoiceSort +
                stringBuilder +
                "#" + "итого  = " + summa +
                "#" + nds + "%  = " + (summa * (nds * 0.01)) +
                "#" + payment +
                "#" + seller +
                "#" + fnsSite +
                "#" + ofd +
                "#" + rnkkt +
                "#" + znkkt +
                "#" + sno +
                "#" + fn +
                "#" + fd +
                "#" + fp +
                "#" + "чек N " + cashReceiptNumber +
                "#" + dateTime +
                "#" + checkLink +
                "#" + anyText;
    }


}
