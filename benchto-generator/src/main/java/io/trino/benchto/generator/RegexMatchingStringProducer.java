/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.benchto.generator;

import com.google.common.base.Predicate;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.shuffle;
import static java.util.Objects.requireNonNull;

public class RegexMatchingStringProducer
        implements ObjectProducer<String>
{
    private static final double ACCEPT_PROBABILITY = 0.3d;

    private final Automaton automaton;
    private final int minLength;
    private final int maxLength;
    private final Random random;
    private final Set<DeadState> deadStates = newHashSet();

    public RegexMatchingStringProducer(String regex, int minLength, int maxLength, Random random)
    {
        checkState(minLength > 0, "minimal length must be greater than 0");
        checkState(maxLength >= minLength, "maximal length must be greater than minimal length");

        this.automaton = new RegExp(requireNonNull(regex)).toAutomaton();
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.random = requireNonNull(random);
    }

    public RegexMatchingStringProducer(String regex, int minLength, int maxLength)
    {
        this(regex, minLength, maxLength, new Random());
    }

    @Override
    public String generateNext()
    {
        Stack<GenerateContext> stack = new Stack<>();
        stack.push(new GenerateContext(automaton.getInitialState()));
        String selectedMatchingString = null;

        while (!stack.isEmpty()) {
            GenerateContext context = stack.pop();
            final int matchingStringLength = context.length;
            State state = context.state;

            boolean matches = state.isAccept() && matchingStringLength >= minLength && matchingStringLength <= maxLength;
            if (matches) {
                selectedMatchingString = context.buildString();
                if (random.nextDouble() <= ACCEPT_PROBABILITY) {
                    return selectedMatchingString;
                }
            }

            List<Transition> transitions = newArrayList(filter(state.getTransitions(), filterDeadStates(matchingStringLength + 1)));

            if ((transitions.size() == 0 && !matches) || matchingStringLength > maxLength) {
                deadStates.add(new DeadState(matchingStringLength, state));
                continue;
            }

            shuffle(transitions, random);

            for (Transition transition : transitions) {
                int diff = transition.getMax() - transition.getMin() + 1;
                int randomOffset = diff;
                if (diff > 0) {
                    randomOffset = random.nextInt(diff);
                }
                char randomChar = (char) (randomOffset + transition.getMin());

                stack.add(new GenerateContext(context, String.valueOf(randomChar), transition.getDest()));
            }
        }

        if (selectedMatchingString == null) {
            throw new IllegalStateException("Cannot generate matching string");
        }

        return selectedMatchingString;
    }

    private Predicate<Transition> filterDeadStates(final int length)
    {
        return new Predicate<Transition>()
        {
            @Override
            public boolean apply(Transition transition)
            {
                return !deadStates.contains(new DeadState(length, transition.getDest()));
            }
        };
    }

    private static class DeadState
    {
        final int length;
        final State state;

        DeadState(int length, State state)
        {
            this.length = length;
            this.state = state;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(length, state);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final DeadState other = (DeadState) obj;
            return Objects.equals(length, other.length) && Objects.equals(state, other.state);
        }
    }

    private static class GenerateContext
    {
        final GenerateContext previous;
        final String string;
        final int length;
        final State state;

        GenerateContext(GenerateContext previous, String string, State state)
        {
            this.previous = previous;
            this.string = string;
            this.length = previous.length + string.length();
            this.state = state;
        }

        GenerateContext(State state)
        {
            this.previous = null;
            this.string = "";
            this.length = 0;
            this.state = state;
        }

        String buildString()
        {
            StringBuilder builder = new StringBuilder(length);
            GenerateContext context = this;
            while (context != null) {
                builder.append(context.string);
                context = context.previous;
            }
            return builder.reverse().toString();
        }
    }
}
