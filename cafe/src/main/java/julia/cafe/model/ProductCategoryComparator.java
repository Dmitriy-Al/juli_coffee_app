package julia.cafe.model;

import java.util.Comparator;

public class ProductCategoryComparator implements Comparator<Product> {
    public int compare(Product f, Product s) {
        return s.getProductCategory().length() - f.getProductCategory().length();
    }
}
