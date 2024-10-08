package usercodeexample;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ManExtended extends Man {

    private LocalDate dateOfBirth;
    private List<Man> favoriteAuthors;
    private Set<ManExtended> relatives;
    private Set<ManExtended> relativesByAge;
    private Map<ManExtended, String> relativesMemberNames;

    private ManExtended[] referrals;
    private double[][] bookRatings;
    private BigDecimal averageOrderValue;

    public ManExtended(String name, int age, List<String> favoriteBooks) {
        super(name, age, favoriteBooks);
        if (age < 1) {
            throw new RuntimeException("Invalid age"); // Exists just for demo
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ManExtended manExtended = (ManExtended) o;

        return Objects.equals(dateOfBirth, manExtended.dateOfBirth);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        return result;
    }

    public List<Man> getFavoriteAuthors() {
        return favoriteAuthors;
    }

    public void setFavoriteAuthors(List<Man> favoriteAuthors) {
        this.favoriteAuthors = favoriteAuthors;
    }

    public ManExtended[] getReferrals() {
        return referrals;
    }

    public void setReferrals(ManExtended[] referrals) {
        this.referrals = referrals;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Set<ManExtended> getRelatives() {
        return relatives;
    }

    public void setRelatives(Set<ManExtended> relatives) {
        this.relatives = relatives;
    }

    public Set<ManExtended> getRelativesByAge() {
        return relativesByAge;
    }

    public void setRelativesByAge(Set<ManExtended> relativesByAge) {
        this.relativesByAge = relativesByAge;
    }

    public Map<ManExtended, String> getRelativesMemberNames() {
        return relativesMemberNames;
    }

    public void setRelativesMemberNames(Map<ManExtended, String> relativesMemberNames) {
        this.relativesMemberNames = relativesMemberNames;
    }

    public double[][] getBookRatings() {
        return bookRatings;
    }

    public void setBookRatings(double[][] bookRatings) {
        this.bookRatings = bookRatings;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }
}
