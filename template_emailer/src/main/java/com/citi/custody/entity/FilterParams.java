package com.citi.custody.entity;

public class FilterParams {
    private int page;
    private int size;
    private String name;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "FilterParams{" +
                "page=" + page +
                ", size=" + size +
                ", name='" + name + '\'' +
                '}';
    }
}
