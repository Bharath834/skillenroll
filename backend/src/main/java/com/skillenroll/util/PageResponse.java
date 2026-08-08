package com.skillenroll.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Pagination metadata wrapper returned inside {@link ApiResponse#getData()}
 * for list endpoints. Keeps the existing envelope intact while giving
 * clients everything they need to page through results.
 *
 * @param <T> the element type of {@link #getContent()}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Builds a {@code PageResponse} from a Spring Data {@link Page},
     * mapping each entity to its response DTO.
     *
     * @param source the Spring Data page (already translated/sorted)
     * @param mapper entity-to-DTO mapper
     * @param <S>    the entity/source type
     * @param <R>    the response DTO type
     * @return a page response whose content is the mapped DTOs
     */
    public static <S, R> PageResponse<R> from(Page<S> source, Function<S, R> mapper) {
        List<R> content = source.getContent().stream().map(mapper).toList();
        return PageResponse.<R>builder()
                .content(content)
                .page(source.getNumber())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .first(source.isFirst())
                .last(source.isLast())
                .hasNext(source.hasNext())
                .hasPrevious(source.hasPrevious())
                .build();
    }
}
