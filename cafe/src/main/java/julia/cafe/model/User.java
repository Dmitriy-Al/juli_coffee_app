package julia.cafe.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@lombok.Setter
@lombok.Getter
@Entity(name = "userTable")
public class User implements Cloneable, Comparable<User> { //


    @Id
    private long chatId;
    private String firstname;
    private String lastname;
    private String patronymic;
    private String userName;
    private String birthday;
    private Timestamp registeredDate;
    private boolean agreeGetMessage;
    @Column(columnDefinition = "varchar(10000000)")
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
            // TODO
        }
        return null;
    }


    @Override
    public String toString() {
        char pointer = 10033;
        return pointer + " userName: "+ userName + "\nимя: " + firstname + "\nдата рождения: " + birthday +
                "\nдата регистрации : " + registeredDate + "\nid: " + chatId + "\nпокупки: " + purchase.replaceAll("purchase", "\n") + "\n";
    }

    @Override
    public int compareTo(User user){
        if(registeredDate.before(user.registeredDate)) {
            return - 1;
        } else if (registeredDate.after(user.registeredDate)){
            return 1;
        }
        return 0;
    }

    public String[] getUserData() {
        String[] data = new String[5];
        data[0] = userName;
        data[1] = firstname;
        data[2] = birthday;
        data[3] = registeredDate.toString();
        data[4] = purchase;
        return data;
    }

}
