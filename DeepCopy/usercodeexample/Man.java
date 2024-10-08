package usercodeexample;

import java.util.List;

public class Man {

    private final String name;
    private final int age;
    private final List<String> favoriteBooks;

    public Man(String name, int age, List<String> favoriteBooks) {
        this.name = name;
        this.age = age;
        this.favoriteBooks = favoriteBooks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Man man = (Man) o;

        if (age != man.age) return false;
        if (!name.equals(man.name)) return false;
        return favoriteBooks.equals(man.favoriteBooks);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + age;
        result = 31 * result + favoriteBooks.hashCode();
        return result;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public List<String> getFavoriteBooks() {
        return favoriteBooks;
    }
}
