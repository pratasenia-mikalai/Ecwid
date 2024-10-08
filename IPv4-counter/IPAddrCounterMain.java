import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 *
 * You have a simple text file with IPv4 addresses. One line is one address, line by line:
 * 145.67.23.4
 * 8.34.5.23
 * 89.54.3.124
 * 89.54.3.124
 * 3.45.71.5
 * ...
 * The file is unlimited in size and can occupy tens and hundreds of gigabytes.
 * You should calculate the number of unique addresses in this file using as little memory and time as possible.
 *
 *
 *
 * Ideas of solution.
 * The least amount of memory we can use to store the information about address founding is 1 bit.
 * IP v4 is 32-bit number, so in theory we just can use maximum 2^32 bits = 512 MB to cover all possible address space within single array.
 * Not so much, but moving closer to real life we know, that some address ranges are reserved for special use,
 * and in general IP addresses in real lists usually grouped by purposes or location ranges.
 * First 2 octets represent most significant bits in address ranges, so we can expect that not all of its possible values
 * exist in some certain list. So I suggest using the following structure:
 *
 * array[256] - represents first octets, store other following arrays, or its references (if we want to save memory, it might be 4 byte each)
 *  \-> array[256] - represents second octets, again store other arrays (4 byte each)
 *       \-> array byte[256 * 256 / 8] - byte array with bit length of 0x10000 or 8192 byte total, to store bits representing third and fourth octets found.
 *
 * This structure allows to dynamically allocate memory by blocks of 8 kB for each unique first + second octets pair.
 * In case of finding all the address ranges total additional memory cost in comparison with 512MB is 256*31 + 256*256*31 = 2039552 extra bits, 248 kB. Reasonable.
 * If there will not be at least one of possible first octets, we will save more, than spend.
 *
 * And as we know exactly the format of strings and as we are going to make really fast solution
 * we also should create optimized parser for it
 *
 */
public class IPAddrCounterMain {

    public static void main(String[] args) {

        Path path = Path.of( "D:/ip_addresses");
        var storage = new IPv4Storage();

        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(storage::put);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(storage.getSize());
    }

}
