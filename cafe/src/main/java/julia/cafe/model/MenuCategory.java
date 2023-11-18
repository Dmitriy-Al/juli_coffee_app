package julia.cafe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;

//@Slf4j
@lombok.Setter
@lombok.Getter
@Entity(name = "category_table")
public class MenuCategory implements Comparable<MenuCategory>{

    public MenuCategory(){}

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int categoryId;
    private String category;
    private String pictureLinc;

    @Override
    public int compareTo(MenuCategory menuCategory){
        return this.category.length() - menuCategory.category.length();
    }


}
