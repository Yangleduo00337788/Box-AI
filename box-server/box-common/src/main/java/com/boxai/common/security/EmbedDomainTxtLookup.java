package com.boxai.common.security;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

public final class EmbedDomainTxtLookup {

    private static final Logger log = LoggerFactory.getLogger(EmbedDomainTxtLookup.class);

    private EmbedDomainTxtLookup() {
    }

    public static boolean containsToken(String domain, String token) {
        if (domain == null || domain.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        for (String name : List.of(domain, "_box-verify." + domain)) {
            for (String txt : lookupTxt(name)) {
                if (matches(txt, token)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean matches(String txt, String token) {
        if (txt == null) {
            return false;
        }
        String value = txt.replace("\"", "").trim();
        if (token.equals(value)) {
            return true;
        }
        String prefix = "box-verify=";
        if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            return token.equals(value.substring(prefix.length()).trim());
        }
        return false;
    }

    private static List<String> lookupTxt(String name) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "3000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        try {
            DirContext context = new InitialDirContext(env);
            try {
                Attributes attributes = context.getAttributes(name, new String[]{"TXT"});
                Attribute attribute = attributes.get("TXT");
                if (attribute == null) {
                    return List.of();
                }
                List<String> values = new ArrayList<>();
                NamingEnumeration<?> enumeration = attribute.getAll();
                while (enumeration.hasMore()) {
                    Object value = enumeration.next();
                    if (value != null) {
                        values.add(String.valueOf(value));
                    }
                }
                return values;
            } finally {
                context.close();
            }
        } catch (Exception ex) {
            log.debug("DNS TXT lookup failed for {}: {}", name, ex.getMessage());
            return List.of();
        }
    }
}
