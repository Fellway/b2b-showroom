package pl.wizytowka.catalog.domain;

/**
 * Cykl życia produktu w katalogu.
 * <ul>
 *   <li>DRAFT – w przygotowaniu, niewidoczny publicznie</li>
 *   <li>PUBLISHED – widoczny w katalogu i dostępny do zestawień</li>
 *   <li>HIDDEN – czasowo ukryty (np. wyprzedany sezon), łatwy do przywrócenia</li>
 *   <li>ARCHIVED – wycofany z oferty, zachowany na potrzeby historii</li>
 * </ul>
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    HIDDEN,
    ARCHIVED
}
