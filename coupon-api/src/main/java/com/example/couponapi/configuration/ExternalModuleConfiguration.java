package com.example.couponapi.configuration;

import com.example.couponcore.CouponCoreConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Import(CouponCoreConfiguration.class)
@Profile("!restDocs")
public class ExternalModuleConfiguration {
}
