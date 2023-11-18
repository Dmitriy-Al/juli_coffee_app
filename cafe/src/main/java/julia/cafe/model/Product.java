package julia.cafe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;

@Slf4j
@lombok.Setter
@lombok.Getter
@Entity(name = "product_table")
public class Product implements Comparable<Product>, Cloneable {

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
    private int productDiscount;


    public int hashCode(){
        return Objects.hash(
                productId,
                menuCategory,
                productCategory,
                productTitle,
                productSize,
                productPrice
                );
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof Product && this.productTitle.equals(((Product) obj).productTitle);
    }


    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Product clone exc: " + e.getMessage());
        }
        return null;
    }


    @Override
    public int compareTo(Product product) {
        char[] a = this.getProductCategory().toCharArray();
        char[] b = product.getProductCategory().toCharArray();
        return Arrays.hashCode(a) - Arrays.hashCode(b);
    }


}
