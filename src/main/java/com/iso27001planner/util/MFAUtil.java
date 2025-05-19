package com.iso27001planner.util;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.iso27001planner.entity.User;
import com.iso27001planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MFAUtil {
    public static String generateOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000); // 6-digit OTP
    }
}
