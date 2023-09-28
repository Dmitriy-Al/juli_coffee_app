package julia.cafe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@lombok.Setter
@lombok.Getter
@Entity(name = "productTable")
public class Product implements Comparable<Product> {

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




    @Override
    public boolean equals(Object obj) {
        return obj instanceof Product && this.productTitle.equals(((Product) obj).productTitle);
    }

/*

    @Override
    public int compareTo(Product product) {
        if (this.getProductCategory().length() < product.getProductCategory().length()) {
            return 1;
        } else if (this.getProductCategory().length() > product.getProductCategory().length()) {
            return -1;
        }
        return this.getMenuCategory().length() - product.getMenuCategory().length();
    }


    @Override
    public int compareTo(Product product) {
       char[] a = this.getProductCategory().toCharArray();
        char[] b = product.getProductCategory().toCharArray();
       return Arrays.hashCode(a) - Arrays.hashCode(b);

    }


 */

    @Override
    public int compareTo(Product product) {
        char[] a = this.getProductCategory().toCharArray();
        char[] b = product.getProductCategory().toCharArray();
        return Arrays.hashCode(a) - Arrays.hashCode(b);
    }




}
