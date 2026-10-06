package pl.wizytowka.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByStyleCode(String styleCode);

    boolean existsByStyleCodeAndIdNot(String styleCode, UUID id);

    boolean existsByCategoryId(UUID categoryId);

    @Query("select count(v) > 0 from ProductVariantJpaEntity v where v.sku = :sku")
    boolean existsVariantBySku(String sku);

    @Query("select count(v) > 0 from ProductVariantJpaEntity v where v.sku = :sku and v.product.id <> :productId")
    boolean existsVariantBySkuOutside(String sku, UUID productId);

    @Query("select count(v) > 0 from ProductVariantJpaEntity v where v.ean = :ean")
    boolean existsVariantByEan(String ean);

    @Query("select count(v) > 0 from ProductVariantJpaEntity v where v.ean = :ean and v.product.id <> :productId")
    boolean existsVariantByEanOutside(String ean, UUID productId);

    @Query("select count(pc) > 0 from ProductColorJpaEntity pc where pc.id.colorId = :colorId")
    boolean existsColorUsage(UUID colorId);

    @Query("select count(v) > 0 from ProductVariantJpaEntity v where v.sizeId = :sizeId")
    boolean existsSizeUsage(UUID sizeId);
}
