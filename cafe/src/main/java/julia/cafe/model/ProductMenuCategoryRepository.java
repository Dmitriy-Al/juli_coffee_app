package julia.cafe.model;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProductMenuCategoryRepository extends CrudRepository<MenuProductCategory, Integer> {
    Optional<MenuProductCategory> findByCategoryLikeIgnoreCase(String category);

}
