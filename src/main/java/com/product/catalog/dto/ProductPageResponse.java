package com.product.catalog.dto;

import java.util.List;
import java.util.Objects;

/**
 * DTO for paginated product response
 */
public class ProductPageResponse {

    private List<ProductResponse> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean last;

    public ProductPageResponse() {}

    public ProductPageResponse(List<ProductResponse> content, Integer page, Integer size, Long totalElements, Integer totalPages, Boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    public List<ProductResponse> getContent() { return content; }
    public void setContent(List<ProductResponse> content) { this.content = content; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public Long getTotalElements() { return totalElements; }
    public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    public Boolean getLast() { return last; }
    public void setLast(Boolean last) { this.last = last; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductPageResponse that = (ProductPageResponse) o;
        return Objects.equals(content, that.content) && Objects.equals(page, that.page) && Objects.equals(size, that.size) && Objects.equals(totalElements, that.totalElements) && Objects.equals(totalPages, that.totalPages) && Objects.equals(last, that.last);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, page, size, totalElements, totalPages, last);
    }
}
