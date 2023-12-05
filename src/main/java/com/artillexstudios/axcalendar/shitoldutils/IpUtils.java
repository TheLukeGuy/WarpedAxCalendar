package com.artillexstudios.axcalendar.shitoldutils;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class IpUtils {

    public static int ipToInt(@NotNull InetAddress inetAddress) {
        try {
            return ByteBuffer.wrap(inetAddress.getAddress()).getInt();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }
}
