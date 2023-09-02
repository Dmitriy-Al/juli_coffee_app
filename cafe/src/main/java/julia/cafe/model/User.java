package julia.cafe.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@lombok.Setter
@lombok.Getter
@Entity(name = "userTable")
public class User implements Cloneable {


    @Id
    private long chatId;
    private String firstname;
    private String lastname;
    private String patronymic;
    private String userName;
    private String birthday;
    private Timestamp registeredDate;
    private boolean agreeGetMessage;
    private String purchase;

    public User(){};

    public User(long chatId, String userName, boolean agreeGetMessage, String firstname, String lastname, String patronymic, String birthday, Timestamp registeredDate, String purchase){
        this.chatId = chatId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.patronymic = patronymic;
        this.userName = userName;
        this.birthday = birthday;
        this.registeredDate = registeredDate;
        this.agreeGetMessage = agreeGetMessage;
        this.purchase = purchase;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {

        }
        return null;
    }


    @Override
    public String toString() {
        char pointer = 10033;
        return pointer + " " + lastname + " " + firstname + " " + patronymic + "\nтелефон: " + "\nзарегистрирован: " +
                ";   id: " + chatId + ";   userName: " + userName;
    }

}
