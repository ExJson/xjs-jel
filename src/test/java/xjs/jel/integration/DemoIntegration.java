package xjs.jel.integration;

import org.junit.jupiter.api.Test;
import xjs.jel.exception.JelException;

// Just manual experimentation ATM. Will replace with dedicated ITs soon
public final class DemoIntegration extends AbstractIntegrationTest {

    @Test
    public void delegateExpressionDemo() throws JelException {
        this.parse("""
            
            Config >> (values): {
              color: $($values.color).orElse(brown)
              type: $($value.type).orElse(fruit)
              
              s >> private: $values.size
              size >> if: {
                $s == null: default
                $s < 25: small // comment
                $s < 50: medium
                _: large
              }
            }
            
            cfg >> @Config: {
              color: yellow
              size: 35
            }
            
            """);
    }

    @Test
    public void delegate_fromPath () {
        this.inputSuccess("""
            utils >> private: {
              Delegate >> (value): $value with delegate
            }
            out >> @utils.Delegate: banana
            """);
        this.outputTrimmed("""
            out: banana with delegate
            """);
    }

    @Test
    public void testingMatch() throws JelException {
        this.parse("""
            t1: abc
            t2 >> match $t1: {
              1: one
              2: two
              _: $t1
            }
            test >> (a) match $a: {
              1: one
              2: two
              _: $a
            }
            >> log: first: $t2
            >> log: [
              $test(1)
              $test(2)
              $test(other)
              $test(3)
              $test(4)
            ]
            """);
    }

    @Test
    public void missingAlias_conditionalStillEvaluatesCaptures() throws JelException {
        this.parse("""
            
            person: {
              name: Dalton
              name >> (): $name
              set_name >> (value): {
                >> if: {
                  $value == Doggy >> log error: "I don't really like Doggy"
                  _ >> log: "Oh i rly like that name that's so good"
                }
                $name >> set: $value
              }
            }
            
            >>: $person.set_name(Kitty)
            >>: $person.set_name(Doggy)
            
            """);
    }

    @Test
    public void nestedMatch_isStillEvaluated() throws JelException {
        this.parse("""
            
            person: {
              name: First
              name >> (): $name
              set_name >> (value): {
                >> if: {
                  $name == First >> match $value: {
                    Kitty >> log: nested match working!
                  }
                  _ >> log: name has already been set
                }
                $name >> set: $value
              }
            }
            
            >>: $person.set_name(Kitty)
            >>: $person.set_name(Doggy)
            
            """);
    }

    @Test
    public void generator_withConditional_isFilter() throws JelException {
        this.parse("""
            
            numbers: [ 1, 2, 3, 4, 5, 6, 7, 8 ]
            
            evens >> [$numbers..]
                if ($v % 2 == 0):
              $v
            
            """);
    }

    @Test
    public void timeFunction_example() throws JelException {
        this.parse("""
            /// n is a string which starts with a number
            /// and is followed by a time unit.
            /// for example:
            ///  * '3h' = 3 hours
            ///  * '5m' = 4 minutes
            ///  * '1d' = 1 day
            ///  * '0.5y' = 6 months
            time >> (n) if: {
              $n.endsWith(s): $parse($n.replace(s$,,))
              $n.endsWith(m): 60 * $parse($n.replace(m$,,))
              $n.endsWith(h): 360 * $parse($n.replace(h$,,))
              $n.endsWith(y): 365 * $parse($n.replace(y$,,)) * 24 * 60 * 60
              _ >> raise: {
                msg >> if: {
                  $n.matches(\\d+): no unit provided
                  _: unknown unit: $n.replace(^\\d+,,)
                }
                details: must be one of: [ s, m, h, y ]
              }
            }
            out: $time(5h)
            """);
    }

    @Test
    public void matchArray_isLiteralArrayForNow() {
        this.inputSuccess("""
            a >> private: 1
            b >> private: 2
            out >> match $([$a, $b]): {
              [ 3, 4 ]: failure
              [ 5, 6 ]: sadness
              [ 7, 8 ]: misery
              [ 1, 2 ]: happiness
              _: doom
            }
            """);
        this.outputTrimmed("""
            out: happiness
            """);
    }

    @Test
    public void nested_destructurePatterns() {
        this.inputSuccess("""
            [ {a, b: x}, { c: a }, [ { d: a }, { e: a } ] ] >> from:
              [ {a: 1, x: 2 }, { a: 3 }, [ { a: 4 }, { a: 5 } ] ]
            """);
        this.outputTrimmed("""
            a: 1, b: 2, c: 3, d: 4, e: 5
            """);
    }

    @Test
    public void privateRef() {
        this.inputSuccess("""
            banana >> private: abc
            fruit: $banana
            """);
        this.outputTrimmed("""
            fruit: abc
            """);
    }

    @Test
    public void log2() throws JelException {
        this.parse("""
            test >> (a) [ 1, 2, 3 ] (b): '$a +' + ' $v + ' + $b
            b >> log: $test('value:')(':)')
            
            concat >> (a)(b): $a$b
            test >> log: $concat(1)(2)
            """);
        this.parse(">> log: 'hello, world!'");
    }

    @Test
    public void test2() {
        this.inputSuccess("""
            o >> private: {
              a >> private: 3.14
              a >> (): $a
            }
            using field: $o.a
            using accessor: $o.a()
            """);
        this.outputTrimmed("""
            using field: null
            using accessor: 3.14
            """);
    }

    @Test
    public void jel_supportsTestingRegularExpression() {
        this.inputSuccess("""
            s: banana apple strawberry
            a: $s.matches(.*banana.*)
            b: $s.matches(.*apple.*)
            c: $s.matches((\\w+\\s?){3})
            d: $s.matches(\\d+)
            e: $s.matches([^\\d]+)
            t: $s.matches(.*strawberry$)
            """);
        this.outputTrimmed("""
            s: banana apple strawberry
            a: true
            b: true
            c: true
            d: false
            e: true
            t: true
            """);
    }

    @Test
    public void demo2() {
        this.inputFailure("""
            banana: 'hello, world!'
            this will fail >> noinline: {}
            """);
        this.outputExactly("""
            JelException: Recursive noinline modifier support is still WIP
            --------------------------------------------------------------
                1 | this will fail >> noinline: {}
                                      ^^^^^^^^
            --------------------------------------------------------------
            For now, append this modifier to primitive values only""");
    }
}
