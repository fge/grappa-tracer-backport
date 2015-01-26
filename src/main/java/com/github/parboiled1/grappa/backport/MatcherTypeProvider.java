package com.github.parboiled1.grappa.backport;

import org.parboiled.matchers.ActionMatcher;
import org.parboiled.matchers.AnyMatcher;
import org.parboiled.matchers.AnyOfMatcher;
import org.parboiled.matchers.CharIgnoreCaseMatcher;
import org.parboiled.matchers.CharMatcher;
import org.parboiled.matchers.EmptyMatcher;
import org.parboiled.matchers.FirstOfMatcher;
import org.parboiled.matchers.FirstOfStringsMatcher;
import org.parboiled.matchers.Matcher;
import org.parboiled.matchers.NothingMatcher;
import org.parboiled.matchers.OneOrMoreMatcher;
import org.parboiled.matchers.OptionalMatcher;
import org.parboiled.matchers.SequenceMatcher;
import org.parboiled.matchers.StringMatcher;
import org.parboiled.matchers.TestMatcher;
import org.parboiled.matchers.TestNotMatcher;
import org.parboiled.matchers.ZeroOrMoreMatcher;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Map of classes to {@link MatcherType matcher types}
 *
 * <p>In 2.0.x, {@code Matcher} defines a {@code .getType()} method; however,
 * 1.0.x (and parboiled 1.1.x) doesn't. This class helps supplement that to
 * collect traces.</p>
 *
 * <p>If you have your own matcher classes, you want to extend this class and
 * register your classes:</p>
 *
 * <pre>
 *     public class MyTypeProvider
 *         extends MatcherTypeProvider
 *     {
 *         public MyTypeProvider()
 *         {
 *             // for instance
 *             addMatcherClass(MyMatcher.class, MatcherType.COMPOSITE);
 *         }
 *     }
 * </pre>
 *
 * @see MatcherType
 */
@ParametersAreNonnullByDefault
public class MatcherTypeProvider
{
    private final Map<Class<? extends Matcher>, MatcherType> map
        = new HashMap<>();

    public MatcherTypeProvider()
    {
        addMatcherClass(ActionMatcher.class, MatcherType.ACTION);
        addMatcherClass(AnyMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(AnyOfMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(CharIgnoreCaseMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(CharMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(EmptyMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(FirstOfMatcher.class, MatcherType.COMPOSITE);
        addMatcherClass(FirstOfStringsMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(NothingMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(OneOrMoreMatcher.class, MatcherType.COMPOSITE);
        addMatcherClass(OptionalMatcher.class, MatcherType.COMPOSITE);
        addMatcherClass(SequenceMatcher.class, MatcherType.COMPOSITE);
        addMatcherClass(StringMatcher.class, MatcherType.TERMINAL);
        addMatcherClass(TestMatcher.class, MatcherType.PREDICATE);
        addMatcherClass(TestNotMatcher.class, MatcherType.PREDICATE);
        addMatcherClass(ZeroOrMoreMatcher.class, MatcherType.COMPOSITE);
    }

    /**
     * Register a matcher class and type
     *
     * @param c the class
     * @param type the type
     */
    protected final void addMatcherClass(final Class<? extends Matcher> c,
        final MatcherType type)
    {
        Objects.requireNonNull(c);
        Objects.requireNonNull(type);
        map.put(c, type);
    }
}
