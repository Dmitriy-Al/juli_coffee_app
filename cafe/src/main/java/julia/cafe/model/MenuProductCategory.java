package julia.cafe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@lombok.Setter
@lombok.Getter
@Entity(name = "categoryTable")
public class MenuProductCategory implements Comparable<MenuProductCategory>{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int productId;
    private String category;
    private String pictureLinc;

    @Override
    public int compareTo(MenuProductCategory productCategory){
        return this.category.length() - productCategory.category.length();
    }


}
