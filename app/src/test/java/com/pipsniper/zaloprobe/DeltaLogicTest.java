package com.pipsniper.zaloprobe;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeltaLogicTest {
    @Test
    public void firstSnapshotReturnsAllIndexes() {
        List<Integer> out = DeltaLogic.newIndexes(Collections.emptyList(), Arrays.asList("a", "b", "c"));
        assertEquals(Arrays.asList(0, 1, 2), out);
    }

    @Test
    public void repeatedNotificationUpdateReturnsOnlyNewLine() {
        List<Integer> out = DeltaLogic.newIndexes(Arrays.asList("a", "b"), Arrays.asList("a", "b", "c"));
        assertEquals(Collections.singletonList(2), out);
    }

    @Test
    public void duplicateTextCountsAreHandledCorrectly() {
        List<Integer> out = DeltaLogic.newIndexes(Arrays.asList("a"), Arrays.asList("a", "a"));
        assertEquals(Collections.singletonList(1), out);
    }

    @Test
    public void reorderedExistingLinesDoNotCreateDuplicates() {
        List<Integer> out = DeltaLogic.newIndexes(Arrays.asList("a", "b"), Arrays.asList("b", "a"));
        assertEquals(Collections.emptyList(), out);
    }
}
