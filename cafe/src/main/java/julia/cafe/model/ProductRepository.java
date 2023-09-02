package julia.cafe.model;

import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Integer> {
    Product findByProductCategoryIgnoreCase(String productCategory);
}
