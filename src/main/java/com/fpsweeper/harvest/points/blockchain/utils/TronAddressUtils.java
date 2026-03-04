package com.fpsweeper.harvest.points.blockchain.utils;

import java.security.MessageDigest;
import java.util.Arrays;

public class TronAddressUtils {

    private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int[] INDEXES = new int[128];

    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    /**
     * Convert Tron hex address to Base58Check format
     */
    public static String hexToBase58(String hexAddress) {
        try {
            // Remove "41" prefix if present (Tron mainnet prefix)
            if (hexAddress.startsWith("41")) {
                hexAddress = hexAddress.substring(2);
            }

            // Convert hex to bytes
            byte[] addressBytes = hexStringToByteArray(hexAddress);

            // Add Tron address prefix (0x41 for mainnet)
            byte[] addressWithPrefix = new byte[21];
            addressWithPrefix[0] = 0x41;
            System.arraycopy(addressBytes, 0, addressWithPrefix, 1, Math.min(addressBytes.length, 20));

            // Add checksum (double SHA256)
            byte[] hash = sha256(sha256(addressWithPrefix));
            byte[] checksum = Arrays.copyOfRange(hash, 0, 4);

            // Combine address + checksum
            byte[] addressWithChecksum = new byte[25];
            System.arraycopy(addressWithPrefix, 0, addressWithChecksum, 0, 21);
            System.arraycopy(checksum, 0, addressWithChecksum, 21, 4);

            // Encode to Base58
            return encode(addressWithChecksum);

        } catch (Exception e) {
            System.err.println("Error converting hex to Base58: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convert hex string to byte array
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Base58 encode
     */
    private static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }

        // Count leading zeros
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            ++zeros;
        }

        // Convert to base58
        input = Arrays.copyOf(input, input.length);
        char[] encoded = new char[input.length * 2];
        int outputStart = encoded.length;
        for (int inputStart = zeros; inputStart < input.length; ) {
            encoded[--outputStart] = ALPHABET[divmod(input, inputStart, 256, 58)];
            if (input[inputStart] == 0) {
                ++inputStart;
            }
        }

        // Preserve leading zeros
        while (outputStart < encoded.length && encoded[outputStart] == ALPHABET[0]) {
            ++outputStart;
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ALPHABET[0];
        }

        return new String(encoded, outputStart, encoded.length - outputStart);
    }

    /**
     * Divmod helper for Base58
     */
    private static byte divmod(byte[] number, int firstDigit, int base, int divisor) {
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = (int) number[i] & 0xFF;
            int temp = remainder * base + digit;
            number[i] = (byte) (temp / divisor);
            remainder = temp % divisor;
        }
        return (byte) remainder;
    }

    /**
     * SHA-256 hash
     */
    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Convert Base58 to hex (for validation)
     */
    public static String base58ToHex(String base58Address) {
        try {
            byte[] decoded = decode(base58Address);

            // Remove checksum (last 4 bytes)
            byte[] addressBytes = Arrays.copyOfRange(decoded, 0, decoded.length - 4);

            // Verify checksum
            byte[] hash = sha256(sha256(addressBytes));
            byte[] checksum = Arrays.copyOfRange(hash, 0, 4);
            byte[] actualChecksum = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);

            if (!Arrays.equals(checksum, actualChecksum)) {
                throw new RuntimeException("Invalid checksum");
            }

            // Remove prefix (first byte should be 0x41)
            byte[] addressWithoutPrefix = Arrays.copyOfRange(addressBytes, 1, addressBytes.length);

            // Convert to hex
            StringBuilder hex = new StringBuilder();
            for (byte b : addressWithoutPrefix) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            System.err.println("Error converting Base58 to hex: " + e.getMessage());
            return null;
        }
    }

    /**
     * Base58 decode
     */
    private static byte[] decode(String input) {
        if (input.length() == 0) {
            return new byte[0];
        }

        byte[] input58 = new byte[input.length()];
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int digit = c < 128 ? INDEXES[c] : -1;
            if (digit < 0) {
                throw new RuntimeException("Invalid character in Base58: " + c);
            }
            input58[i] = (byte) digit;
        }

        int zeros = 0;
        while (zeros < input58.length && input58[zeros] == 0) {
            ++zeros;
        }

        byte[] decoded = new byte[input.length()];
        int outputStart = decoded.length;
        for (int inputStart = zeros; inputStart < input58.length; ) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256);
            if (input58[inputStart] == 0) {
                ++inputStart;
            }
        }

        while (outputStart < decoded.length && decoded[outputStart] == 0) {
            ++outputStart;
        }

        return Arrays.copyOfRange(decoded, outputStart - zeros, decoded.length);
    }
}