package pl.wizytowka.catalog.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.AddColorRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.ColorOrderRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.ImageColorRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.ImageOrderRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.ProductRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.StatusChangeRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.VariantRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductRequests.VariantUpdateRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductResponses.ProductListItemResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminProductResponses.ProductResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.IdResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.PageResponse;
import pl.wizytowka.catalog.application.port.in.AdminCatalogQueryUseCase;
import pl.wizytowka.catalog.application.port.in.ManageProductImagesUseCase;
import pl.wizytowka.catalog.application.port.in.ManageProductsUseCase;
import pl.wizytowka.catalog.application.view.AdminProductFilter;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ImageId;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Zarządzanie produktami, ich kolorami, wariantami i zdjęciami w panelu administracyjnym. */
@RestController
@RequestMapping("/api/v1/admin/products")
class AdminProductController {

    private final ManageProductsUseCase products;
    private final ManageProductImagesUseCase images;
    private final AdminCatalogQueryUseCase queries;

    AdminProductController(ManageProductsUseCase products, ManageProductImagesUseCase images,
                           AdminCatalogQueryUseCase queries) {
        this.products = products;
        this.images = images;
        this.queries = queries;
    }

    // ------------------------------------------------------------------ produkt

    @GetMapping
    PageResponse<ProductListItemResponse> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) ProductStatus status,
                                               @RequestParam(required = false) UUID categoryId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "25") int size) {
        return PageResponse.of(queries.products(new AdminProductFilter(q, status, categoryId), new PageQuery(page, size)),
                ProductListItemResponse::of);
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.of(queries.product(ProductId.of(id)));
    }

    @PostMapping
    ResponseEntity<IdResponse> create(@Valid @RequestBody ProductRequest request) {
        return Created.at(products.create(request.toDetails()).value());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        products.update(ProductId.of(id), request.requiredVersion(), request.toDetails());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        products.delete(ProductId.of(id));
    }

    @PostMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChangeRequest request) {
        products.changeStatus(ProductId.of(id), request.version(), request.transition());
    }

    // ------------------------------------------------------------------ kolory

    @PostMapping("/{id}/colors")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addColor(@PathVariable UUID id, @Valid @RequestBody AddColorRequest request) {
        products.addColor(ProductId.of(id), ColorId.of(request.colorId()));
    }

    @DeleteMapping("/{id}/colors/{colorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeColor(@PathVariable UUID id, @PathVariable UUID colorId) {
        products.removeColor(ProductId.of(id), ColorId.of(colorId));
    }

    @PutMapping("/{id}/colors/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reorderColors(@PathVariable UUID id, @Valid @RequestBody ColorOrderRequest request) {
        products.reorderColors(ProductId.of(id), request.colorIds().stream().map(ColorId::of).toList());
    }

    // ------------------------------------------------------------------ warianty

    @PostMapping("/{id}/variants")
    ResponseEntity<IdResponse> addVariant(@PathVariable UUID id, @Valid @RequestBody VariantRequest request) {
        VariantId variantId = products.addVariant(ProductId.of(id), ColorId.of(request.colorId()),
                SizeId.of(request.sizeId()), Sku.of(request.sku()), Gtin.of(request.ean()));
        return Created.at(variantId.value());
    }

    @PutMapping("/{id}/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateVariant(@PathVariable UUID id, @PathVariable UUID variantId,
                       @Valid @RequestBody VariantUpdateRequest request) {
        products.updateVariant(ProductId.of(id), VariantId.of(variantId), Sku.of(request.sku()),
                Gtin.of(request.ean()), request.active());
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeVariant(@PathVariable UUID id, @PathVariable UUID variantId) {
        products.removeVariant(ProductId.of(id), VariantId.of(variantId));
    }

    // ------------------------------------------------------------------ zdjęcia

    /**
     * Wgranie zdjęcia. Format rozpoznaje warstwa aplikacji po zawartości pliku — deklarowany
     * Content-Type i nazwa pliku od klienta są ignorowane.
     */
    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> uploadImage(@PathVariable UUID id,
                                  @RequestPart("file") MultipartFile file,
                                  @RequestParam(required = false) UUID colorId) {
        if (file.isEmpty()) {
            throw new InvalidValueException("catalog.image.empty", "Uploaded file is empty");
        }
        if (file.getSize() > ManageProductImagesUseCase.MAX_IMAGE_BYTES) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image exceeds " + ManageProductImagesUseCase.MAX_IMAGE_BYTES + " bytes");
            problem.setProperty("code", "request.too-large");
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
        }
        ImageId imageId = images.upload(ProductId.of(id), colorId == null ? null : ColorId.of(colorId), bytesOf(file));
        return Created.at(imageId.value());
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        images.remove(ProductId.of(id), ImageId.of(imageId));
    }

    @PutMapping("/{id}/images/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reorderImages(@PathVariable UUID id, @Valid @RequestBody ImageOrderRequest request) {
        images.reorder(ProductId.of(id), request.imageIds().stream().map(ImageId::of).toList());
    }

    @PutMapping("/{id}/images/{imageId}/color")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void assignImageColor(@PathVariable UUID id, @PathVariable UUID imageId,
                          @RequestBody ImageColorRequest request) {
        images.assignColor(ProductId.of(id), ImageId.of(imageId),
                request.colorId() == null ? null : ColorId.of(request.colorId()));
    }

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read uploaded file", e);
        }
    }
}
