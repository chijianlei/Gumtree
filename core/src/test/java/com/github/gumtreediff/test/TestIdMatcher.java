/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.heuristic.IdMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestIdMatcher {
    @Test
    public void testIdMatcher() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getGumtreePair();
        ITree t1 = trees.first.getRoot();
        ITree t2 = trees.second.getRoot();
        t1.setMetadata("id", "id1");
        t2.setMetadata("id", "id1");

        t1.getChild(0).setMetadata("id", "id2");
        t1.getChild(1).setMetadata("id", "id2");
        t2.getChild(0).setMetadata("id", "id2");

        t1.getChild(2).setMetadata("id", "id3");
        t2.getChild(1).setMetadata("id", "id3");
        t2.getChild(2).setMetadata("id", "id3");

        Matcher matcher = new IdMatcher();
        MappingStore ms = matcher.match(t1, t2);

        assertEquals(1, ms.size());
        assertTrue(ms.has(t1, t2));
    }

    @Test
    public void testSimAndSizeThreshold() {
        Pair<ITree, ITree> trees = TreeLoader.getBottomUpPair();
        ITree t1 = trees.first;
        ITree t2 = trees.second;
        MappingStore ms = new MappingStore(t1, t2);
        ms.addMapping(t1.getChild("0.2.0"), t2.getChild("0.2.0"));
        ms.addMapping(t1.getChild("0.2.1"), t2.getChild("0.2.1"));
        ms.addMapping(t1.getChild("0.2.2"), t2.getChild("0.2.2"));
        ms.addMapping(t1.getChild("0.2.3"), t2.getChild("0.2.3"));

        GreedyBottomUpMatcher matcher = new GreedyBottomUpMatcher();
        GumTreeProperties properties = new GumTreeProperties();

        matcher.setSim_threshold(1.0);
        matcher.setSize_threshold(0);

        MappingStore ms1 = matcher.match(t1, t2, new MappingStore(ms));

        assertEquals(5, ms1.size());
        for (Mapping m : ms)
            assertTrue(ms1.has(m.first, m.second));
        assertTrue(ms1.has(t1, t2));

        matcher.setSim_threshold(0.5);
        matcher.setSize_threshold(0);

        MappingStore ms2 = matcher.match(t1, t2, new MappingStore(ms));
        assertEquals(7, ms2.size());
        for (Mapping m : ms)
            assertTrue(ms2.has(m.first, m.second));
        assertTrue(ms2.has(t1, t2));
        assertTrue(ms2.has(t1.getChild(0), t2.getChild(0)));
        assertTrue(ms2.has(t1.getChild("0.2"), t2.getChild("0.2")));

        matcher.setSim_threshold(0.5);
        matcher.setSize_threshold(10);

        MappingStore ms3 = matcher.match(t1, t2, new MappingStore(ms));
        assertEquals(9, ms3.size());
        for (Mapping m : ms)
            assertTrue(ms3.has(m.first, m.second));
        assertTrue(ms3.has(t1, t2));
        assertTrue(ms3.has(t1.getChild(0), t2.getChild(0)));
        assertTrue(ms3.has(t1.getChild("0.0"), t2.getChild("0.0")));
        assertTrue(ms3.has(t1.getChild("0.1"), t2.getChild("0.1")));
        assertTrue(ms3.has(t1.getChild("0.2"), t2.getChild("0.2")));
    }
}