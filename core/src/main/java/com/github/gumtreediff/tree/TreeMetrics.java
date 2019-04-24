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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2019 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BiFunction;

public class TreeMetrics {
    public final int size;

    public final int height;

    public final int hash;

    public final int depth;

    public final int position;

    public TreeMetrics(int size, int height, int hash, int depth, int position) {
        this.size = size;
        this.height = height;
        this.hash = hash;
        this.depth = depth;
        this.position = position;
    }

    public MetricHolder<Integer> position() {
        return new MetricHolder<Integer>() {
            @Override
            public Integer value(TreeMetrics metrics) {
                return metrics.position;
            }
        };
    }

    public MetricHolder<Integer> size() {
        return new MetricHolder<Integer>() {
            @Override
            public Integer value(TreeMetrics metrics) {
                return metrics.size;
            }
        };
    }

    public MetricHolder<Integer> hash() {
        return new MetricHolder<Integer>() {
            @Override
            public Integer value(TreeMetrics metrics) {
                return metrics.height;
            }
        };
    }

    public MetricHolder<Integer> depth() {
        return new MetricHolder<Integer>() {
            @Override
            public Integer value(TreeMetrics metrics) {
                return metrics.depth;
            }
        };
    }

    public static class Comparators {
        public static Provider<Integer> position() {
            return new Provider<Integer>() {
                @Override
                public Integer get(TreeMetrics metrics) {
                    return metrics.position;
                }
            };
        }

        public static Provider<Integer> height() {
            return new Provider<Integer>() {
                @Override
                public Integer get(TreeMetrics metrics) {
                    return metrics.height;
                }
            };
        }

        public static Provider<Integer> size() {
            return new Provider<Integer>() {
                @Override
                public Integer get(TreeMetrics metrics) {
                    return metrics.size;
                }
            };
        }

        public static Provider<Integer> depth() {
            return new Provider<Integer>() {
                @Override
                public Integer get(TreeMetrics metrics) {
                    return metrics.depth;
                }
            };
        }

        public static Provider<Integer> hash() { // FIXME Should exist or not. Comparing hash is a bit meaningless
            return new Provider<Integer>() {
                @Override
                public Integer get(TreeMetrics metrics) {
                    return metrics.hash;
                }
            };
        }
    }

    public abstract class MetricHolder<T extends Comparable<T>> {
        public final int compare(ITree other) {
            return compare(other.getMetrics());
        }

        public final int compare(TreeMetrics other) {
            return value(TreeMetrics.this).compareTo(value(other));
        }

        public final T min(ITree other) {
            return min(other.getMetrics());
        }

        public final T min(TreeMetrics other) {
            T s = value(TreeMetrics.this);
            T o = value(other);
            return s.compareTo(o) <= 0 ? s : o;
        }

        public final T max(ITree other) {
            return max(other.getMetrics());
        }

        public final T max(TreeMetrics other) {
            T s = value(TreeMetrics.this);
            T o = value(other);
            return s.compareTo(o) >= 0 ? s : o;
        }

        public final boolean equalsTo(ITree other) {
            return equalsTo(other.getMetrics());
        }

        public final boolean equalsTo(TreeMetrics other) {
            return equalsTo(value(other));
        }

        public final boolean equalsTo(T other) { // FIXME is it a good idea to expose this one ? Why not the others
            return value(TreeMetrics.this).compareTo(other) == 0;
        }

        public abstract T value(TreeMetrics metrics);
    }

    public abstract static class Provider<T extends Comparable<T>>
            implements Comparator<TreeMetrics> { // Unfortunately it can't also be a Comparator<ITree>

        public final T get(ITree tree) {
            return get(tree.getMetrics());
        }

        public abstract T get(TreeMetrics metrics);

        public final int compare(TreeMetrics some, ITree other) {
            return compare(some, other.getMetrics());
        }

        public final int compare(ITree some, ITree other) {
            return compare(some.getMetrics(), other.getMetrics());
        }

        public final int compare(TreeMetrics some, TreeMetrics other) {
            return get(some).compareTo(get(other));
        }

        public final T fold(Iterable<? extends ITree> others, BiFunction<T, T, T> fun) {
            Iterator<? extends ITree> it = others.iterator();
            if (!it.hasNext())
                return null;
            T m = get(it.next());
            while (it.hasNext())
                m = fun.apply(m, get(it.next()));
            return m;
        }

        public final T min(Iterable<? extends ITree> others) {
            return fold(others, (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }

        public final T max(Iterable<? extends ITree> others) {
            return fold(others, (a, b) -> a.compareTo(b) > 0 ? a : b);
        }
    }
}
