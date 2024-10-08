import deepcopy.DeepCopyModule;
import usercodeexample.ArraysArrayListCopyPlugin;
import usercodeexample.Man;
import usercodeexample.ManExtended;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Sometimes you want to make a complete copy of some object.
 * <p>
 * Something like this:
 * <p>
 * ComplexObject obj = ...
 * ComplexObject copy = CopyUtils.deepCopy(obj);
 * <p>
 * The problem is that classes in Java can be of arbitrary complexity - the number of class fields and their types are not regulated in any way. Moreover, the type system in Java is closed - elements of an array/list can be absolutely any data types, including arrays and lists. And also there are recursive data structures - when an object somewhere in its depths contains a reference to itself (or to a part of itself).
 * <p>
 * You need to write a deepCopy() method that takes all these nuances into account and works on objects of arbitrary structure and size.
 * <p>
 * Some details:
 * <p>
 * If you have any questions, feel free to write to join-ecom@lightspeedhq.com.
 * First of all, the method should work correctly. Speed is also important, but not as much as correctness
 * You can only use the features of the standard J2SE library
 * Code should be written in Java (version 21 and above) or Kotlin.
 * The assignment must have a working main() method, to demonstrate how it works
 * The completed assignment should be posted on GitHub
 * <p>
 * P.S. I know about hacks with java.io.Serializable and java.lang.Cloneable, please don't use them
 * <p>
 * <p>
 * <p>
 * Ideas and reasoning of solution.
 * In old kind Java world this task looked simpler, because modules didn't restrict reflection access to jdk internal classes.
 * It could be just recursive loop through object fields using reflection but won't be. And that's why I find this task
 * challenging and interesting, because it can't be solved in ideal way within normal use and without tricks of special JVM run argument.
 * But we still can build useful tool in practice.
 * First of all, the main goal of such solution I see is copying DTO. Objects that represents some data processed in application.
 * Then. Let's have a look on issues that I see and going to manage in code:
 * - Recursive structures don't allow to make just one method with recursive calls anyway. And it's totally real situations that
 * can't be ignored. JPA/Hibernate mapping as example, and Lombok is good illustration that produces quite infamous StackOverflowError problem with it.
 * So the algorithm must have at least two stages. First stage is to recursive instantiate all reference type copies to some plain structure,
 * let's call this structure copy context. And second stage is to go through this structure and set reference fields with instantiated objects.
 * - We don't really need to copy objects of some classes, especially such as String and number wrappers. JDK contains more of them.
 * And we can "copy" this objects just by reference value.
 * - JDK Collection and Map implementations are widely used but still closed to access inside with reflection. So we need to consider
 * copying this structures using its API. Additionally, some of these structures depend on the objects that are put in, like HashSet consider
 * hashСode() and equals() of object, and TreeSet consider its compare() or Comparator. So we can put objects inside through API only when we
 * set all fields inside objects. This means that it is going to be the third stage.
 * - For some reason some people create classes with possibility of RuntimeException throwing from constructor or initialization block.
 * We can't predict this, but can keep extension point to add some custom implementation to algorithm.
 * - Actually we can't predict much more of use cases of JDK classes and specific user views of their code.
 * So I'm going to create solution that is suitable in most of common cases, and create extension points for custom implementation of processing other cases.
 *
 * And as I see, it's better not to make algorithm as static utility class but something with instance.
 * This approach will allow the use of different settings in different contexts or use it as configurable Spring Bean for example.
 */
public class DeepCloneMain {

    public static void main(String[] args) throws Exception {
        List<String> favoriteBooks = new ArrayList<>();
        Stream.of("The Lord of the Rings", "Alice's Adventures in Wonderland", "The Hobbit").forEach(favoriteBooks::add);

        ManExtended client = new ManExtended("Dave", 22, favoriteBooks);

        client.setDateOfBirth(LocalDate.of(2002, 5, 6));
        client.setFavoriteAuthors(Arrays.asList(
                new Man("J. R. R. Tolkien", 81, Collections.emptyList()),
                new Man("Lewis Carroll", 65, Collections.emptyList())
        ));

        ManExtended clientsBrother = new ManExtended("Bob", 20, Arrays.asList("Harry Potter"));
        List<String> sisterFavoriteBooks = new LinkedList<>();
        sisterFavoriteBooks.add("Teach Yourself C++ in 21 Days");
        ManExtended clientsSister = new ManExtended("Kate", 25, sisterFavoriteBooks);

        client.setRelatives(new HashSet<>());
        clientsBrother.setRelatives(new HashSet<>());
        clientsSister.setRelatives(new HashSet<>());

        client.getRelatives().addAll(Arrays.asList(clientsBrother, clientsSister, client));
        clientsBrother.getRelatives().addAll(Arrays.asList(clientsBrother, clientsSister, client));
        clientsSister.getRelatives().addAll(Arrays.asList(clientsBrother, clientsSister, client));

        client.setRelativesByAge(new TreeSet<>(Comparator.comparing(Man::getAge)));
        client.getRelativesByAge().addAll(Arrays.asList(clientsBrother, clientsSister, client));

        client.setRelativesMemberNames(new LinkedHashMap<>());
        client.getRelativesMemberNames().put(clientsBrother, "Brother");
        client.getRelativesMemberNames().put(clientsSister, "Sister");

        ManExtended clientsFriend = new ManExtended("Steve", 23, Arrays.asList("Java for dummies"));
        client.setReferrals(new ManExtended[]{clientsBrother, clientsFriend});

        client.setBookRatings(new double[][]{{4.8, 3.9, 1d}, {2.6, 4.5}, {4.4}});
        client.setAverageOrderValue(new BigDecimal("10.56"));


        var copyModule = DeepCopyModule.builder()
                .registerClassInstanceSupplier(ManExtended.class, () -> new ManExtended("", 1, null)) // Demo of custom instantiation, when exception is thrown from constructor
                .registerWrapperClass(BigDecimal.class) // Already included, just demo of API use
                .registerWrapperClassPredicate(cls -> Collections.emptyList().getClass().equals(cls)) // Demo of alternative definition of classes that are not deep copied
                .registerCopyPlugin(new ArraysArrayListCopyPlugin(), 1) // Demo use of custom copy plugin for Arrays$ArrayList
                .build();

        ManExtended copy = copyModule.deepCopy(client);


        // Method for check usercodeexample.ManExtended and its copy field by field with recursively checking inside data structure
        assertManCopy(client, copy, 2);

        // Additionally check, that other usercodeexample.ManExtended objects nested in different structures of copy are the same as in original client
        ManExtended copyBrother = copy.getRelatives().stream().filter(m -> m.getName().equals(clientsBrother.getName())).findFirst().get();
        ManExtended copySister = copy.getRelatives().stream().filter(m -> m.getName().equals(clientsSister.getName())).findFirst().get();

        assertThat(copy.getRelatives().stream().anyMatch(c -> c == copy), "relatives element object reference");
        assertThat(copy.getRelatives().stream().anyMatch(c -> c == copyBrother), "relatives element object reference");
        assertThat(copy.getRelatives().stream().anyMatch(c -> c == copySister), "relatives element object reference");

        assertThat(copyBrother.getRelatives().stream().anyMatch(c -> c == copy), "relatives element object reference");
        assertThat(copyBrother.getRelatives().stream().anyMatch(c -> c == copyBrother), "relatives element object reference");
        assertThat(copyBrother.getRelatives().stream().anyMatch(c -> c == copySister), "relatives element object reference");

        assertThat(copySister.getRelatives().stream().anyMatch(c -> c == copy), "relatives element object reference");
        assertThat(copySister.getRelatives().stream().anyMatch(c -> c == copyBrother), "relatives element object reference");
        assertThat(copySister.getRelatives().stream().anyMatch(c -> c == copySister), "relatives element object reference");

        assertThat(copy.getRelativesByAge().stream().anyMatch(c -> c == copy), "relativesByAge element object reference");
        assertThat(copy.getRelativesByAge().stream().anyMatch(c -> c == copyBrother), "relativesByAge element object reference");
        assertThat(copy.getRelativesByAge().stream().anyMatch(c -> c == copySister), "relativesByAge element object reference");

        assertThat(copy.getRelativesMemberNames().keySet().stream().anyMatch(c -> c == copyBrother), "relativesMemberNames key object reference");
        assertThat(copy.getRelativesMemberNames().keySet().stream().anyMatch(c -> c == copySister), "relativesMemberNames key object reference");

        assertThat(Arrays.stream(copy.getReferrals()).anyMatch(c -> c == copyBrother), "referrals element object reference");
    }

    private static void assertManCopy(ManExtended man, ManExtended copy, int recursiveLevel) {
        if (recursiveLevel < 1) return;

        assertThat(man != copy, "usercodeexample.Man object reference");
        assertThat(man.getName() == copy.getName(), "name");
        assertThat(man.getAge() == copy.getAge(), "age");

        assertThat(man.getFavoriteBooks() != copy.getFavoriteBooks(), "favoriteBooks List reference");
        assertThat(man.getFavoriteBooks().getClass() == copy.getFavoriteBooks().getClass(), "favoriteBooks List class");
        forEachElementPair(man.getFavoriteBooks(), copy.getFavoriteBooks(), (b1, b2) -> assertThat(b1.equals(b2), "favoriteBooks elements value"));

        // These fields are not set in additional ExtendedMan objects and exists in client only
        if (man.getDateOfBirth() != null || copy.getDateOfBirth() != null) {
            assertThat(man.getDateOfBirth() == copy.getDateOfBirth(), "dateOfBirth");
        }
        if (man.getFavoriteAuthors() != null || copy.getFavoriteAuthors() != null) {
            assertThat(man.getFavoriteAuthors() != copy.getFavoriteAuthors(), "favoriteAuthors List reference");
            assertThat(man.getFavoriteAuthors().getClass() == copy.getFavoriteAuthors().getClass(), "favoriteAuthors List class");
            forEachElementPair(man.getFavoriteAuthors(), copy.getFavoriteAuthors(), (a1, a2) -> assertThat(a1 != a2 && a1.equals(a2), "favoriteAuthors elements reference or value"));
        }
        if (man.getRelatives() != null || copy.getRelatives() != null) {
            assertThat(man.getRelatives() != copy.getRelatives(), "relatives Set reference");
            assertThat(man.getRelatives().getClass() == copy.getRelatives().getClass(), "relatives Set class");
            forEachElementPair(man.getRelatives(), copy.getRelatives(), (m, c) -> assertManCopy(m, c, recursiveLevel - 1));
        }
        if (man.getRelativesByAge() != null || copy.getRelativesByAge() != null) {
            assertThat(man.getRelativesByAge() != copy.getRelativesByAge(), "relativesByAge Set reference");
            assertThat(man.getRelativesByAge().getClass() == copy.getRelativesByAge().getClass(), "relativesByAge Set class");
            forEachElementPair(man.getRelativesByAge(), copy.getRelativesByAge(), (m, c) -> assertManCopy(m, c, recursiveLevel - 1));
        }
        if (man.getRelativesMemberNames() != null || copy.getRelativesMemberNames() != null) {
            assertThat(man.getRelativesMemberNames() != copy.getRelativesMemberNames(), "relativeMembers Map reference");
            assertThat(man.getRelativesMemberNames().getClass() == copy.getRelativesMemberNames().getClass(), "relativeMembers Map class");
            forEachElementPair(man.getRelativesMemberNames().entrySet(), copy.getRelativesMemberNames().entrySet(), (me, ce) -> {
                assertManCopy(me.getKey(), ce.getKey(), recursiveLevel - 1);
                assertThat(me.getValue().equals(ce.getValue()), "relativeMembers entry value");
            });
        }
        if (man.getReferrals() != null || copy.getReferrals() != null) {
            assertThat(man.getReferrals().length == copy.getReferrals().length, "referrals array length");
            forEachElementPair(Stream.of(man.getReferrals()).toList(), Stream.of(copy.getReferrals()).toList(), (m, c) -> assertManCopy(m, c, recursiveLevel - 1));
        }
        if (man.getBookRatings() != null || copy.getBookRatings() != null) {
            assertThat(man.getBookRatings().length == copy.getBookRatings().length, "bookRatings array length");
            assertThat(man.getBookRatings() != copy.getBookRatings(), "bookRatings array reference");
            for (int i = 0; i < man.getBookRatings().length; i++) {
                assertThat(man.getBookRatings()[i] != null && copy.getBookRatings()[i] != null, "bookRatings nested array not null");
                assertThat(man.getBookRatings()[i].length == copy.getBookRatings()[i].length, "bookRatings nested array length");
                assertThat(man.getBookRatings()[i] != copy.getBookRatings()[i], "bookRatings nested array reference");
                for (int j = 0; j < man.getBookRatings()[i].length; j++) {
                    assertThat(man.getBookRatings()[i][j] == copy.getBookRatings()[i][j], "bookRatings nested array value");
                }
            }
        }
        if (man.getAverageOrderValue() != null || copy.getAverageOrderValue() != null) {
            assertThat(man.getAverageOrderValue().equals(copy.getAverageOrderValue()), "averageOrderValue");
        }
    }

    private static <T> void forEachElementPair(Iterable<T> iterable1, Iterable<T> iterable2, BiConsumer<T, T> biConsumer) {
        Iterator<T> iterator1 = iterable1.iterator();
        Iterator<T> iterator2 = iterable2.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            T object1 = iterator1.next();
            T object2 = iterator2.next();
            biConsumer.accept(object1, object2);
        }
    }

    private static void assertThat(boolean expression, String issuePoint) {
        if (!expression) throw new RuntimeException("Assertion exception: " + issuePoint);
    }

}