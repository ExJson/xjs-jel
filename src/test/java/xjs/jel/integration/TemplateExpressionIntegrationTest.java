package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class TemplateExpressionIntegrationTest extends AbstractIntegrationTest {

    private static final String TIME_FUNCTION = """
        time >> (n) if: {
          $n.endsWith(s): $parse($n.replace(s$,,))
          $n.endsWith(m): $parse($n.replace(m$,,)) * 60
          $n.endsWith(h): $parse($n.replace(h$,,)) * 60 * 60
          $n.endsWith(y): $parse($n.replace(y$,,)) * 365 * 24 * 60 * 60
          _ >> raise: {
            msg >> if: {
              $n.matches(\\d+): no unit provided
              _: unknown unit: $n.replace(^\\d+,,)
            }
            details: must be one of: [ s, m, h, y ]
          }
        }
        """;

    @Test
    public void interpreter_toleratesRealisticFunction() {
        this.inputSuccess(TIME_FUNCTION + """
            a: $time(3m)
            b: $time(5h)
            c: $time(20s)
            """);
        this.outputTrimmed("""
            a: 180
            b: 18000
            c: 20
            """);
    }

    @Test
    public void timeFunction_doesThrowNoUnitProvided() {
        this.inputFailure(TIME_FUNCTION + """
            a: $time(30)
            """);
        this.outputExactly("""
            JelException: no unit provided
            ---------------------------------------------------
                6 |   _ >> raise: {
                           ^^^^^
                7 |     msg >> if: {
                8 |       $n.matches(\\d+): no unit provided
                9 |       _: unknown unit: $n.replace(^\\d+,,)
               10 |     }
               11 |     details: must be one of: [ s, m, h, y ]
               12 |   }
               13 | }
               14 | a: $time(30)
                        ^^^^^^^^
            ---------------------------------------------------
            must be one of: [ s, m, h, y ]""");
    }

    @Test
    public void timeFunction_doesThrowUnknownUnit_withDetails() {
        this.inputFailure(TIME_FUNCTION + """
            a: $time(15j)
            """);
        this.outputExactly("""
            JelException: unknown unit: j
            ---------------------------------------------------
                6 |   _ >> raise: {
                           ^^^^^
                7 |     msg >> if: {
                8 |       $n.matches(\\d+): no unit provided
                9 |       _: unknown unit: $n.replace(^\\d+,,)
               10 |     }
               11 |     details: must be one of: [ s, m, h, y ]
               12 |   }
               13 | }
               14 | a: $time(15j)
                        ^^^^^^^^^
            ---------------------------------------------------
            must be one of: [ s, m, h, y ]""");
    }

    @Test
    public void templates_mayBeUsedAs_gettersAndSetters() {
        this.inputSuccess("""
            person: {
              name >> private: John
              name >> (): $name
              set_name >> (v): {
                name >> set: $v
              }
            }
            first: $person.name()
            >>: $person.set_name(Matthew)
            second: $person.name()
            """);
        // should probably add a way to preserve this value
        this.outputTrimmed("""
            person: {
            }
            first: John
            second: Matthew
            """);
    }

    @Test
    public void template_capturesScope() {
        this.inputSuccess("""
            a: {
              b >> private: value
              c: {
                d >> (): $b
              }
            }
            e: $a.b
            f: $a.c.d()
            """);
        this.outputTrimmed("""
            a: {
              c: {
              }
            }
            e: null
            f: value
            """);
    }
}
