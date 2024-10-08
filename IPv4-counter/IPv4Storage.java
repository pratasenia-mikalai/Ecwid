public class IPv4Storage {

    private final byte[][][] STORAGE = new byte[256][][];
    private long size = 0L;


    public boolean put(String address) {
        int[] splittedAddress = parseAddress(address);

        int octet1 = splittedAddress[0];
        int octet2 = splittedAddress[1];
        int last2octets = splittedAddress[2];

        if (STORAGE[octet1] == null) {
            STORAGE[octet1] = new byte[256][];
        }
        if (STORAGE[octet1][octet2] == null) {
            STORAGE[octet1][octet2] = new byte[8192];
        }

        int lastArrayByteIndex = last2octets / 8;
        byte bitMask = (byte)(1 << (last2octets % 8));

        byte bitBlock = STORAGE[octet1][octet2][lastArrayByteIndex];

        if ((byte) (bitBlock & bitMask) != bitMask) {
            STORAGE[octet1][octet2][lastArrayByteIndex] |= bitMask;
            size++;
            return true;
        }
        return false;
    }

    public long getSize() {
        return this.size;
    }

    /**
     * Split address into 3 int component
     * Example:
     * "145.67.23.4" results int[] = { 145, 67, 5892 }
     * where 5892 = 00010111_00000100 (byte numbers 23 and 4 union in binary)
     *
     */
    private int[] parseAddress(String address) {
        int[] numbers = new int[3];
        int numbersIndex = 0;

        int octet = 0;
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            if (c == '.') {
                if (numbersIndex < 2) {
                    numbers[numbersIndex] = octet;
                    octet = 0;
                    numbersIndex++;
                } else {
                    numbers[numbersIndex] = octet << 8;
                    octet = 0;
                }
            } else {
                int digit = address.charAt(i) - '0';
                octet = octet * 10 + digit;
            }
        }
        numbers[numbersIndex] += octet;

        return numbers;
    }

}
