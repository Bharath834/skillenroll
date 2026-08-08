package com.skillenroll.courseservice.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guards controller-provided {@link Pageable} instances before they reach the
 * repository layer: caps the page size and validates sort properties against
 * an explicit whitelist so an unknown sort field yields a clear 400 instead of
 * an opaque 500.
 */
public final class PaginationUtils {

    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    /**
     * Normalizes a {@link Pageable}: enforces the maximum page size and maps
     * friendly sort aliases (e.g. {@code userId} -> {@code user.id}) onto the
     * entity's real property paths, rejecting anything not in
     * {@code allowedProperties} with an {@link IllegalArgumentException}.
     *
     * @param pageable         the raw pageable from the controller
     * @param allowedProperties sort properties allowed for this resource
     * @param aliases          optional alias -> entity property map
     * @return a safe pageable (or the original when unsorted)
     * @throws IllegalArgumentException if the page size is too large or a sort
     *                                  field is not allowed
     */
    public static Pageable normalize(Pageable pageable, Set<String> allowedProperties, Map<String, String> aliases) {
        // Spring's Pageable resolver already clamps page/size to safe bounds;
        // only the explicit upper cap on page size needs enforcing here.
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
        }
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            String mapped = aliases.getOrDefault(property, property);
            if (!allowedProperties.contains(mapped)) {
                throw new IllegalArgumentException(
                        "Invalid sort field '" + property + "'. Allowed sort fields: " + allowedProperties);
            }
            orders.add(order.withProperty(mapped));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }
}
