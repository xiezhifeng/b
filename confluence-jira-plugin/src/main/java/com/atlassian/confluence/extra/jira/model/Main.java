package com.atlassian.confluence.extra.jira.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by tdang on 25/06/15.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Cache<String, String> t = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<String, String>() {
            public String load(String key) {
                return null;
            }
        });
        t.asMap().put("1", "2");
        Thread.sleep(1000);
        String r = t.get("1");
        System.out.print(r);
    }
}
