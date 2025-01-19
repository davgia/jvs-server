package jvs.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Network utilities
 */
public class NetworkUtils {

    /**
     * Regex patterns to check whether a given ip is valid IPV4
     */
    private static Pattern IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])", Pattern.CASE_INSENSITIVE);

    /**
     * Regex patterns to check whether a given ip is valid IPV6
     */
    private static Pattern IPV6_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}", Pattern.CASE_INSENSITIVE);

    /**
     * Checks whether an input address is a valid IPV4.
     * @param address The input address.
     * @return True, if the address is a valid IPV4; otherwise false;
     */
    public static boolean isValidIPV4Address(final String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(address).matches();
    }

    /**
     * Checks whether an input address is a valid IPV6.
     * @param address The input address.
     * @return True, if the address is a valid IPV6; otherwise false;
     */
    public static boolean isValidIPV6Address(final String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        return IPV6_PATTERN.matcher(address).matches();
    }

    /**
     * Discovers the IP address of the local machine, excluding loopback addresses.
     * @param excludeIPV6 If true, the method
     * @return The IPV4 address of the local machine; otherwise null.
     */
    public static String discoverAddress(final boolean excludeIPV6) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {

                NetworkInterface networkInterface = interfaces.nextElement();

                //exclude loopback by default
                if (networkInterface.isLoopback() || !networkInterface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    //exclude ipv6 if needed
                    if (addr instanceof Inet6Address && excludeIPV6) continue;

                    if (addr.isSiteLocalAddress() && !addr.isAnyLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            //do nothing
        }

        return null;
    }
}
