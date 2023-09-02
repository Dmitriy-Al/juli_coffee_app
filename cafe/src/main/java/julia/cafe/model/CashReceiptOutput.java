package julia.cafe.model;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CashReceiptOutput {


    private String path;

    private final DateTimeFormatter dateTDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH.mm.ss");


    public String getDirectoryPath() {
        return path;
    }

    public void createTXT(long id, String directory, String cashReceiptData) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(path + ".txt")) {
            fileOutputStream.write(cashReceiptData.getBytes());
            fileOutputStream.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    public void createApachePDF(long id, String directory, String cashReceiptData) {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        String[] splitCashReceiptData = cashReceiptData.split("#");

        try {
            PDPageContentStream stream = new PDPageContentStream(document, page);
            PDFont font = PDType0Font.load(document, new File("cafe/src/main/resources/font/helvetica.ttf"));
            stream.setNonStrokingColor(Color.BLUE);
            int marginTop = 760;
            int marginLeft = 30;
            for (int i = 0; i < splitCashReceiptData.length; i++) {
                marginTop -= 15;
                stream.beginText();
                stream.setFont(font, 10);
                stream.newLineAtOffset(marginLeft, marginTop);
                stream.showText(splitCashReceiptData[i]);
                stream.endText();
            }
            stream.close();
            path = directory + "\\" + id + "_" + dateTDateTimeFormatter.format(LocalDateTime.now()) + ".pdf";
            document.save(path);
            document.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

}
