package com.example.secureapi.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Construye la paginación en el servidor con un orden fijo. No se acepta un
 * parámetro {@code sort} libre para evitar ordenar por propiedades internas o sensibles.
 */
public final class PageRequests {

    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable newestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }
}
