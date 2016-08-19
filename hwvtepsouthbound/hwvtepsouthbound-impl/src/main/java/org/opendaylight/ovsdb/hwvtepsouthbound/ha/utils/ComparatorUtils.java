package org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil.isEmptyList;

public class ComparatorUtils {

    static class ComparatorBasedPredicate<T> implements Predicate<T> {

        Comparator comparator;
        T updated;
        ComparatorBasedPredicate(T updated, Comparator comparator) {
            this.updated = updated;
            this.comparator = comparator;
        }
        @Override
        public boolean apply(T original) {
            return comparator.compare(updated, original) == 0;
        }
    }

    static class DoesNotContainCondition<T> implements Predicate<T> {

        Comparator comparator;
        List<T> original;
        DoesNotContainCondition(List<T> original, Comparator comparator) {
            this.original = original;
            this.comparator = comparator;
        }
        @Override
        public boolean apply(T updatedItem) {
            return !Iterables.any(original, new ComparatorBasedPredicate(updatedItem, comparator));
        }
    }

    public static <T> List<T> diffOf(List<T> updated, final List<T> original, final Comparator comparator) {
        if (updated == null) {
            return Lists.newArrayList();
        }
        if (original == null) {
            return new ArrayList<>(updated);
        }

        return Lists.newArrayList(Iterables.filter(updated, new DoesNotContainCondition<T>(original, comparator)));
    }

    public static <T> List<T> translateOperationalAdd(List<T> dst,
                                                    List<T> src,
                                                    Function<T,T> translator,
                                                    Comparator<T> comparator) {
        if (isEmptyList(src)) {
            return dst;
        }
        if (dst == null) {
            dst = Lists.newArrayList();
        }
        List<T> added = diffOf(src, dst, comparator);
        added = diffOf(added, dst, comparator);

        dst.addAll(Lists.transform(added, translator));
        return dst;
    }

    public static <T> List<T> translateConfigUpdate(List<T> updatedSrc, Function<T,T> translator) {
        if (updatedSrc == null) {
            return updatedSrc;
        }
        return Lists.transform(updatedSrc, translator);
    }

    public static <T> List<T> translateOperationalUpdate(List<T> dst,
                                                     List<T> updatedSrc,
                                                     List<T> origSrc,
                                                     Function<T,T> translator,
                                                     Comparator<T> comparator
                                                     ) {
        if (dst == null) {
            dst = Lists.newArrayList();
        }
        List<T> added   = diffOf(updatedSrc, origSrc, comparator);
        List<T> removed = diffOf(origSrc, updatedSrc, comparator);

        added = diffOf(added, dst, comparator);
        dst = diffOf(dst, removed, comparator);
        dst.addAll(Lists.transform(added, translator));
        return dst;
    }
}
