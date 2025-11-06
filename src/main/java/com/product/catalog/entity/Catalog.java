package com.product.catalog.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "catalogs")
public class Catalog {

    @Id
    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_version", nullable = false)
    private CatalogVersion catalogVersion;

    @OneToMany(mappedBy = "catalog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    // Default constructor
    public Catalog() {
    }

    // Constructor with parameters
    public Catalog(String code, String name, CatalogVersion catalogVersion) {
        this.code = code;
        this.name = name;
        this.catalogVersion = catalogVersion;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CatalogVersion getCatalogVersion() {
        return catalogVersion;
    }

    public void setCatalogVersion(CatalogVersion catalogVersion) {
        this.catalogVersion = catalogVersion;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    // Helper methods for managing bidirectional relationship
    public void addProduct(Product product) {
        products.add(product);
        product.setCatalog(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setCatalog(null);
    }

    @Override
    public String toString() {
        return "Catalog{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", catalogVersion=" + catalogVersion +
                ", productsCount=" + (products != null ? products.size() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Catalog)) return false;
        Catalog catalog = (Catalog) o;
        return code != null ? code.equals(catalog.code) : catalog.code == null;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}
