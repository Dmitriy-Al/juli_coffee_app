package julia.cafe.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@lombok.Setter
@lombok.Getter
@Entity(name = "userTable")
public class User {

    @Id
    private long chatId;
    private long phoneNumber;
    private boolean isReceiveMessage;
    private Timestamp registeredAt;
    private String firstname;
    private String lastname;
    private String patronymic;
    private String userName;
    private String birthday;
    private String orders;

    @Override
    public String toString() {
        char pointer = 10033;
        return pointer + " " +lastname + " " + firstname + " " + patronymic + "\nтелефон: " + phoneNumber + "\nзарегистрирован: " + registeredAt +
                ";   id: " + chatId + ";   userName: " + userName;
    }

}
