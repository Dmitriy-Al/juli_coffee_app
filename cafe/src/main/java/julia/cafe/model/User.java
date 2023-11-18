package julia.cafe.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.Objects;

@Slf4j
@lombok.Setter
@lombok.Getter
@Entity(name = "user_table")
public class User implements Cloneable, Comparable<User> {

    @Id
    private long chatId;
    private String firstname;
    private String userName;
    private Timestamp registeredDate;
    private boolean agreeGetMessage;
    @Column(columnDefinition = "varchar(10000000)")
    private String purchase;

    public User(){};

    public User(long chatId, String userName, boolean agreeGetMessage, Timestamp registeredDate, String firstname, String purchase){
        this.chatId = chatId;
        this.firstname = firstname;
        this.userName = userName;
        this.registeredDate = registeredDate;
        this.agreeGetMessage = agreeGetMessage;
        this.purchase = purchase;
    }


    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("User clone exc: " + e.getMessage());
        }
        return null;
    }


    public int hashCode(){
        return Objects.hash(chatId, firstname, userName, registeredDate, agreeGetMessage, purchase);
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


    @Override
    public String toString() {
        char pointer = 10033;
        return pointer + " userName: "+ userName + "\nимя: " + firstname + "\nдата регистрации : " + registeredDate +
                "\nid: " + chatId + "\nпокупки: " + purchase.replaceAll("purchase", "\n") + "\n";
    }


    public String getUserData() {
        return " @username: "+ userName + "\nимя: " + firstname + "\nпервое посещение : " + registeredDate + "\nid: " + chatId;
    }

}
