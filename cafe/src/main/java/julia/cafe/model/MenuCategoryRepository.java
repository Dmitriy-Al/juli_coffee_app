package julia.cafe.model;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface MenuCategoryRepository extends CrudRepository<MenuCategory, Integer> {
    Optional<MenuCategory> findByCategoryLikeIgnoreCase(String category);

}
