package julia.cafe.model;

import java.util.Comparator;

public class ProductComparator implements Comparator<Product> {

    public int compare(Product f, Product s) {
        if(s.getProductCategory().length() < f.getProductCategory().length()){
            return 1;
        } else if (s.getProductCategory().length() > f.getProductCategory().length()){
            return -1;
        }
        return s.getMenuCategory().length() - f.getMenuCategory().length();
    }

}
