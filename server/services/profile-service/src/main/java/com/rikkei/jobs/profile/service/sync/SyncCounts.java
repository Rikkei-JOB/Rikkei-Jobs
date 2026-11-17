package com.rikkei.jobs.profile.service.sync;

import java.util.LinkedHashMap;
import java.util.Map;

/** Số bản ghi của một lần đồng bộ; lưu vào sync_runs.counts. */
public record SyncCounts(int fetched, int inserted, int updated, int unchanged, int skipped) {
    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("fetched", fetched);
        map.put("inserted", inserted);
        map.put("updated", updated);
        map.put("unchanged", unchanged);
        map.put("skipped", skipped);
        return map;
    }
}
