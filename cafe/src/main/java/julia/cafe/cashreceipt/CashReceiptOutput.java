package julia.cafe.cashreceipt;
import julia.cafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class CashReceiptOutput {

    private String path;

    private final Config config = new Config();

    private final DateTimeFormatter dateTDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH.mm.ss");

    public String getCashReceiptSaveDirectory() {
        return path;
    }

    public void createApachePDF(long id, String directory, String cashReceiptData) {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        String[] splitCashReceiptData = cashReceiptData.split("#"); // электронный платёж# - недопустимый символ Ё

        try {
            PDPageContentStream stream = new PDPageContentStream(document, page);
            PDFont font = PDType0Font.load(document, new File("cafe/src/main/resources/font/helvetica.ttf")); // TODO  "config.fontDirectoryPath"
            stream.setNonStrokingColor(Color.BLUE);
            int marginTop = 760;
            int marginLeft = 30;
            for (String splitCashReceiptDatum : splitCashReceiptData) {
                marginTop -= 15;
                stream.beginText();
                stream.setFont(font, 10);
                stream.newLineAtOffset(marginLeft, marginTop);
                stream.showText(splitCashReceiptDatum);
                stream.endText();
            }
            stream.close();
            path = directory + id + "_" + dateTDateTimeFormatter.format(LocalDateTime.now()) + ".pdf";
            document.save(path);
            document.close();
        } catch (IOException e) {
            log.error("createApachePDF exc: " + e.getMessage());
        }
    }

}
