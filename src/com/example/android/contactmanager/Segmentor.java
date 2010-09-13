package com.example.android.contactmanager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Segmentor {
    private HashMap<String, HashSet<String>> map= new HashMap<String, HashSet<String>>();
	public void add(String key, String id) {
		HashSet<String> set=map.get(key);
		if (set==null) {
			set=new HashSet<String>();
			map.put(key, set);
		}
		set.add(id);
		
	}
	public HashSet<String> getIntersection(String[] keys) {
		HashSet<String> retval = null;
		if (keys.length>0) {
			HashSet<String> set=map.get(keys[0]);
			retval=set;	
				
		
		for (String key:keys) {
		 retval.retainAll(map.get(key));	
		}
		}
		return retval;
	}
	public Set<String> getKeys() {
		 
		return map.keySet();
	}
	 
}
