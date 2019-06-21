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
 * Copyright 2019 Floréal Morandat <florealm@gmail.com>
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */
package com.github.gumtreediff.actions.model;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.ITreeClassifier;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.MatcherResult;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.io.Reader;

public abstract class D<G extends TreeGenerator, M extends Matcher,
                        A extends EditScriptGenerator, C extends ITreeClassifier> {

    protected abstract G makeGenerator();

    protected abstract M makeMatcher();

    protected abstract A makeEditScriptGenerator();

//    protected abstract C makeClassifier();

    final void diff(Reader srcSource, Reader dstSource) throws IOException {
        TreeContext src = makeGenerator().generateFrom().reader(srcSource);
        TreeContext dst = makeGenerator().generateFrom().reader(dstSource);
        MappingStore ms = new MappingStore(src.getRoot(), dst.getRoot());
        MatcherResult matcherResult = new MatcherResult(src, dst, ms);

        makeMatcher().match(src.getRoot(), dst.getRoot(), ms);

        EditScript editScript = makeEditScriptGenerator().computeActions(matcherResult);

//        makeClassifier().
    }
}
