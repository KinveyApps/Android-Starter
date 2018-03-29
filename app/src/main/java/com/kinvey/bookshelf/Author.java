package com.kinvey.bookshelf;


import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class Author extends GenericJson{

    @Key
    private String name;

    @Key
    private Integer age;

    public Author() {

    }

    public Author(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
