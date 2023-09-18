package julia.cafe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@lombok.Setter
@lombok.Getter
@Entity(name = "productTable")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int productId;
    private String menuCategory;
    private String productCategory;
    private String productTitle;
    private String productDescription;
    private String productPhotoLinc;
    private String productSize;
    private String productPrice;

    public String getProductInfo() {
        return  productDescription;
    }

}
