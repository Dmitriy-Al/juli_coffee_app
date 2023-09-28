package julia.cafe.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Integer> {
    List<Product> findByProductTitleIgnoreCase(String productTitle);
    List<Product> findByProductCategory(String productCategory);

}
