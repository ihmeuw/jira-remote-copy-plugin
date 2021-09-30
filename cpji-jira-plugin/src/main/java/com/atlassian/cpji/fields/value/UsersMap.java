package com.atlassian.cpji.fields.value;

import com.atlassian.jira.user.ApplicationUser;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class UsersMap extends LinkedHashMap<String, Collection<ApplicationUser>> {

    private static final long serialVersionUID = 9068908421393575838L;

    private static final int MAX_ENTRIES = 1_000;

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Collection<ApplicationUser>> eldest) {
        return size() > MAX_ENTRIES;
    }
}
