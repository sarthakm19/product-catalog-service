package com.product.catalog.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    // Self-referencing parent-child relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> childCategories = new ArrayList<>();

    // Many-to-many relationship for subcategories (using the table we created in Liquibase)
    @ManyToMany
    @JoinTable(
        name = "category_subcategories",
        joinColumns = @JoinColumn(name = "parent_category_id"),
        inverseJoinColumns = @JoinColumn(name = "subcategory_id")
    )
    private List<Category> subcategories = new ArrayList<>();

    @ManyToMany(mappedBy = "subcategories")
    private List<Category> parentCategories = new ArrayList<>();

    // One-to-many relationship with products
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    // Default constructor
    public Category() {
    }

    // Constructor with parameters
    public Category(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }


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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(Category parentCategory) {
        this.parentCategory = parentCategory;
    }

    public List<Category> getChildCategories() {
        return childCategories;
    }

    public void setChildCategories(List<Category> childCategories) {
        this.childCategories = childCategories;
    }

    public List<Category> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<Category> subcategories) {
        this.subcategories = subcategories;
    }

    public List<Category> getParentCategories() {
        return parentCategories;
    }

    public void setParentCategories(List<Category> parentCategories) {
        this.parentCategories = parentCategories;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    // Helper methods for managing relationships
    public void addChildCategory(Category childCategory) {
        childCategories.add(childCategory);
        childCategory.setParentCategory(this);
    }

    public void removeChildCategory(Category childCategory) {
        childCategories.remove(childCategory);
        childCategory.setParentCategory(null);
    }

    public void addSubcategory(Category subcategory) {
        subcategories.add(subcategory);
        subcategory.getParentCategories().add(this);
    }

    public void removeSubcategory(Category subcategory) {
        subcategories.remove(subcategory);
        subcategory.getParentCategories().remove(this);
    }

    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setCategory(null);
    }

    @Override
    public String toString() {
        return "Category{" +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return code != null ? code.equals(category.code) : category.code == null;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}
