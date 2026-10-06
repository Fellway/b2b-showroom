package pl.wizytowka.linesheet.adapter.in.web;

import pl.wizytowka.shared.application.PageResult;

import java.util.List;

record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    static <T> PageResponse<T> of(PageResult<T> result) {
        return new PageResponse<>(result.items(), result.page(), result.size(), result.totalItems(), result.totalPages());
    }
}
