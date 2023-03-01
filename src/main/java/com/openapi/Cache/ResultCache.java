package com.openapi.Cache;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


public class ResultCache {

    private static Logger logger = LogManager.getLogger(ResultCache.class);

    private static class cache{
        public static final ResultCache instance=new ResultCache();
    }
    public static ResultCache getInstance(){
        return cache.instance;
    }


    public static Cache<String, String> results = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    public void setCache (String askid,String id){
        results.put(askid,id);
    }

    public String isCacheExist (String askid){
        return results.getIfPresent(askid);

    }

}
