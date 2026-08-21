package com.pipsniper.zaloprobe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeltaLogic {
    private DeltaLogic() {}

    public static List<Integer> newIndexes(List<String> previousHashes, List<String> currentHashes) {
        Map<String, Integer> remaining = new HashMap<>();
        if (previousHashes != null) {
            for (String h : previousHashes) remaining.put(h, remaining.getOrDefault(h, 0) + 1);
        }
        List<Integer> out = new ArrayList<>();
        if (currentHashes == null) return out;
        for (int i = 0; i < currentHashes.size(); i++) {
            String h = currentHashes.get(i);
            int count = remaining.getOrDefault(h, 0);
            if (count > 0) remaining.put(h, count - 1);
            else out.add(i);
        }
        return out;
    }
}
